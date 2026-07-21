package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.ChiTietDonHang;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.DonHangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * REST Controller lịch sử đơn hàng dành cho Khách Hàng đã đăng nhập.
 * <p>
 * Base path: {@code /api/public/don-hang-cua-toi} — Public endpoint, không bảo
 * vệ
 * bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Bảo mật được thực hiện thủ công tại từng method:
 * <ul>
 * <li>Kiểm tra {@code CURRENT_CUSTOMER} session → HTTP 401 nếu chưa đăng
 * nhập.</li>
 * <li>Kiểm tra đơn hàng thuộc đúng khách hàng trong session → HTTP 403 nếu
 * IDOR.</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * 
 * <pre>
 *   GET /api/public/don-hang-cua-toi            → Danh sách đơn hàng của khách đang đăng nhập
 *   GET /api/public/don-hang-cua-toi/{donHangId} → Chi tiết 1 đơn hàng (kiểm tra ownership)
 * </pre>
 */
@RestController
@RequestMapping("/api/public/don-hang-cua-toi")
@RequiredArgsConstructor
public class DonHangKhachHangController {

    private static final Logger log = LoggerFactory.getLogger(DonHangKhachHangController.class);

    private final DonHangRepository donHangRepository;
    private final org.example.primemobile.repository.MayDienThoaiRepository mayDienThoaiRepository;

    // =========================================================================
    // GET /api/public/don-hang-cua-toi — Lịch sử đơn hàng
    // =========================================================================

    /**
     * Lấy toàn bộ lịch sử đơn hàng của khách hàng đang đăng nhập.
     * <p>
     * Kết quả sắp xếp mới nhất lên đầu (ORDER BY ngay_dat DESC).
     * <p>
     * Ví dụ: {@code GET /api/public/don-hang-cua-toi}
     * <p>
     * Yêu cầu: Session {@code "CURRENT_CUSTOMER"} phải tồn tại (đã đăng nhập).
     *
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm danh sách {@link DonHang}; HTTP 401 nếu chưa đăng nhập.
     */
    @GetMapping
    public ResponseEntity<?> layLichSuDonHang(HttpServletRequest httpRequest) {

        SessionKhachHang session = requireLogin(httpRequest);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(buildErrorResponse("Bạn cần đăng nhập để xem lịch sử đơn hàng."));
        }

        log.info("[DonHangKhachHang] Lấy lịch sử — khachHangId={}", session.getKhachHangId());

        List<Map<String, Object>> danhSach = donHangRepository
                .findByKhachHangIdOrderByNgayDatDesc(session.getKhachHangId())
                .stream()
                .map(this::buildOrderSummary)
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("khachHangId", session.getKhachHangId());
        response.put("tongSoDonHang", danhSach.size());
        response.put("danhSachDonHang", danhSach);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/public/don-hang-cua-toi/{donHangId} — Chi tiết đơn hàng
    // =========================================================================

    /**
     * Lấy chi tiết 1 đơn hàng của khách hàng đang đăng nhập.
     * <p>
     * Bảo vệ IDOR: Kiểm tra đơn hàng phải thuộc đúng khách hàng trong session.
     * Trả về HTTP 403 nếu khách cố xem đơn của người khác.
     * <p>
     * Ví dụ: {@code GET /api/public/don-hang-cua-toi/42}
     *
     * @param donHangId   ID đơn hàng cần xem.
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm {@link DonHang} chi tiết (kèm danh sách sản phẩm);
     *         HTTP 401 nếu chưa đăng nhập;
     *         HTTP 403 nếu đơn không thuộc về mình;
     *         HTTP 404 nếu đơn không tồn tại.
     */
    @GetMapping("/{donHangId}")
    public ResponseEntity<?> layChiTietDonHang(
            @PathVariable Integer donHangId,
            HttpServletRequest httpRequest) {

        SessionKhachHang session = requireLogin(httpRequest);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(buildErrorResponse("Bạn cần đăng nhập để xem chi tiết đơn hàng."));
        }

        log.info("[DonHangKhachHang] Xem chi tiết — donHangId={}, khachHangId={}",
                donHangId, session.getKhachHangId());

        // Lấy chi tiết đơn hàng kèm danh sách sản phẩm (FETCH JOIN để tránh N+1)
        DonHang donHang = donHangRepository.findByIdWithDetails(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng #" + donHangId));

        // Bảo vệ IDOR: Đơn hàng phải thuộc về khách hàng đang đăng nhập
        if (!donHang.getKhachHang().getId().equals(session.getKhachHangId())) {
            log.warn("[DonHangKhachHang] 403 IDOR — donHangId={} không thuộc khachHangId={}",
                    donHangId, session.getKhachHangId());
            return ResponseEntity.status(403)
                    .body(buildErrorResponse("Bạn không có quyền xem đơn hàng này."));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("donHang", buildOrderDetail(donHang));
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // Exception Handler cục bộ
    // =========================================================================

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PUT /api/public/don-hang-cua-toi/{donHangId}/huy — Tự hủy đơn hàng
    // =========================================================================

    /**
     * Khách hàng tự hủy đơn hàng của mình.
     * <p>
     * Điều kiện bắt buộc (system_rules.md §2.2):
     * - Đơn hàng phải đang ở trạng thái 'cho_xac_nhan'.
     * - Không áp dụng hoàn kho vì lúc này kho_tong chưa bị trừ.
     *
     * @param donHangId ID đơn hàng cần hủy.
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm đơn hàng đã hủy.
     */
    @PutMapping("/{donHangId}/huy")
    public ResponseEntity<?> huyDonHang(
            @PathVariable Integer donHangId,
            HttpServletRequest httpRequest) {

        SessionKhachHang session = requireLogin(httpRequest);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(buildErrorResponse("Bạn cần đăng nhập để thực hiện thao tác này."));
        }

        log.info("[DonHangKhachHang] Khách yêu cầu hủy đơn — donHangId={}, khachHangId={}",
                donHangId, session.getKhachHangId());

        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng #" + donHangId));

        // Bảo vệ IDOR
        if (!donHang.getKhachHang().getId().equals(session.getKhachHangId())) {
            log.warn("[DonHangKhachHang] 403 IDOR — donHangId={} không thuộc khachHangId={}",
                    donHangId, session.getKhachHangId());
            return ResponseEntity.status(403)
                    .body(buildErrorResponse("Bạn không có quyền thao tác trên đơn hàng này."));
        }

        // Kiểm tra trạng thái cho phép hủy
        if (!"cho_xac_nhan".equals(donHang.getTrangThai())) {
            return ResponseEntity.badRequest()
                    .body(buildErrorResponse("Chỉ có thể hủy đơn hàng khi đang chờ xác nhận. " +
                            "Đơn hàng của bạn đang ở trạng thái: " + donHang.getTrangThai()));
        }

        // Cập nhật trạng thái
        donHang.setUpdatedAt(java.time.LocalDateTime.now());
        
        String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
        donHang.setGhiChu(ghiChuCu + "[KHÁCH HÀNG TỰ HỦY " + donHang.getUpdatedAt().toLocalDate() + "]");

        if ("da_thanh_toan".equals(donHang.getTrangThaiThanhToan())) {
            // Đã thanh toán -> chuyển sang chờ hoàn tiền
            donHang.setTrangThai("cho_hoan_tien");
        } else {
            // Chưa thanh toán -> Hủy luôn
            donHang.setTrangThai("da_huy");
            
            // Nếu thanh toán VNPay đang treo thì chuyển thành thất bại
            if ("dang_chuyen_huong".equals(donHang.getTrangThaiThanhToan())) {
                donHang.setTrangThaiThanhToan("that_bai");
            } else {
                donHang.setTrangThaiThanhToan("chua_thanh_toan");
            }
        }

        donHangRepository.save(donHang);
        log.info("[DonHangKhachHang] ✅ Hủy đơn thành công — maDonHang={}", donHang.getMaDonHang());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("message", "Hủy đơn hàng thành công.");
        response.put("donHang", buildOrderSummary(donHang));

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Lấy {@link SessionKhachHang} từ HttpSession hiện tại.
     * Trả về {@code null} nếu khách chưa đăng nhập hoặc session không hợp lệ.
     */
    private SessionKhachHang requireLogin(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null)
            return null;
        Object attr = session.getAttribute(SessionKhachHang.SESSION_KEY);
        return (attr instanceof SessionKhachHang kh) ? kh : null;
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }

    private Map<String, Object> buildOrderSummary(DonHang donHang) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", donHang.getId());
        r.put("maDonHang", donHang.getMaDonHang());
        r.put("ngayDat", donHang.getNgayDat());
        r.put("trangThai", donHang.getTrangThai());
        r.put("trangThaiText", labelTrangThai(donHang.getTrangThai()));
        r.put("trangThaiThanhToan", donHang.getTrangThaiThanhToan());
        r.put("trangThaiThanhToanText", labelThanhToan(donHang.getTrangThaiThanhToan()));
        r.put("kenhBan", donHang.getKenhBan());
        r.put("tongTienHang", valueOrZero(donHang.getTongTienHang()));
        r.put("tienGiamGia", valueOrZero(donHang.getTienGiamGia()));
        r.put("phiShip", valueOrZero(donHang.getPhiShip()));
        r.put("tongThanhToan", calculateTotal(donHang));
        r.put("nguoiNhan", donHang.getHoTenNguoiNhan());
        r.put("soDienThoaiNhan", donHang.getSdtNguoiNhan());
        r.put("diaChiNhan", buildAddress(donHang));
        r.put("ghiChu", donHang.getGhiChu());
        r.put("coTheHuy", "cho_xac_nhan".equals(donHang.getTrangThai()));
        
        // Add chiTiet directly to summary so UI can render products outside modal
        List<Map<String, Object>> items = donHang.getChiTietDonHangs().stream()
                .map(ct -> buildOrderItemSimplified(ct))
                .toList();
        r.put("chiTiet", items);
        
        return r;
    }

    private Map<String, Object> buildOrderItemSimplified(ChiTietDonHang chiTiet) {
        BienTheSanPham bienThe = chiTiet.getBienTheSanPham();
        SanPham sanPham = bienThe != null ? bienThe.getSanPham() : null;
        
        String anhChinhUrl = null;
        if (bienThe != null && !bienThe.getHinhAnhSanPhams().isEmpty()) {
            anhChinhUrl = bienThe.getHinhAnhSanPhams().stream()
                    .filter(org.example.primemobile.entity.HinhAnhSanPham::getLaAnhChinh)
                    .findFirst()
                    .map(org.example.primemobile.entity.HinhAnhSanPham::getDuongDan)
                    .orElse(bienThe.getHinhAnhSanPhams().get(0).getDuongDan());
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", chiTiet.getId());
        r.put("bienTheId", bienThe != null ? bienThe.getId() : null);
        r.put("sanPhamId", sanPham != null ? sanPham.getId() : null);
        r.put("tenSanPham", sanPham != null ? sanPham.getTenSanPham() : "Sản phẩm");
        r.put("mauSac", bienThe != null ? bienThe.getMauSac() : null);
        r.put("ramGb", bienThe != null ? bienThe.getRamGb() : null);
        r.put("luuTruGb", bienThe != null ? bienThe.getLuuTruGb() : null);
        r.put("soLuong", chiTiet.getSoLuong());
        r.put("donGiaBan", valueOrZero(chiTiet.getDonGiaBan()));
        r.put("thanhTien", chiTiet.getThanhTien() != null
                ? chiTiet.getThanhTien()
                : valueOrZero(chiTiet.getDonGiaBan()).multiply(BigDecimal.valueOf(chiTiet.getSoLuong())));
        r.put("anhChinh", anhChinhUrl);
        return r;
    }

    private Map<String, Object> buildOrderDetail(DonHang donHang) {
        Map<String, Object> r = buildOrderSummary(donHang);
        
        List<org.example.primemobile.entity.MayDienThoai> allImeis = mayDienThoaiRepository.findByDonHangId(donHang.getId());
        
        List<Map<String, Object>> items = donHang.getChiTietDonHangs().stream()
                .map(ct -> buildOrderItem(ct, allImeis))
                .toList();
        r.put("chiTiet", items);
        
        if (donHang.getNguoiXuLy() != null) {
            r.put("nhanVienXuLy", donHang.getNguoiXuLy().getHoTen());
        }
        String emailKhach = null;
        if (donHang.getKhachHang() != null) {
            emailKhach = donHang.getKhachHang().getEmail();
            if ((emailKhach == null || emailKhach.trim().isEmpty()) && donHang.getKhachHang().getNguoiDung() != null) {
                emailKhach = donHang.getKhachHang().getNguoiDung().getEmail();
            }
        }
        if (emailKhach == null || emailKhach.trim().isEmpty()) {
            emailKhach = donHang.getEmailNguoiNhan();
        }
        if (emailKhach != null && emailKhach.trim().isEmpty()) {
            emailKhach = null;
        }
        r.put("emailNhan", emailKhach);
        
        return r;
    }

    private Map<String, Object> buildOrderItem(ChiTietDonHang chiTiet, List<org.example.primemobile.entity.MayDienThoai> allImeis) {
        BienTheSanPham bienThe = chiTiet.getBienTheSanPham();
        SanPham sanPham = bienThe != null ? bienThe.getSanPham() : null;

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", chiTiet.getId());
        r.put("bienTheId", bienThe != null ? bienThe.getId() : null);
        r.put("sanPhamId", sanPham != null ? sanPham.getId() : null);
        r.put("tenSanPham", sanPham != null ? sanPham.getTenSanPham() : "Sản phẩm");
        r.put("maSku", bienThe != null ? bienThe.getMaSku() : null);
        r.put("mauSac", bienThe != null ? bienThe.getMauSac() : null);
        r.put("ramGb", bienThe != null ? bienThe.getRamGb() : null);
        r.put("luuTruGb", bienThe != null ? bienThe.getLuuTruGb() : null);
        r.put("soLuong", chiTiet.getSoLuong());
        r.put("donGiaBan", valueOrZero(chiTiet.getDonGiaBan()));
        r.put("giaGoc", bienThe != null ? valueOrZero(bienThe.getGiaBan()) : valueOrZero(chiTiet.getDonGiaBan()));
        r.put("thanhTien", chiTiet.getThanhTien() != null
                ? chiTiet.getThanhTien()
                : valueOrZero(chiTiet.getDonGiaBan()).multiply(BigDecimal.valueOf(chiTiet.getSoLuong())));
                
        List<String> imeiList = allImeis.stream()
                .filter(m -> m.getBienTheSanPham() != null && bienThe != null && m.getBienTheSanPham().getId().equals(bienThe.getId()))
                .map(org.example.primemobile.entity.MayDienThoai::getImei1)
                .toList();
        r.put("imeiList", imeiList);
        
        return r;
    }

    private BigDecimal calculateTotal(DonHang donHang) {
        if (donHang.getTongThanhToan() != null) {
            return donHang.getTongThanhToan();
        }
        return valueOrZero(donHang.getTongTienHang())
                .subtract(valueOrZero(donHang.getTienGiamGia()))
                .add(valueOrZero(donHang.getPhiShip()));
    }

    private BigDecimal valueOrZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String buildAddress(DonHang donHang) {
        return Stream.of(
                        donHang.getDiaChiGiaCuThe(),
                        donHang.getPhuongXaGiao(),
                        donHang.getQuanHuyenGiao(),
                        donHang.getTinhThanhGiao()
                )
                .filter(part -> part != null && !part.isBlank())
                .reduce((left, right) -> left + ", " + right)
                .orElse("");
    }

    private String labelTrangThai(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "cho_xac_nhan" -> "Chờ xác nhận";
            case "da_xac_nhan" -> "Đã xác nhận";
            case "dang_giao" -> "Đang giao";
            case "da_hoan_thanh" -> "Đã giao";
            case "da_huy" -> "Đã hủy";
            default -> status;
        };
    }

    private String labelThanhToan(String status) {
        if (status == null) {
            return "";
        }
        return switch (status) {
            case "chua_thanh_toan" -> "Chưa thanh toán";
            case "dang_chuyen_huong" -> "Đang thanh toán";
            case "da_thanh_toan" -> "Đã thanh toán";
            case "that_bai" -> "Thanh toán thất bại";
            default -> status;
        };
    }
}
