package org.example.primemobile.service.impl;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.dto.VnPayPaymentResponse;
import org.example.primemobile.dto.request.DatHangRequest;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IDatHangOnlineService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.IVnPayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Triển khai phân hệ Đặt Hàng Online (Checkout) cho PrimeMobile.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md):</h2>
 * <ul>
 * <li>Đơn online: {@code kenh_ban = "online"},
 * {@code trang_thai = "cho_xac_nhan"}.</li>
 * <li>Kiểm tra tồn kho tại {@code kho_online} — chặn đặt quá số lượng.</li>
 * <li><b>⚠️ TUYỆT ĐỐI KHÔNG trừ kho lúc đặt hàng</b> (§2.2).
 * Kho bị trừ khi nhân viên xác nhận đơn ở bước tiếp theo.</li>
 * <li>VNPay: {@code trangThaiThanhToan = "dang_chuyen_huong"} +
 * {@code thoiGianHetHanTt = now + 15 phút}.</li>
 * <li>COD: {@code trangThaiThanhToan = "chua_thanh_toan"}.</li>
 * <li>Sau khi lưu đơn: xóa toàn bộ giỏ hàng của khách.</li>
 * </ul>
 *
 * <h2>Chiến lược Transaction:</h2>
 * <p>
 * {@code taoDonHang} chạy trong 1 {@code @Transactional} nguyên tử.
 * Bất kỳ lỗi nào → rollback hoàn toàn, giỏ hàng không bị xóa.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatHangOnlineServiceImpl implements IDatHangOnlineService {

    // ───────────────────────────────────────────────────────────────────────
    // HẰNG SỐ NGHIỆP VỤ
    // ───────────────────────────────────────────────────────────────────────

    private static final String TEN_PTTT_VNPAY = "VNPay";
    private static final int VNPAY_HET_HAN_PHUT = 15;
    private static final String KENH_BAN_ONLINE = "online";
    private static final String TRANG_THAI_CHO_XAC_NHAN = "cho_xac_nhan";
    private static final DateTimeFormatter MA_DON_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // ───────────────────────────────────────────────────────────────────────
    // DEPENDENCIES
    // ───────────────────────────────────────────────────────────────────────

    private final GioHangRepository gioHangRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final ThanhToanRepository thanhToanRepository;
    private final KhachHangRepository khachHangRepository;
    private final KhoRepository khoRepository;
    private final TonKhoRepository tonKhoRepository;
    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final IKhuyenMaiService khuyenMaiService; // ✅ Service tính khuyến mãi
    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository; // ✅ Repository để load CTKM
    private final IVnPayService vnPayService; // ✅ Service thanh toán VNPay

    @PersistenceContext
    private EntityManager entityManager; // ✅ Inject EntityManager để refresh computed column

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Phương thức này dành cho các phương thức thanh toán không phải VNPay (COD, chuyển khoản...).
     */
    @Override
    @Transactional
    public DonHang taoDonHang(DatHangRequest request) {
        log.info("[DatHangOnline] ▶ Bắt đầu checkout (COD/khác) — khachHangId={}, sessionId={}",
                request.khachHangId(), request.sessionId());

        // Gọi method chung để tạo đơn và lấy đối tượng DonHang đã lưu
        DonHang donHang = taoDonHangInternal(request, false);

        log.info("[DatHangOnline] ✅ Checkout hoàn tất (COD/khác) — maDonHang={}, tongThanhToan={}",
                donHang.getMaDonHang(), donHang.getTongThanhToan());

        return donHang;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Phương thức này dành cho phương thức thanh toán VNPay.
     * Sau khi tạo đơn, nó sẽ tạo URL thanh toán và trả về cho frontend.
     */
    @Override
    @Transactional
    public VnPayPaymentResponse taoDonHangVnPay(DatHangRequest request, String clientIp) {
        log.info("[DatHangOnline] ▶ Bắt đầu checkout VNPay — khachHangId={}, sessionId={}, clientIp={}",
                request.khachHangId(), request.sessionId(), clientIp);

        // Kiểm tra phương thức thanh toán có phải VNPay không
        PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                .findById(request.phuongThucThanhToanId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phương thức thanh toán ID: " + request.phuongThucThanhToanId()));

        if (!TEN_PTTT_VNPAY.equalsIgnoreCase(pttt.getTenPttt())) {
            throw new IllegalArgumentException(
                    "Phương thức thanh toán không phải VNPay. Vui lòng sử dụng đúng endpoint.");
        }

        // Gọi method chung để tạo đơn với flag isVnPay = true
        DonHang donHang = taoDonHangInternal(request, true);

        // Tạo URL thanh toán VNPay
        String paymentUrl = vnPayService.createPaymentUrl(donHang, clientIp);

        log.info("[DatHangOnline] ✅ Checkout VNPay hoàn tất — maDonHang={}, paymentUrl={}",
                donHang.getMaDonHang(), paymentUrl);

        return VnPayPaymentResponse.builder()
                .paymentUrl(paymentUrl)
                .maDonHang(donHang.getMaDonHang())
                .donHangId(donHang.getId())
                .build();
    }

    // =========================================================================
    // PRIVATE METHODS — LOGIC CHUNG CHO TẤT CẢ PHƯƠNG THỨC THANH TOÁN
    // =========================================================================

    /**
     * Phương thức nội bộ thực hiện toàn bộ luồng tạo đơn hàng.
     * Được gọi từ cả {@link #taoDonHang(DatHangRequest)} và {@link #taoDonHangVnPay(DatHangRequest, String)}.
     *
     * @param request  DTO chứa thông tin đặt hàng.
     * @param isVnPay  {@code true} nếu là thanh toán VNPay (sẽ set trạng thái "dang_chuyen_huong"),
     *                 {@code false} cho các phương thức khác (COD, chuyển khoản).
     * @return {@link DonHang} đã được persist.
     * @throws IllegalArgumentException nếu giỏ trống, kho không đủ, hoặc dữ liệu không hợp lệ.
     */
    private DonHang taoDonHangInternal(DatHangRequest request, boolean isVnPay) {
        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 1 — LẤY GIỎ HÀNG
        // ─────────────────────────────────────────────────────────────────────
        GioHang gioHang = layGioHangHoacNemLoi(request.khachHangId(), request.sessionId());

        List<ChiTietGioHang> danhSachGio = gioHang.getChiTietGioHangs();
        if (danhSachGio == null || danhSachGio.isEmpty()) {
            throw new IllegalArgumentException(
                    "Giỏ hàng đang trống. Vui lòng thêm sản phẩm trước khi đặt hàng.");
        }
        log.debug("[DatHangOnline] Giỏ hàng hợp lệ — id={}, {} SKU",
                gioHang.getId(), danhSachGio.size());

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 2 — KIỂM TRA KHO ONLINE & TÍNH TỔNG TIỀN SAU KHUYẾN MÃI SẢN PHẨM
        // ─────────────────────────────────────────────────────────────────────
        Kho khoOnline = layKhoOnlineHoacNemLoi();
        BigDecimal tongTienHang = BigDecimal.ZERO; // Tổng tiền sau khuyến mãi sản phẩm

        for (ChiTietGioHang item : danhSachGio) {
            BienTheSanPham bienThe = item.getBienTheSanPham();
            int soLuongYeuCau = item.getSoLuong();

            // Kiểm tra tồn kho kho_online
            int tonKhoHienTai = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoOnline, bienThe)
                    .map(TonKho::getSoLuong)
                    .orElse(0);

            if (soLuongYeuCau > tonKhoHienTai) {
                throw new IllegalArgumentException(String.format(
                        "Sản phẩm [%s] không đủ hàng trong kho online. " +
                                "Yêu cầu: %d, còn lại: %d.",
                        bienThe.getMaSku(), soLuongYeuCau, tonKhoHienTai));
            }

            // Tính giá sau khuyến mãi sản phẩm (không áp dụng toàn đơn)
            BigDecimal donGiaSauKhuyenMai = khuyenMaiService.tinhGiaSauKhuyenMai(bienThe.getId(), null);
            tongTienHang = tongTienHang.add(donGiaSauKhuyenMai.multiply(BigDecimal.valueOf(soLuongYeuCau)));

            log.debug("[DatHangOnline] SKU [{}] — soLuong={}, donGiaSauKhuyenMai={}, tonKho={}",
                    bienThe.getMaSku(), soLuongYeuCau, donGiaSauKhuyenMai, tonKhoHienTai);
        }
        log.info("[DatHangOnline] Kiểm tra kho OK — tongTienHang={}", tongTienHang);

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 3 — XỬ LÝ KHUYẾN MÃI TOÀN ĐƠN
        // ─────────────────────────────────────────────────────────────────────
        KhuyenMaiResult kmResult = khuyenMaiService.tinhKhuyenMaiChoDonHang(tongTienHang);
        BigDecimal tienGiamGia = kmResult.getTienGiam();
        ChuongTrinhKhuyenMai ctkm = null;

        if (kmResult.getCtkmId() != null) {
            ctkm = chuongTrinhKhuyenMaiRepository.findById(kmResult.getCtkmId())
                    .orElse(null);
            log.info("[DatHangOnline] Áp dụng CTKM toàn đơn: id={}, ten='{}', giảm={}%, tiền giảm={}",
                    kmResult.getCtkmId(), kmResult.getTenCtkm(),
                    kmResult.getGiaTriUuDai(), tienGiamGia);
        } else {
            log.debug("[DatHangOnline] Không có CTKM toàn đơn phù hợp.");
        }

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 4 — CHUẨN BỊ & LƯU DONHANG
        // ─────────────────────────────────────────────────────────────────────
        KhachHang khachHang = layKhachHang(request.khachHangId(), gioHang);

        PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                .findById(request.phuongThucThanhToanId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phương thức thanh toán ID: " + request.phuongThucThanhToanId()));

        // Xác định trạng thái thanh toán theo flag isVnPay
        String trangThaiThanhToan = isVnPay ? "dang_chuyen_huong" : "chua_thanh_toan";
        LocalDateTime thoiGianHetHanTt = isVnPay
                ? LocalDateTime.now().plusMinutes(VNPAY_HET_HAN_PHUT)
                : null;

        log.info("[DatHangOnline] Phương thức: {} — trangThaiThanhToan={}",
                pttt.getTenPttt(), trangThaiThanhToan);

        // Build DonHang
        LocalDateTime now = LocalDateTime.now();
        DonHang.DonHangBuilder builder = DonHang.builder()
                .maDonHang(sinhMaDonHang())
                .khachHang(khachHang)
                .kenhBan(KENH_BAN_ONLINE)
                .ngayDat(now)
                .tongTienHang(tongTienHang)
                .tienGiamGia(tienGiamGia)
                .phiShip(request.phiShip())
                .chuongTrinhKhuyenMai(ctkm)
                .trangThai(TRANG_THAI_CHO_XAC_NHAN)
                .trangThaiThanhToan(trangThaiThanhToan)
                .thoiGianHetHanTt(thoiGianHetHanTt)
                .ghiChu(request.ghiChu())
                .updatedAt(now);

        // Snapshot địa chỉ giao hàng
        if (request.diaChiGiaoId() != null) {
            DiaChiKhachHang dc = diaChiKhachHangRepository
                    .findById(request.diaChiGiaoId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy địa chỉ giao hàng ID: " + request.diaChiGiaoId()));
            builder.diaChiGiao(dc)
                    .hoTenNguoiNhan(dc.getHoTenNguoiNhan())
                    .sdtNguoiNhan(dc.getSoDienThoaiNguoiNhan())
                    .diaChiGiaCuThe(dc.getDiaChiChiTiet())
                    .tinhThanhGiao(dc.getTinhThanhTen())
                    .quanHuyenGiao(dc.getQuanHuyenTen())
                    .phuongXaGiao(dc.getPhuongXaTen());
            log.debug("[DatHangOnline] Snapshot địa chỉ cũ — diaChiId={}", dc.getId());
        } else {
            builder.hoTenNguoiNhan(request.hoTenNguoiNhan())
                    .sdtNguoiNhan(request.sdtNguoiNhan())
                    .diaChiGiaCuThe(request.diaChiGiaoCuThe())
                    .tinhThanhGiao(request.tinhThanhGiao())
                    .quanHuyenGiao(request.quanHuyenGiao())
                    .phuongXaGiao(request.phuongXaGiao());
            log.debug("[DatHangOnline] Snapshot địa chỉ mới — nguoiNhan={}",
                    request.hoTenNguoiNhan());
        }

        // ⚠️ QUAN TRỌNG: Sử dụng saveAndFlush() + refresh() để lấy computed column
        DonHang donHang = donHangRepository.saveAndFlush(builder.build());
        entityManager.refresh(donHang); // Load computed column tong_thanh_toan từ DB

        log.info("[DatHangOnline] Đã lưu DonHang — maDonHang={}, id={}, tongTienHang={}, tienGiamGia={}, tongThanhToan={}",
                donHang.getMaDonHang(), donHang.getId(),
                donHang.getTongTienHang(), donHang.getTienGiamGia(),
                donHang.getTongThanhToan());

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 5 — LƯU CHI TIẾT ĐƠN HÀNG (TUYỆT ĐỐI KHÔNG TRỪ KHO)
        // ─────────────────────────────────────────────────────────────────────
        for (ChiTietGioHang item : danhSachGio) {
            BienTheSanPham bienThe = item.getBienTheSanPham();
            BigDecimal donGiaSauKhuyenMai = khuyenMaiService.tinhGiaSauKhuyenMai(bienThe.getId(), null);

            ChiTietDonHang chiTiet = ChiTietDonHang.builder()
                    .donHang(donHang)
                    .bienTheSanPham(bienThe)
                    .soLuong(item.getSoLuong())
                    .donGiaBan(donGiaSauKhuyenMai)
                    .build();

            chiTietDonHangRepository.save(chiTiet);
            log.debug("[DatHangOnline] Lưu CTDH — sku={}, soLuong={}, donGiaSauKhuyenMai={}",
                    bienThe.getMaSku(), item.getSoLuong(), donGiaSauKhuyenMai);
        }
        log.info("[DatHangOnline] Đã lưu {} dòng CTDH — KHO KHÔNG BỊ TRỪ.", danhSachGio.size());

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 6 — TẠO BẢN GHI THANH TOÁN
        // ─────────────────────────────────────────────────────────────────────
        // Sử dụng tongThanhToan đã refresh để tính số tiền thanh toán
        BigDecimal soTienThanhToan = donHang.getTongThanhToan() != null
                ? donHang.getTongThanhToan()
                : tongTienHang.subtract(tienGiamGia).add(request.phiShip());

        ThanhToan thanhToan = ThanhToan.builder()
                .donHang(donHang)
                .phuongThucThanhToan(pttt)
                .soTien(soTienThanhToan)
                .trangThai("cho") // Luôn khởi tạo "cho" — cập nhật qua VNPay IPN hoặc confirm COD
                .thoiGianTao(now)
                .build();

        // Nếu là VNPay, lưu vnpTxnRef sau khi tạo URL (ở bên ngoài)
        thanhToanRepository.save(thanhToan);
        log.info("[DatHangOnline] Đã tạo ThanhToan — soTien={}, pttt={}",
                soTienThanhToan, pttt.getTenPttt());

        // ─────────────────────────────────────────────────────────────────────
        // BƯỚC 7 — DỌN DẸP GIỎ HÀNG
        // ─────────────────────────────────────────────────────────────────────
        Integer gioHangId = gioHang.getId();
        gioHangRepository.delete(gioHang);
        log.info("[DatHangOnline] Đã dọn sạch giỏ hàng — gioHangId={}", gioHangId);

        log.info("[DatHangOnline] Đã tạo đơn thành công — maDonHang={}, tongThanhToan={}",
                donHang.getMaDonHang(), soTienThanhToan);

        return donHang;
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Tìm GioHang kèm FETCH JOIN chi tiết (tránh N+1).
     * Ưu tiên theo khachHangId, fallback theo sessionId.
     * Ném {@link IllegalArgumentException} nếu không tìm thấy.
     */
    private GioHang layGioHangHoacNemLoi(Integer khachHangId, String sessionId) {
        if (khachHangId != null) {
            return gioHangRepository.findByKhachHangIdWithDetails(khachHangId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy giỏ hàng cho khách hàng ID: " + khachHangId
                                    + ". Giỏ hàng trống hoặc chưa có sản phẩm."));
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return gioHangRepository.findBySessionIdWithDetails(sessionId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Không tìm thấy giỏ hàng cho session: " + sessionId
                                    + ". Giỏ hàng trống hoặc chưa có sản phẩm."));
        }
        throw new IllegalArgumentException(
                "Phải cung cấp khachHangId hoặc sessionId để xác định giỏ hàng.");
    }

    /**
     * Lấy KhachHang từ request hoặc từ GioHang đang attach.
     * Ném lỗi nếu không xác định được khách hàng (đơn online PHẢI gắn khách).
     */
    private KhachHang layKhachHang(Integer khachHangId, GioHang gioHang) {
        if (khachHangId != null) {
            return khachHangRepository.findById(khachHangId)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy khách hàng ID: " + khachHangId));
        }
        if (gioHang.getKhachHang() != null) {
            return gioHang.getKhachHang();
        }
        throw new IllegalArgumentException(
                "Vui lòng đăng nhập hoặc cung cấp khachHangId để đặt hàng. " +
                        "Khách vãng lai cần tạo tài khoản trước khi checkout.");
    }

    /**
     * Lấy kho duy nhất trong hệ thống (ID = 1).
     * Ném {@link IllegalStateException} nếu chưa cấu hình kho.
     */
    private Kho layKhoOnlineHoacNemLoi() {
        return khoRepository.findById(1)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy kho ID=1 trong hệ thống. " +
                                "Vui lòng kiểm tra dữ liệu bảng kho."));
    }

    /**
     * Sinh mã đơn hàng theo format: {@code DHO-YYYYMMDD-<6 chữ số cuối millis>}.
     * Tiền tố "DHO" (Đặt Hàng Online) phân biệt với đơn offline "DH".
     */
    private String sinhMaDonHang() {
        String datePart = LocalDateTime.now().format(MA_DON_FMT);
        String randPart = String.format("%06d", System.currentTimeMillis() % 1_000_000L);
        return "DHO-" + datePart + "-" + randPart;
    }
}