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

    private static final String LOAI_KHO_TONG = "kho_tong";
    private static final String SDT_KHACH_LE = "0000000000";
    private static final String TRANG_THAI_DA_BAN = "da_ban";
    private static final DateTimeFormatter MA_DON_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

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

        List<TonKho> tonKhos = tonKhoRepository.layDanhSachChoPos(LOAI_KHO_TONG);

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
    public ResponseEntity<KhuyenMaiPosDto> tinhKhuyenMai(
            @RequestParam BigDecimal tongTien) {

        log.debug("[POS] GET /tinh-khuyen-mai — tongTien={}", tongTien);

        // Sử dụng method thống nhất để tính khuyến mãi
        KhuyenMaiResult result = khuyenMaiService.tinhKhuyenMaiChoDonHang(tongTien);

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
     * <li><b>Tạo DonHang</b>: Trạng thái {@code "da_giao"}, thanh toán
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

        log.info("[POS] POST /thanh-toan — nhanVienId={}, khachHangId={}, tongTien={}, " +
                        "tienGiam={}, soLuongDong={}",
                sessionUser.getId(), req.getKhachHangId(), req.getTongTien(),
                req.getTienGiam(), req.getChiTiets() == null ? 0 : req.getChiTiets().size());

        // ── Validate payload cơ bản ────────────────────────────────────────
        if (req.getChiTiets() == null || req.getChiTiets().isEmpty()) {
            return ResponseEntity.badRequest().body("Đơn hàng phải có ít nhất 1 sản phẩm.");
        }
        if (req.getTongTien() == null || req.getTongTien().compareTo(BigDecimal.ZERO) <= 0) {
            return ResponseEntity.badRequest().body("Tổng tiền không hợp lệ.");
        }

        // ── Bước 1: Lấy Kho Tổng ──────────────────────────────────────────
        Kho khoTong = khoRepository.findByLoai(LOAI_KHO_TONG)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy Kho Tổng trong hệ thống."));

        // ── Bước 2: Chốt khách hàng (Optional-based) ──────────────────────
        KhachHang khachHang = java.util.Optional.ofNullable(req.getKhachHangId())
                .flatMap(khachHangRepository::findById)
                .orElseGet(() -> {
                    if (req.getKhachHangId() != null) {
                        log.warn("[POS] Không tìm thấy khách hàng id={}, fallback Khách lẻ.",
                                req.getKhachHangId());
                    }
                    return khachHangRepository.findBySoDienThoai(SDT_KHACH_LE)
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Không tìm thấy tài khoản Khách lẻ mặc định (sdt='0000000000')."));
                });

        // ── Bước 3: Lấy nhân viên từ session ──────────────────────────────
        NguoiDung nhanVien = nguoiDungRepository.findById(sessionUser.getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy nhân viên id=" + sessionUser.getId()));

        // ── Bước 4: Validate IMEI (fail-fast toàn bộ trước khi ghi bất kỳ thứ gì) ──
        for (PosThanhToanRequest.ChiTietPosRequest ct : req.getChiTiets()) {
            if (ct.getImeis() == null || ct.getImeis().isEmpty()) {
                return ResponseEntity.badRequest().body(
                        "Dòng hàng bienTheId=" + ct.getBienTheId() +
                                " chưa có mã IMEI. Vui lòng nhập IMEI.");
            }
            if (ct.getImeis().size() != ct.getSoLuong()) {
                return ResponseEntity.badRequest().body(String.format(
                        "Số lượng IMEI (%d) không khớp với số lượng mua (%d) " +
                                "cho biến thể id=%d.",
                        ct.getImeis().size(), ct.getSoLuong(), ct.getBienTheId()));
            }
            // Validate từng IMEI tồn tại và đang 'trong_kho'
            for (String imei : ct.getImeis()) {
                String imeiClean = imei.trim();
                MayDienThoai may = mayDienThoaiRepository.findByImei1(imeiClean)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Không tìm thấy IMEI [" + imeiClean
                                        + "] trong hệ thống."));

                if (!"trong_kho".equals(may.getTinhTrang())) {
                    return ResponseEntity.badRequest().body(String.format(
                            "IMEI [%s] không ở trạng thái 'trong_kho' (hiện tại: '%s'). " +
                                    "Vui lòng kiểm tra lại.",
                            imeiClean, may.getTinhTrang()));
                }
            }
        }

        // ── Bước 5: Sinh mã đơn hàng ──────────────────────────────────────
        String maDonHang = sinhMaDonHang();
        BigDecimal tienGiam = req.getTienGiam() != null
                ? req.getTienGiam()
                : BigDecimal.ZERO;

        // ── Bước 6: Build và lưu DonHang ──────────────────────────────────
        DonHang donHang = DonHang.builder()
                .maDonHang(maDonHang)
                .khachHang(khachHang)
                .nguoiXuLy(nhanVien)
                .kenhBan("tai_quay")
                .ngayDat(LocalDateTime.now())
                .tongTienHang(req.getTongTien())
                .tienGiamGia(tienGiam)
                .phiShip(BigDecimal.ZERO) // Tại quầy: không phí ship
                .trangThai("da_giao") // §2.1: Bán tại quầy = giao ngay
                .trangThaiThanhToan("da_thanh_toan")
                .ngayGiaoThucTe(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Gán chương trình khuyến mãi nếu có
        if (req.getCtkmId() != null) {
            ctkmRepository.findById(req.getCtkmId())
                    .ifPresent(donHang::setChuongTrinhKhuyenMai);
        }

        donHang = donHangRepository.save(donHang);
        final DonHang savedDonHang = donHang;

        // ── Bước 7: Tạo ChiTietDonHang + Cập nhật IMEI + Trừ kho ─────────
        LocalDateTime now = LocalDateTime.now();
        for (PosThanhToanRequest.ChiTietPosRequest ct : req.getChiTiets()) {

            // 7a. Load biến thể (đã validate ở bước 4, chắc chắn tồn tại)
            BienTheSanPham bienThe = mayDienThoaiRepository
                    .findByImei1(ct.getImeis().get(0).trim())
                    .orElseThrow()
                    .getBienTheSanPham();

            // 7b. Tạo ChiTietDonHang - tính giá động nếu client không gửi donGia
            BigDecimal donGia = ct.getDonGia() != null
                    ? ct.getDonGia()
                    : khuyenMaiService.tinhGiaSauKhuyenMai(bienThe.getId(), req.getTongTien());

            ChiTietDonHang chiTiet = ChiTietDonHang.builder()
                    .donHang(savedDonHang)
                    .bienTheSanPham(bienThe)
                    .soLuong(ct.getSoLuong())
                    .donGiaBan(donGia)
                    .build();
            chiTietDonHangRepository.save(chiTiet);

            // 7c. Cập nhật từng IMEI → 'da_ban'
            for (String imei : ct.getImeis()) {
                MayDienThoai may = mayDienThoaiRepository
                        .findByImei1(imei.trim())
                        .orElseThrow(); // Đã validate, không thể null

                may.setTinhTrang(TRANG_THAI_DA_BAN);
                may.setDonHang(savedDonHang);
                mayDienThoaiRepository.save(may);
                log.debug("[POS] Đã mark IMEI [{}] → da_ban.", imei.trim());
            }

            // 7d. Trừ tồn kho Kho Tổng
            TonKho tonKho = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoTong, bienThe)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(
                            "Không tìm thấy tồn kho cho SKU [%s] tại Kho Tổng.",
                            bienThe.getMaSku())));

            int soLuongMoi = tonKho.getSoLuong() - ct.getSoLuong();
            if (soLuongMoi < 0) {
                throw new IllegalArgumentException(String.format(
                        "Tồn kho SKU [%s] không đủ: hiện có %d, cần %d.",
                        bienThe.getMaSku(), tonKho.getSoLuong(), ct.getSoLuong()));
            }
            tonKho.setSoLuong(soLuongMoi);
            tonKho.setUpdatedAt(now);
            tonKhoRepository.save(tonKho);

            log.info("[POS] Trừ kho SKU [{}]: -{} → còn {}.",
                    bienThe.getMaSku(), ct.getSoLuong(), soLuongMoi);
        }

        log.info("[POS] Hoàn tất đơn POS — maDonHang={}, donHangId={}, khachHangId={}",
                savedDonHang.getMaDonHang(), savedDonHang.getId(), khachHang.getId());

        return ResponseEntity.ok(Map.of(
                "donHangId", savedDonHang.getId(),
                "maDonHang", savedDonHang.getMaDonHang(),
                "tongThanh", req.getTongTien().subtract(tienGiam),
                "khachHang", khachHang.getHoTen()));
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

        return BienThePosDto.builder()
                .bienTheId(bt.getId())
                .tenSanPham(sp.getTenSanPham())
                .maSku(bt.getMaSku())
                .mauSac(bt.getMauSac())
                .ramGb(bt.getRamGb())
                .luuTruGb(bt.getLuuTruGb())
                .giaGoc(giaGoc)          // Giá gốc (không khuyến mãi)
                .giaBan(giaSauKM)        // Giá sau khuyến mãi
                .tonKho(tonKho.getSoLuong())
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
        String randPart = String.format("%06d", System.currentTimeMillis() % 1_000_000L);
        return "POS-" + datePart + "-" + randPart;
    }
}