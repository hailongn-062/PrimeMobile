package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.service.IKhachHangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý Khách hàng — dành cho Admin.
 * <p>
 * Base path: {@code /api/admin/khach-hang} — được bảo vệ bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET  /api/admin/khach-hang            → Tìm kiếm theo SĐT hoặc email (?tuKhoa=...)
 *   GET  /api/admin/khach-hang/{id}       → Xem chi tiết một khách hàng
 *   PUT  /api/admin/khach-hang/{id}       → Cập nhật thông tin khách hàng
 * </pre>
 *
 * <h3>Lưu ý:</h3>
 * Không có endpoint DELETE — không cho phép xóa vật lý khách hàng
 * để bảo toàn lịch sử đơn hàng liên quan.
 */
@RestController
@RequestMapping("/api/admin/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private static final Logger log = LoggerFactory.getLogger(KhachHangController.class);

    private final IKhachHangService khachHangService;

    // =========================================================================
    // GET
    // =========================================================================

    /**
     * Tìm kiếm khách hàng theo từ khóa (SĐT hoặc email).
     * <p>
     * Ví dụ: {@code GET /api/admin/khach-hang?tuKhoa=0912}
     * <p>
     * Không truyền {@code tuKhoa} hoặc để rỗng → trả về tất cả khách hàng.
     *
     * @param tuKhoa Từ khóa tìm kiếm. Optional.
     */
    @GetMapping
    public ResponseEntity<?> timKiem(@RequestParam(required = false) String tuKhoa) {
        return ResponseEntity.ok(khachHangService.timKiem(tuKhoa));
    }

    /**
     * Xem chi tiết một khách hàng theo ID.
     * Trả về đầy đủ thông tin bao gồm hạng thành viên, điểm tích lũy, tổng chi tiêu.
     *
     * @param id ID khách hàng.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layTheoId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(khachHangService.layTheoId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // PUT
    // =========================================================================

    /**
     * Cập nhật thông tin hồ sơ khách hàng.
     * <p>
     * Các trường được phép cập nhật: {@code hoTen}, {@code email},
     * {@code soDienThoai}, {@code gioiTinh}, {@code ngaySinh}.
     * <p>
     * Body JSON ví dụ:
     * <pre>
     * {
     *   "hoTen": "Nguyễn Văn An",
     *   "email": "an.moi@gmail.com",
     *   "soDienThoai": "0912345679",
     *   "gioiTinh": "Nam",
     *   "ngaySinh": "1995-05-20"
     * }
     * </pre>
     *
     * @param id        ID khách hàng cần cập nhật.
     * @param khachHang Dữ liệu mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody KhachHang khachHang) {
        log.info("[KhachHangController] Cập nhật khách hàng — id={}", id);
        try {
            return ResponseEntity.ok(khachHangService.capNhat(id, khachHang));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
