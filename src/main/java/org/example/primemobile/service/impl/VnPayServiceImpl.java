package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.config.VnPayConfig;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.ThanhToan;
import org.example.primemobile.repository.DonHangRepository;
import org.example.primemobile.repository.ThanhToanRepository;
import org.example.primemobile.service.IVnPayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Triển khai nghiệp vụ tích hợp thanh toán VNPay.
 *
 * <h3>Luồng xử lý:</h3>
 * <ol>
 *   <li><b>Tạo URL thanh toán</b> – xây dựng các tham số, tạo chữ ký và trả về URL.</li>
 *   <li><b>Xử lý IPN</b> – VNPay gọi để xác nhận giao dịch, cập nhật trạng thái đơn hàng.</li>
 *   <li><b>Xử lý Return</b> – Khách hàng được redirect về, hiển thị kết quả.</li>
 * </ol>
 *
 * <h3>Chiến lược Transaction:</h3>
 * Các phương thức xử lý IPN/Return đều được đánh dấu {@code @Transactional}
 * để đảm bảo tính nhất quán dữ liệu.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VnPayServiceImpl implements IVnPayService {

    // ────────────────────────────────────────────────────────────────────────
    // HẰNG SỐ
    // ────────────────────────────────────────────────────────────────────────

    /** Phiên bản API VNPay hiện tại. */
    private static final String VERSION = "2.1.0";

    /** Command gửi sang VNPay: thanh toán. */
    private static final String COMMAND = "pay";

    /** Loại đơn hàng (mặc định là 'other'). */
    private static final String ORDER_TYPE = "other";

    /** Đơn vị tiền tệ. */
    private static final String CURRENCY = "VND";

    /** Ngôn ngữ giao diện VNPay. */
    private static final String LOCALE = "vn";

    /** Số phút hết hạn thanh toán. */
    private static final int EXPIRE_MINUTES = 15;

    /** Định dạng ngày giờ theo yêu cầu của VNPay. */
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // ────────────────────────────────────────────────────────────────────────
    // DEPENDENCIES
    // ────────────────────────────────────────────────────────────────────────

    private final VnPayConfig vnPayConfig;
    private final DonHangRepository donHangRepository;
    private final ThanhToanRepository thanhToanRepository;

    // ────────────────────────────────────────────────────────────────────────
    // PUBLIC METHODS
    // ────────────────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     */
    @Override
    public String createPaymentUrl(DonHang donHang, String clientIp) {
        log.info("[VNPay] Tạo URL thanh toán cho đơn hàng: {}, clientIp: {}",
                donHang.getMaDonHang(), clientIp);

        // ── Kiểm tra đơn hàng ──
        if (donHang.getTongThanhToan() == null ||
                donHang.getTongThanhToan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng tiền đơn hàng không hợp lệ.");
        }

        // ── Tạo mã giao dịch duy nhất ──
        // Định dạng: DH{idĐơnHàng}{timestampMillis} để đảm bảo duy nhất
        String vnp_TxnRef = "DH" + donHang.getId() + System.currentTimeMillis();

        // ── Số tiền (VNPay yêu cầu nhân với 100) ──
        long amount = donHang.getTongThanhToan().multiply(BigDecimal.valueOf(100)).longValue();

        // ── Thời gian ──
        String vnp_CreateDate = LocalDateTime.now().format(DATE_FORMATTER);
        String vnp_ExpireDate = LocalDateTime.now().plusMinutes(EXPIRE_MINUTES)
                .format(DATE_FORMATTER);

        // ── Xây dựng Map tham số ──
        Map<String, String> params = new LinkedHashMap<>();
        params.put("vnp_Version", VERSION);
        params.put("vnp_Command", COMMAND);
        params.put("vnp_TmnCode", vnPayConfig.getTmnCode());
        params.put("vnp_Amount", String.valueOf(amount));
        params.put("vnp_CurrCode", CURRENCY);
        params.put("vnp_TxnRef", vnp_TxnRef);
        params.put("vnp_OrderInfo", "Thanh toan don hang " + donHang.getMaDonHang());
        params.put("vnp_OrderType", ORDER_TYPE);
        params.put("vnp_Locale", LOCALE);
        params.put("vnp_ReturnUrl", vnPayConfig.getReturnUrl());
        params.put("vnp_IpAddr", clientIp);
        params.put("vnp_CreateDate", vnp_CreateDate);
        params.put("vnp_ExpireDate", vnp_ExpireDate);

        // ── Sắp xếp tham số và tạo query string ──
        String queryString = vnPayConfig.buildQueryString(params);

        // ── Tạo chữ ký ──
        String secureHash = vnPayConfig.hmacSHA512(vnPayConfig.getSecretKey(), queryString);

        // ── URL hoàn chỉnh ──
        String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryString +
                "&vnp_SecureHash=" + secureHash;

        log.info("[VNPay] URL thanh toán đã tạo thành công cho đơn hàng: {}",
                donHang.getMaDonHang());

        // ── Lưu vnp_TxnRef vào bảng thanh_toan để xử lý IPN/Return sau ──
        // Tìm bản ghi thanh toán 'cho' của đơn hàng này và cập nhật vnpTxnRef
        thanhToanRepository.findByDonHangIdAndTrangThai(donHang.getId(), "cho")
                .ifPresent(thanhToan -> {
                    thanhToan.setVnpTxnRef(vnp_TxnRef);
                    thanhToanRepository.save(thanhToan);
                    log.debug("[VNPay] Đã cập nhật vnpTxnRef={} cho thanh toán ID: {}",
                            vnp_TxnRef, thanhToan.getId());
                });

        return paymentUrl;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public String processIpn(Map<String, String> params) {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_SecureHash = params.get("vnp_SecureHash");

        log.info("[VNPay] IPN nhận được - TxnRef: {}, ResponseCode: {}",
                vnp_TxnRef, vnp_ResponseCode);

        // ── Bước 1: Kiểm tra chữ ký ──
        if (!vnPayConfig.verifySignature(params, vnp_SecureHash)) {
            log.warn("[VNPay] IPN - Chữ ký không hợp lệ cho TxnRef: {}", vnp_TxnRef);
            return "{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}";
        }

        // ── Bước 2: Tìm bản ghi thanh toán theo vnp_TxnRef ──
        Optional<ThanhToan> thanhToanOpt = thanhToanRepository.findByVnpTxnRef(vnp_TxnRef);
        if (thanhToanOpt.isEmpty()) {
            log.warn("[VNPay] IPN - Không tìm thấy giao dịch với TxnRef: {}", vnp_TxnRef);
            return "{\"RspCode\":\"01\",\"Message\":\"Transaction not found\"}";
        }

        ThanhToan thanhToan = thanhToanOpt.get();
        DonHang donHang = thanhToan.getDonHang();

        // ── Bước 3: Kiểm tra trạng thái đơn hàng ──
        // Chỉ cho phép xử lý IPN khi đơn hàng đang ở trạng thái chờ thanh toán
        if (!"dang_chuyen_huong".equals(donHang.getTrangThaiThanhToan())) {
            log.warn("[VNPay] IPN - Đơn hàng {} không ở trạng thái chờ thanh toán (hiện tại: {})",
                    donHang.getMaDonHang(), donHang.getTrangThaiThanhToan());
            return "{\"RspCode\":\"02\",\"Message\":\"Order not in pending state\"}";
        }

        // ── Bước 4: Cập nhật thông tin giao dịch ──
        // Lưu các thông tin từ VNPay để tra cứu sau
        thanhToan.setVnpResponseCode(vnp_ResponseCode);
        thanhToan.setVnpTransactionNo(params.get("vnp_TransactionNo"));
        thanhToan.setVnpBankCode(params.get("vnp_BankCode"));
        thanhToan.setVnpBankTranNo(params.get("vnp_BankTranNo"));
        thanhToan.setVnpCardType(params.get("vnp_CardType"));
        thanhToan.setVnpPayDate(params.get("vnp_PayDate"));
        thanhToan.setRawIpn(params.toString());

        // ── Bước 5: Xử lý kết quả ──
        if ("00".equals(vnp_ResponseCode)) {
            // THÀNH CÔNG
            thanhToan.setTrangThai("thanh_cong");
            thanhToan.setSoTienThucTe(thanhToan.getSoTien());
            thanhToan.setThoiGianThanhCong(LocalDateTime.now());

            donHang.setTrangThaiThanhToan("da_thanh_toan");
            donHang.setUpdatedAt(LocalDateTime.now());

            log.info("[VNPay] IPN - Thanh toán thành công cho đơn hàng: {}",
                    donHang.getMaDonHang());

            return "{\"RspCode\":\"00\",\"Message\":\"Success\"}";
        } else {
            // THẤT BẠI
            thanhToan.setTrangThai("that_bai");
            donHang.setTrangThaiThanhToan("that_bai");
            donHang.setUpdatedAt(LocalDateTime.now());

            log.warn("[VNPay] IPN - Thanh toán thất bại cho đơn hàng: {}, ResponseCode: {}",
                    donHang.getMaDonHang(), vnp_ResponseCode);

            // VNPay yêu cầu trả về RspCode=00 dù thành công hay thất bại
            // để không gửi lại IPN
            return "{\"RspCode\":\"00\",\"Message\":\"Success\"}";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public DonHang processReturn(Map<String, String> params) {
        String vnp_TxnRef = params.get("vnp_TxnRef");
        String vnp_ResponseCode = params.get("vnp_ResponseCode");
        String vnp_SecureHash = params.get("vnp_SecureHash");

        log.info("[VNPay] Return nhận được - TxnRef: {}, ResponseCode: {}",
                vnp_TxnRef, vnp_ResponseCode);

        // ── Bước 1: Kiểm tra chữ ký ──
        if (!vnPayConfig.verifySignature(params, vnp_SecureHash)) {
            log.warn("[VNPay] Return - Chữ ký không hợp lệ cho TxnRef: {}", vnp_TxnRef);
            throw new IllegalArgumentException("Chữ ký không hợp lệ.");
        }

        // ── Bước 2: Tìm bản ghi thanh toán ──
        ThanhToan thanhToan = thanhToanRepository.findByVnpTxnRef(vnp_TxnRef)
                .orElseThrow(() -> {
                    log.warn("[VNPay] Return - Không tìm thấy giao dịch với TxnRef: {}",
                            vnp_TxnRef);
                    return new IllegalArgumentException(
                            "Không tìm thấy giao dịch với mã: " + vnp_TxnRef);
                });

        DonHang donHang = thanhToan.getDonHang();

        // ── Bước 3: Nếu IPN đã xử lý thành công, không cần xử lý lại ──
        if ("thanh_cong".equals(thanhToan.getTrangThai())) {
            log.info("[VNPay] Return - Đơn hàng {} đã được IPN xác nhận thành công.",
                    donHang.getMaDonHang());
            return donHang;
        }

        // ── Bước 4: Nếu IPN chưa xử lý, tự xử lý (fallback) ──
        // Cập nhật thông tin giao dịch nếu chưa có
        if (thanhToan.getVnpResponseCode() == null) {
            thanhToan.setVnpResponseCode(vnp_ResponseCode);
            thanhToan.setVnpTransactionNo(params.get("vnp_TransactionNo"));
            thanhToan.setVnpBankCode(params.get("vnp_BankCode"));
            thanhToan.setVnpBankTranNo(params.get("vnp_BankTranNo"));
            thanhToan.setVnpCardType(params.get("vnp_CardType"));
            thanhToan.setVnpPayDate(params.get("vnp_PayDate"));
        }

        // ── Bước 5: Cập nhật trạng thái dựa trên response code ──
        if ("00".equals(vnp_ResponseCode)) {
            thanhToan.setTrangThai("thanh_cong");
            thanhToan.setSoTienThucTe(thanhToan.getSoTien());
            thanhToan.setThoiGianThanhCong(LocalDateTime.now());
            donHang.setTrangThaiThanhToan("da_thanh_toan");
            log.info("[VNPay] Return - Cập nhật thành công cho đơn hàng: {}",
                    donHang.getMaDonHang());
        } else {
            thanhToan.setTrangThai("that_bai");
            donHang.setTrangThaiThanhToan("that_bai");
            log.warn("[VNPay] Return - Cập nhật thất bại cho đơn hàng: {}, ResponseCode: {}",
                    donHang.getMaDonHang(), vnp_ResponseCode);
        }

        donHang.setUpdatedAt(LocalDateTime.now());

        // Lưu thay đổi
        thanhToanRepository.save(thanhToan);
        DonHang saved = donHangRepository.save(donHang);

        log.info("[VNPay] Return - Hoàn tất xử lý cho đơn hàng: {}, trạng thái thanh toán: {}",
                saved.getMaDonHang(), saved.getTrangThaiThanhToan());

        return saved;
    }
}