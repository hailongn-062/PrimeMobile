package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.request.DatHangRequest;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IDatHangOnlineService;
import org.example.primemobile.service.IKhuyenMaiService;
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

        private static final String LOAI_KHO_ONLINE = "kho_online";
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
        private final IKhuyenMaiService khuyenMaiService; // ✅ Thêm service tính giá khuyến mãi động

        // =========================================================================
        // PUBLIC METHOD
        // =========================================================================

        /**
         * {@inheritDoc}
         */
        @Override
        @Transactional
        public DonHang taoDonHang(DatHangRequest request) {

                log.info("[DatHangOnline] ▶ Bắt đầu checkout — khachHangId={}, sessionId={}",
                                request.khachHangId(), request.sessionId());

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 1 — LẤY GIỎ HÀNG
                // ═══════════════════════════════════════════════════════════════════
                GioHang gioHang = layGioHangHoacNemLoi(request.khachHangId(), request.sessionId());

                List<ChiTietGioHang> danhSachGio = gioHang.getChiTietGioHangs();
                if (danhSachGio == null || danhSachGio.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Giỏ hàng đang trống. Vui lòng thêm sản phẩm trước khi đặt hàng.");
                }
                log.debug("[DatHangOnline] Giỏ hàng hợp lệ — id={}, {} SKU",
                                gioHang.getId(), danhSachGio.size());

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 2 — KIỂM TRA KHO ONLINE & TÍNH TỔNG TIỀN HÀNG GỐC
                // Chiến lược Fail-Fast: validate toàn bộ TRƯỚC khi ghi bất kỳ dòng nào.
                // ═══════════════════════════════════════════════════════════════════
                Kho khoOnline = layKhoOnlineHoacNemLoi();
                BigDecimal tongTienHang = BigDecimal.ZERO;

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

                        // Tính tổng tiền gốc (chưa áp dụng khuyến mãi) để làm căn cứ cho KM toàn đơn
                        BigDecimal donGiaGoc = bienThe.getGiaBan();
                        tongTienHang = tongTienHang.add(donGiaGoc.multiply(BigDecimal.valueOf(soLuongYeuCau)));

                        log.debug("[DatHangOnline] SKU [{}] — soLuong={}, donGiaGoc={}, tonKho={}",
                                        bienThe.getMaSku(), soLuongYeuCau, donGiaGoc, tonKhoHienTai);
                }
                log.info("[DatHangOnline] Kiểm tra kho OK — tongTienHang={}", tongTienHang);

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 3 — CHUẨN BỊ & LƯU DONHANG
                // ═══════════════════════════════════════════════════════════════════

                // --- Lấy các entity liên quan ---
                KhachHang khachHang = layKhachHang(request.khachHangId(), gioHang);

                PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                                .findById(request.phuongThucThanhToanId())
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy phương thức thanh toán ID: "
                                                                + request.phuongThucThanhToanId()));

                // --- Xử lý mã giảm giá ---
                // (Đã loại bỏ mã giảm giá, tiền giảm mặc định là 0 cho đến khi logic khuyến mãi
                // mới được tích hợp)
                BigDecimal tienGiamGia = BigDecimal.ZERO;

                // --- Xác định trạng thái thanh toán theo phương thức ---
                boolean isVnPay = TEN_PTTT_VNPAY.equalsIgnoreCase(pttt.getTenPttt());
                String trangThaiThanhToan = isVnPay ? "dang_chuyen_huong" : "chua_thanh_toan";
                LocalDateTime thoiGianHetHanTt = isVnPay
                                ? LocalDateTime.now().plusMinutes(VNPAY_HET_HAN_PHUT)
                                : null;

                log.info("[DatHangOnline] Phương thức: {} — trangThaiThanhToan={}",
                                pttt.getTenPttt(), trangThaiThanhToan);

                // --- Build DonHang ---
                LocalDateTime now = LocalDateTime.now();
                DonHang.DonHangBuilder builder = DonHang.builder()
                                .maDonHang(sinhMaDonHang())
                                .khachHang(khachHang)
                                .kenhBan(KENH_BAN_ONLINE)
                                .ngayDat(now)
                                .tongTienHang(tongTienHang)
                                .tienGiamGia(tienGiamGia)
                                .phiShip(request.phiShip())
                                .trangThai(TRANG_THAI_CHO_XAC_NHAN)
                                .trangThaiThanhToan(trangThaiThanhToan)
                                .thoiGianHetHanTt(thoiGianHetHanTt)
                                .ghiChu(request.ghiChu())
                                .updatedAt(now);

                // --- Snapshot địa chỉ giao hàng ---
                if (request.diaChiGiaoId() != null) {
                        // Ưu tiên: dùng địa chỉ đã lưu → snapshot từ entity
                        DiaChiKhachHang dc = diaChiKhachHangRepository
                                        .findById(request.diaChiGiaoId())
                                        .orElseThrow(() -> new EntityNotFoundException(
                                                        "Không tìm thấy địa chỉ giao hàng ID: "
                                                                        + request.diaChiGiaoId()));
                        builder.diaChiGiao(dc)
                                        .hoTenNguoiNhan(dc.getHoTenNguoiNhan())
                                        .sdtNguoiNhan(dc.getSoDienThoaiNguoiNhan())
                                        .diaChiGiaCuThe(dc.getDiaChiChiTiet())
                                        .tinhThanhGiao(dc.getTinhThanhTen())
                                        .quanHuyenGiao(dc.getQuanHuyenTen())
                                        .phuongXaGiao(dc.getPhuongXaTen());
                        log.debug("[DatHangOnline] Snapshot địa chỉ cũ — diaChiId={}", dc.getId());
                } else {
                        // Fallback: dùng các field nhập tay
                        builder.hoTenNguoiNhan(request.hoTenNguoiNhan())
                                        .sdtNguoiNhan(request.sdtNguoiNhan())
                                        .diaChiGiaCuThe(request.diaChiGiaoCuThe())
                                        .tinhThanhGiao(request.tinhThanhGiao())
                                        .quanHuyenGiao(request.quanHuyenGiao())
                                        .phuongXaGiao(request.phuongXaGiao());
                        log.debug("[DatHangOnline] Snapshot địa chỉ mới — nguoiNhan={}",
                                        request.hoTenNguoiNhan());
                }

                DonHang donHang = donHangRepository.save(builder.build());
                log.info("[DatHangOnline] Đã lưu DonHang — maDonHang={}, id={}",
                                donHang.getMaDonHang(), donHang.getId());

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 4 — LƯU CHI TIẾT ĐƠN HÀNG (TUYỆT ĐỐI KHÔNG TRỪ KHO)
                // SNAPSHOT GIÁ SAU KHUYẾN MÃI ĐỘNG
                // ═══════════════════════════════════════════════════════════════════
                for (ChiTietGioHang item : danhSachGio) {
                        BienTheSanPham bienThe = item.getBienTheSanPham();

                        // Tính giá snapshot: áp dụng khuyến mãi động với tổng tiền gốc làm điều kiện
                        BigDecimal donGiaSauKM = khuyenMaiService.tinhGiaSauKhuyenMai(bienThe.getId(), tongTienHang);

                        ChiTietDonHang chiTiet = ChiTietDonHang.builder()
                                        .donHang(donHang)
                                        .bienTheSanPham(bienThe)
                                        .soLuong(item.getSoLuong())
                                        .donGiaBan(donGiaSauKM)
                                        .build();

                        chiTietDonHangRepository.save(chiTiet);
                        log.debug("[DatHangOnline] Lưu CTDH — sku={}, soLuong={}, donGiaSauKM={}",
                                        bienThe.getMaSku(), item.getSoLuong(), donGiaSauKM);
                }
                log.info("[DatHangOnline] Đã lưu {} dòng CTDH — KHO KHÔNG BỊ TRỪ.", danhSachGio.size());

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 5 — TẠO BẢN GHI THANH TOÁN
                // ═══════════════════════════════════════════════════════════════════
                // Tính số tiền: tongTienHang - tienGiamGia + phiShip
                // (tong_thanh_toan là computed column, chưa available trong session này)
                BigDecimal soTienThanhToan = tongTienHang
                                .subtract(tienGiamGia)
                                .add(request.phiShip());

                ThanhToan thanhToan = ThanhToan.builder()
                                .donHang(donHang)
                                .phuongThucThanhToan(pttt)
                                .soTien(soTienThanhToan)
                                .trangThai("cho") // Luôn khởi tạo "cho" — cập nhật qua VNPay IPN hoặc confirm COD
                                .thoiGianTao(now)
                                .build();

                thanhToanRepository.save(thanhToan);
                log.info("[DatHangOnline] Đã tạo ThanhToan — soTien={}, pttt={}",
                                soTienThanhToan, pttt.getTenPttt());

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC 6 — DỌN DẸP GIỎ HÀNG
                // Xóa GioHang → CascadeType.ALL tự xóa toàn bộ ChiTietGioHang theo.
                // ═══════════════════════════════════════════════════════════════════
                Integer gioHangId = gioHang.getId();
                gioHangRepository.delete(gioHang);
                log.info("[DatHangOnline] Đã dọn sạch giỏ hàng — gioHangId={}", gioHangId);

                log.info("[DatHangOnline] ✅ Checkout hoàn tất — maDonHang={}, tongThanhToan={}",
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
                // Khách vãng lai có thể đã đăng nhập gắn vào GioHang
                if (gioHang.getKhachHang() != null) {
                        return gioHang.getKhachHang();
                }
                // Khách vãng lai hoàn toàn (chỉ có session) → bắt buộc phải đăng nhập để đặt
                // hàng
                throw new IllegalArgumentException(
                                "Vui lòng đăng nhập hoặc cung cấp khachHangId để đặt hàng. " +
                                                "Khách vãng lai cần tạo tài khoản trước khi checkout.");
        }

        /**
         * Tìm kho online trong hệ thống.
         * Ném {@link IllegalStateException} nếu chưa cấu hình kho.
         */
        private Kho layKhoOnlineHoacNemLoi() {
                return khoRepository.findByLoai(LOAI_KHO_ONLINE)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Không tìm thấy kho online trong hệ thống. " +
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