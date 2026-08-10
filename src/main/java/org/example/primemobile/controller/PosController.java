package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.pos.BienThePosDto;
import org.example.primemobile.dto.pos.KhuyenMaiPosDto;
import org.example.primemobile.dto.request.PosThanhToanRequest;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IBanHangOfflineService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller hỗ trợ màn hình Bán hàng tại quầy (POS).
 *
 * <p>
 * Base path: {@code /api/admin/pos}
 * Được bảo vệ bởi {@code AuthInterceptor} (pattern {@code /api/admin/**}).
 *
 * <h2>Endpoints:</h2>
 *
 * <pre>
 *   GET  /san-pham           → Danh sách biến thể còn hàng tại Kho Tổng
 *   GET  /tinh-khuyen-mai    → Tính khuyến mãi tốt nhất cho tổng tiền
 *   POST /thanh-toan         → Hoàn tất đơn POS (lưu đơn + IMEI + trừ kho)
 * </pre>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/pos")
@RequiredArgsConstructor
public class PosController {

    // ──────────────────────────────────────────────────────────────────────────
    // Constants
    // ──────────────────────────────────────────────────────────────────────────

    private static final int    KHO_ID          = 1; // Kho duy nhất trong hệ thống
    private static final String SDT_KHACH_LE = "0000000000";
    private static final String TRANG_THAI_DA_BAN = "da_ban";
    private static final DateTimeFormatter MA_DON_FMT = DateTimeFormatter.ofPattern("yyMMdd");

    // ──────────────────────────────────────────────────────────────────────────
    // Dependencies
    // ──────────────────────────────────────────────────────────────────────────

    private final IBanHangOfflineService banHangOfflineService;
    private final IKhuyenMaiService khuyenMaiService;

    private final TonKhoRepository tonKhoRepository;
    private final KhoRepository khoRepository;
    private final KhachHangRepository khachHangRepository;
    private final DonHangRepository donHangRepository;
    private final ChiTietDonHangRepository chiTietDonHangRepository;
    private final MayDienThoaiRepository mayDienThoaiRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final ChuongTrinhKhuyenMaiRepository ctkmRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/admin/pos/san-pham
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Lấy danh sách biến thể sản phẩm còn tồn kho tại Kho Tổng.
     *
     * <p>
     * Chỉ trả về những biến thể có {@code soLuong > 0}.
     * Kết quả bao gồm tên SP, SKU, màu sắc, RAM, ROM, giá bán (ưu tiên
     * giá khuyến mãi nếu có), tồn kho, và URL ảnh đại diện đầu tiên.
     *
     * @return HTTP 200 kèm {@code List<BienThePosDto>}.
     */
    @GetMapping("/san-pham")
    @Transactional(readOnly = true)
    public ResponseEntity<List<BienThePosDto>> layDanhSachSanPham() {
        log.debug("[POS] GET /san-pham — Lấy danh sách sản phẩm cho POS");

        List<TonKho> tonKhos = tonKhoRepository.layDanhSachChoPos(KHO_ID);

        List<BienThePosDto> result = tonKhos.stream()
                .map(this::mapToBienThePosDto)
                .collect(Collectors.toList());

        log.info("[POS] Trả về {} biến thể có tồn kho tại Kho Tổng.", result.size());
        return ResponseEntity.ok(result);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/admin/pos/tinh-khuyen-mai?tongTien=...
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Tự động tính khuyến mãi tốt nhất cho tổng tiền hàng hiện tại.
     *
     * <p>
     * Gọi {@link IKhuyenMaiService#tinhKhuyenMaiChoDonHang(BigDecimal)}
     * để lấy kết quả khuyến mãi thống nhất với luồng Online và Giỏ hàng.
     *
     * @param tongTien Tổng tiền hàng (query param, bắt buộc).
     * @return HTTP 200 kèm {@link KhuyenMaiPosDto}.
     */
    @GetMapping("/tinh-khuyen-mai")
    @Transactional(readOnly = true)
    public ResponseEntity<?> tinhKhuyenMai(
            @RequestParam BigDecimal tongTien,
            @RequestParam(required = false) Integer ctkmId) {

        log.debug("[POS] GET /tinh-khuyen-mai — tongTien={}, ctkmId={}", tongTien, ctkmId);

        KhuyenMaiResult result;
        if (ctkmId != null) {
            try {
                result = khuyenMaiService.apDungCtkmTheoId(ctkmId, tongTien);
            } catch (IllegalArgumentException e) {
                log.warn("[POS] Lỗi áp dụng CTKM {}: {}", ctkmId, e.getMessage());
                return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
            }
        } else {
            result = khuyenMaiService.tinhKhuyenMaiChoDonHang(tongTien);
        }

        KhuyenMaiPosDto dto = KhuyenMaiPosDto.builder()
                .ctkmId(result.getCtkmId())
                .tenCtkm(result.getTenCtkm())
                .giaTriUuDai(result.getGiaTriUuDai())
                .tienGiam(result.getTienGiam())
                .build();

        log.info("[POS] tinhKhuyenMai — tongTien={}, ctkmId={}, tienGiam={}",
                tongTien, dto.getCtkmId(), dto.getTienGiam());
        return ResponseEntity.ok(dto);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/admin/pos/thanh-toan
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Hoàn tất thanh toán đơn hàng POS trong một transaction nguyên tử.
     *
     * <h3>Luồng xử lý chi tiết:</h3>
     * <ol>
     * <li><b>Chốt khách hàng</b>: Nếu {@code khachHangId} null hoặc không tồn tại
     * → fallback về tài khoản khách lẻ ({@code sdt = '0000000000'}).</li>
     * <li><b>Validate IMEI</b>: Với mỗi dòng hàng, kiểm tra:
     * <ul>
     * <li>Số lượng IMEI == {@code soLuong}.</li>
     * <li>Từng IMEI tồn tại trong DB và đang có tình trạng
     * {@code 'trong_kho'}.</li>
     * </ul>
     * </li>
     * <li><b>Tạo DonHang</b>: Trạng thái {@code "da_hoan_thanh"}, thanh toán
     * {@code "da_thanh_toan"},
     * kênh bán {@code "tai_quay"}.</li>
     * <li><b>Tạo ChiTietDonHang</b>: Từng dòng trong {@code chiTiets}.</li>
     * <li><b>Cập nhật IMEI</b>: Đổi {@code tinhTrang → 'da_ban'}, gán
     * {@code donHang}.</li>
     * <li><b>Trừ tồn kho</b>: Trừ {@code soLuong} vào {@code ton_kho} Kho
     * Tổng.</li>
     * <li><b>Gán CTKM</b>: Nếu {@code ctkmId} hợp lệ, gán vào đơn hàng.</li>
     * </ol>
     *
     * @param req         Payload từ màn hình POS.
     * @param sessionUser Nhân viên đang đăng nhập (lấy từ session).
     * @return HTTP 200 kèm {@code Map} chứa {@code donHangId} và {@code maDonHang}.
     */
    @PostMapping("/thanh-toan")
    @Transactional
    public ResponseEntity<?> thanhToan(
            @RequestBody PosThanhToanRequest req,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[POS] POST /thanh-toan — nhanVienId={}, khachHangId={}, tongTien={}, tienGiam={}, soLuongDong={}",
                sessionUser.getId(), req.getKhachHangId(), req.getTongTien(),
                req.getTienGiam(), req.getChiTiets() == null ? 0 : req.getChiTiets().size());

        // ── Validate payload cơ bản ────────────────────────────────────────
        if (req.getChiTiets() == null || req.getChiTiets().isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng phải có ít nhất 1 sản phẩm.");
        }
        if (req.getTongTien() == null || req.getTongTien().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Tổng tiền không hợp lệ.");
        }

        try {
            // ── Bước 1: Lưu đơn hàng bằng logic luuDonHangCho ────────────
            // luuDonHangCho xử lý tạo mới hoặc cập nhật đơn cũ, gán khách hàng, 
            // lock IMEI (da_ban), tính giá, gán CTKM.
            // Kết thúc luuDonHangCho, đơn ở trạng thái 'don_hang_cho'
            DonHang donHang = banHangOfflineService.luuDonHangCho(req.getDonHangId(), sessionUser.getId(), req);

            // (Đã loại bỏ việc gọi tiepTucDonHangCho vì thanhToanDonHang nay đã chấp nhận trực tiếp don_hang_cho)

            // ── Bước 3: Hoàn tất thanh toán, trừ tồn kho, lưu lịch sử ─────
            // Lấy ID PTTT từ request, nếu null thì mặc định 1 (Tiền mặt)
            Integer phuongThucThanhToanId = req.getPhuongThucThanhToanId() != null ? req.getPhuongThucThanhToanId() : 1; 
            donHang = banHangOfflineService.thanhToanDonHang(donHang.getId(), phuongThucThanhToanId);

            BigDecimal tienGiam = req.getTienGiam() != null ? req.getTienGiam() : BigDecimal.ZERO;
            return ResponseEntity.ok(Map.of(
                    "donHangId", donHang.getId(),
                    "maDonHang", donHang.getMaDonHang(),
                    "tongThanh", donHang.getTongThanhToan() != null ? donHang.getTongThanhToan() : req.getTongTien().subtract(tienGiam),
                    "khachHang", donHang.getKhachHang() != null ? donHang.getKhachHang().getHoTen() : "Khách lẻ"));

        } catch (EntityNotFoundException | IllegalArgumentException | IllegalStateException e) {
            log.error("[POS] Lỗi thanh toán: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            log.error("[POS] Lỗi không xác định khi thanh toán: ", e);
            return ResponseEntity.status(500).body("Lỗi hệ thống khi thanh toán: " + e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // IMEI RESERVATION (GIỮ IMEI)
    // ══════════════════════════════════════════════════════════════════════════

    @PostMapping("/giu-imei")
    public ResponseEntity<?> giuImei(@RequestParam String imei, @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            banHangOfflineService.giuImei(imei, sessionUser.getId());
            return ResponseEntity.ok("Đã giữ IMEI thành công");
        } catch (Exception e) {
            log.warn("[POS] Lỗi giữ IMEI {}: {}", imei, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/nha-imei")
    public ResponseEntity<?> nhaImei(@RequestParam String imei, @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            banHangOfflineService.nhaImei(imei, sessionUser.getId());
            return ResponseEntity.ok("Đã nhả IMEI thành công");
        } catch (Exception e) {
            log.warn("[POS] Lỗi nhả IMEI {}: {}", imei, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/nha-tat-ca-imei")
    public ResponseEntity<?> nhaTatCaImei(@SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            banHangOfflineService.nhaTatCaImeiCuaNhanVien(sessionUser.getId());
            return ResponseEntity.ok("Đã nhả tất cả IMEI thành công");
        } catch (Exception e) {
            log.error("[POS] Lỗi nhả tất cả IMEI: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EXCEPTION HANDLERS cục bộ
    // ══════════════════════════════════════════════════════════════════════════

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFound(EntityNotFoundException e) {
        log.error("[POS] EntityNotFound: {}", e.getMessage());
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleBadRequest(IllegalArgumentException e) {
        log.warn("[POS] IllegalArgument: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        log.warn("[POS] IllegalState: {}", e.getMessage());
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Map {@link TonKho} (kèm JOIN FETCH biến thể + sản phẩm) sang
     * {@link BienThePosDto}.
     * Lấy ảnh đại diện (laAnhChinh = true) làm {@code anhDaiDien},
     * fallback về ảnh đầu tiên nếu không có ảnh chính.
     */
    private BienThePosDto mapToBienThePosDto(TonKho tonKho) {
        BienTheSanPham bt = tonKho.getBienTheSanPham();
        SanPham sp = bt.getSanPham();

        String anhDaiDien = bt.getHinhAnhSanPhams().stream()
                .filter(h -> Boolean.TRUE.equals(h.getLaAnhChinh()))
                .findFirst()
                .or(() -> bt.getHinhAnhSanPhams().stream().findFirst())
                .map(HinhAnhSanPham::getDuongDan)
                .orElse(null);

        // Tính giá gốc và giá sau khuyến mãi
        BigDecimal giaGoc = bt.getGiaBan(); // Giá bán gốc
        BigDecimal giaSauKM = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), BigDecimal.ZERO);

        // Tính số lượng tồn thực tế (chỉ đếm IMEI có trạng thái 'trong_kho')
        int soLuongThucTe = (int) mayDienThoaiRepository.countTrongKhoByBienThe(bt.getId());

        return BienThePosDto.builder()
                .sanPhamId(sp.getId())
                .bienTheId(bt.getId())
                .tenSanPham(sp.getTenSanPham())
                .maSku(bt.getMaSku())
                .mauSac(bt.getMauSacTen())
                .ramGb(bt.getRamGb())
                .luuTruGb(bt.getLuuTruGb())
                .giaGoc(giaGoc)          // Giá gốc (không khuyến mãi)
                .giaBan(giaSauKM)        // Giá sau khuyến mãi
                .tonKho(soLuongThucTe)
                .anhDaiDien(anhDaiDien)
                .build();
    }

    /**
     * Sinh mã đơn hàng POS theo format:
     * {@code POS-YYYYMMDD-<6 chữ số cuối millis>}.
     * Ví dụ: {@code POS-20240626-334521}.
     */
    private String sinhMaDonHang() {
        String datePart = LocalDateTime.now().format(MA_DON_FMT);
        String randPart = String.format("%04d", System.currentTimeMillis() % 10_000L);
        return "POS" + datePart + randPart;
    }
}