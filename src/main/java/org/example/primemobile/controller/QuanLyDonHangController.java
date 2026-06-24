package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.service.IQuanLyDonHangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller cho phân hệ Quản lý Đơn Hàng (dành cho Nhân viên / Admin).
 * <p>
 * Base path: {@code /api/admin/don-hang} — được bảo vệ tự động bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (pattern {@code /api/admin/**}).
 * Mọi request phải có session hợp lệ với vai trò {@code NhanVien} hoặc {@code Admin}.
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   GET  /api/admin/don-hang              → Danh sách đơn hàng (phân trang, lọc)
 *   GET  /api/admin/don-hang/{id}         → Chi tiết 1 đơn hàng kèm sản phẩm
 *   POST /api/admin/don-hang/{id}/xac-nhan      → Xác nhận đơn (trừ kho_online)
 *   PUT  /api/admin/don-hang/{id}/trang-thai    → Cập nhật lộ trình giao hàng
 *   POST /api/admin/don-hang/{id}/huy           → Hủy đơn (kèm hoàn kho nếu cần)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/don-hang")
@RequiredArgsConstructor
public class QuanLyDonHangController {

    private static final Logger log = LoggerFactory.getLogger(QuanLyDonHangController.class);

    private final IQuanLyDonHangService quanLyDonHangService;

    // =========================================================================
    // GET /api/admin/don-hang — Danh sách đơn hàng
    // =========================================================================

    /**
     * Lấy danh sách đơn hàng có phân trang, hỗ trợ lọc đa tiêu chí.
     *
     * <h3>Query params:</h3>
     * <ul>
     *   <li>{@code trangThai}   — Lọc theo trạng thái (null = tất cả)</li>
     *   <li>{@code maDonHang}   — Tìm LIKE theo mã đơn (null = bỏ qua)</li>
     *   <li>{@code soDienThoai} — Tìm LIKE theo SĐT khách (null = bỏ qua)</li>
     *   <li>{@code page}        — Số trang (0-indexed, mặc định = 0)</li>
     *   <li>{@code size}        — Kích thước trang (mặc định = 20)</li>
     * </ul>
     *
     * Ví dụ: {@code GET /api/admin/don-hang?trangThai=cho_xac_nhan&page=0&size=10}
     */
    @GetMapping
    public ResponseEntity<?> layDanhSachDonHang(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maDonHang,
            @RequestParam(required = false) String soDienThoai,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] Lấy danh sách — nhanVienId={}, trangThai={}, page={}/{}",
                sessionUser.getId(), trangThai, page, size);

        // Sắp xếp mặc định: đơn hàng mới nhất lên đầu
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDat"));

        Page<DonHang> ketQua = quanLyDonHangService.layDanhSachDonHang(
                trangThai, maDonHang, soDienThoai, pageable);

        // Build response wrapper với metadata phân trang
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("danhSachDonHang", ketQua.getContent());
        response.put("tongSoDonHang",   ketQua.getTotalElements());
        response.put("tongSoTrang",     ketQua.getTotalPages());
        response.put("trangHienTai",    ketQua.getNumber());
        response.put("kichThuocTrang",  ketQua.getSize());

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/admin/don-hang/{id} — Chi tiết đơn hàng
    // =========================================================================

    /**
     * Lấy chi tiết 1 đơn hàng kèm toàn bộ danh sách sản phẩm bên trong (FETCH JOIN).
     *
     * @param id ID của đơn hàng cần xem.
     * @return HTTP 200 kèm {@link DonHang} đã load đầy đủ.
     *         HTTP 404 nếu không tìm thấy.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTietDonHang(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] Xem chi tiết — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.layChiTietDonHang(id);
            return ResponseEntity.ok(donHang);

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] Không tìm thấy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/xac-nhan — Xác nhận đơn hàng
    // =========================================================================

    /**
     * Xác nhận đơn hàng online — bước nghiệp vụ quan trọng nhất.
     * <p>
     * Thực hiện trong 1 transaction:
     * <ol>
     *   <li>Kiểm tra trạng thái phải là {@code "cho_xac_nhan"}.</li>
     *   <li>Fail-Fast: validate toàn bộ SKU xem kho_online có đủ (Safety Stock §3.1).</li>
     *   <li>Trừ thực tế vào ton_kho của kho_online.</li>
     *   <li>Chuyển trạng thái đơn → {@code "da_xac_nhan"}.</li>
     * </ol>
     *
     * @param id          ID đơn hàng cần xác nhận.
     * @param sessionUser Nhân viên đang đăng nhập (lấy từ session, không cần truyền body).
     * @return HTTP 200 kèm đơn hàng đã xác nhận.
     *         HTTP 400 nếu sai trạng thái hoặc vi phạm Safety Stock.
     *         HTTP 404 nếu đơn hoặc kho không tồn tại.
     */
    @PostMapping("/{id}/xac-nhan")
    public ResponseEntity<?> xacNhanDonHang(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Xác nhận đơn — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.xacNhanDonHang(id, sessionUser.getId());
            log.info("[QuanLyDonHang] ✅ Xác nhận thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Xác nhận đơn hàng thành công. Kho online đã được trừ.", donHang));

        } catch (IllegalArgumentException e) {
            // Sai trạng thái hoặc vi phạm Safety Stock §3.1
            log.warn("[QuanLyDonHang] ❌ Xác nhận thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy tài nguyên khi xác nhận: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // PUT /api/admin/don-hang/{id}/trang-thai — Cập nhật lộ trình giao hàng
    // =========================================================================

    /**
     * Cập nhật lộ trình giao hàng theo chiều tiến: da_xac_nhan → dang_giao → da_giao.
     * <p>
     * Ví dụ: {@code PUT /api/admin/don-hang/5/trang-thai?trangThaiMoi=dang_giao}
     *
     * @param id           ID đơn hàng cần cập nhật.
     * @param trangThaiMoi Trạng thái mới muốn chuyển sang ("dang_giao" hoặc "da_giao").
     * @param sessionUser  Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã cập nhật.
     *         HTTP 400 nếu luồng chuyển trạng thái không hợp lệ.
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable  Integer id,
            @RequestParam  String  trangThaiMoi,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Cập nhật trạng thái — donHangId={}, trangThaiMoi={}, nhanVienId={}",
                id, trangThaiMoi, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.capNhatTrangThai(id, trangThaiMoi);
            log.info("[QuanLyDonHang] ✅ Cập nhật thành công — maDonHang={}, trangThai={}",
                    donHang.getMaDonHang(), donHang.getTrangThai());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Cập nhật trạng thái giao hàng thành công.", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Cập nhật trạng thái thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/huy — Hủy đơn hàng
    // =========================================================================

    /**
     * Hủy đơn hàng kèm hoàn kho tự động nếu cần (system_rules.md §2.2.7).
     * <p>
     * Body JSON (optional):
     * <pre>{@code
     * {
     *   "lyDoHuy": "Khách yêu cầu hủy qua điện thoại"
     * }
     * }</pre>
     * Nếu không truyền body, lý do hủy mặc định là "Nhân viên hủy đơn".
     *
     * @param id          ID đơn hàng cần hủy.
     * @param body        Map chứa field {@code lyDoHuy} (optional).
     * @param sessionUser Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã hủy.
     *         HTTP 400 nếu đơn không thể hủy (đã giao hoặc đã hủy trước đó).
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PostMapping("/{id}/huy")
    public ResponseEntity<?> huyDonHang(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        String lyDoHuy = (body != null && body.containsKey("lyDoHuy"))
                ? body.get("lyDoHuy")
                : "Nhân viên hủy đơn (ID: " + sessionUser.getId() + ")";

        log.info("[QuanLyDonHang] ▶ Hủy đơn — donHangId={}, nhanVienId={}, lyDo={}",
                id, sessionUser.getId(), lyDoHuy);
        try {
            DonHang donHang = quanLyDonHangService.huyDonHang(id, lyDoHuy);
            log.info("[QuanLyDonHang] ✅ Hủy thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Hủy đơn hàng thành công. Kho online đã được hoàn trả (nếu cần).", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Không thể hủy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy tài nguyên khi hủy id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[QuanLyDonHang] Handler toàn cục — IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("[QuanLyDonHang] Handler toàn cục — IllegalStateException: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Tạo response body thành công theo chuẩn thống nhất.
     */
    private Map<String, Object> buildSuccessResponse(String message, DonHang donHang) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", message);
        res.put("donHang", donHang);
        return res;
    }

    /**
     * Tạo response body lỗi theo chuẩn thống nhất.
     */
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}
