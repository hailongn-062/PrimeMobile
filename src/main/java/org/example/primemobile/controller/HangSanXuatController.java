package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.service.IHangSanXuatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý Hãng sản xuất (thương hiệu điện thoại).
 * <p>
 * Base path: {@code /api/admin/hang-san-xuat} — được bảo vệ bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET    /api/admin/hang-san-xuat       → Lấy tất cả hãng
 *   GET    /api/admin/hang-san-xuat/{id}  → Lấy theo ID
 *   POST   /api/admin/hang-san-xuat       → Thêm mới
 *   PUT    /api/admin/hang-san-xuat/{id}  → Cập nhật
 *   DELETE /api/admin/hang-san-xuat/{id}  → Xóa (chỉ được khi chưa có sản phẩm liên kết)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/hang-san-xuat")
@RequiredArgsConstructor
public class HangSanXuatController {

    private static final Logger log = LoggerFactory.getLogger(HangSanXuatController.class);

    private final IHangSanXuatService hangSanXuatService;

    // =========================================================================
    // GET — Lấy dữ liệu
    // =========================================================================

    /**
     * Lấy danh sách tất cả hãng sản xuất.
     */
    @GetMapping
    public ResponseEntity<?> layTatCa() {
        return ResponseEntity.ok(hangSanXuatService.layTatCa());
    }

    /**
     * Lấy chi tiết một hãng sản xuất theo ID.
     *
     * @param id ID hãng sản xuất.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layTheoId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(hangSanXuatService.layTheoId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST — Thêm mới
    // =========================================================================

    /**
     * Thêm hãng sản xuất mới.
     * <p>
     * Body JSON tối thiểu: {@code {"tenHang": "Apple"}}.
     * Các trường tuỳ chọn: {@code logo}, {@code quocGia}.
     *
     * @param hangSanXuat Dữ liệu hãng từ request body.
     */
    @PostMapping
    public ResponseEntity<?> them(@RequestBody HangSanXuat hangSanXuat) {
        log.info("[HangSanXuatController] Thêm hãng mới — ten={}", hangSanXuat.getTenHang());
        try {
            HangSanXuat saved = hangSanXuatService.them(hangSanXuat);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PUT — Cập nhật
    // =========================================================================

    /**
     * Cập nhật thông tin hãng sản xuất.
     * <p>
     * Cho phép cập nhật: {@code tenHang}, {@code logo}, {@code quocGia}.
     *
     * @param id          ID hãng cần cập nhật.
     * @param hangSanXuat Dữ liệu mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody HangSanXuat hangSanXuat) {
        log.info("[HangSanXuatController] Cập nhật hãng — id={}, tenMoi={}", id, hangSanXuat.getTenHang());
        try {
            HangSanXuat updated = hangSanXuatService.capNhat(id, hangSanXuat);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // DELETE — Xóa
    // =========================================================================

    /**
     * Xóa hãng sản xuất.
     * <p>
     * <b>Lưu ý:</b> Thao tác này sẽ thất bại nếu hãng đang có sản phẩm liên kết
     * (DB constraint foreign key). Response trả về HTTP 400 với message lỗi rõ ràng.
     *
     * @param id ID hãng cần xóa.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> xoa(@PathVariable Integer id) {
        log.info("[HangSanXuatController] Xóa hãng — id={}", id);
        try {
            hangSanXuatService.xoa(id);
            return ResponseEntity.ok("Đã xóa hãng sản xuất ID: " + id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            // Bắt lỗi FK constraint khi hãng đang có sản phẩm liên kết
            log.error("[HangSanXuatController] Không thể xóa hãng id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(
                    "Không thể xóa hãng này vì đang có sản phẩm liên kết. " +
                    "Vui lòng xóa hoặc chuyển sản phẩm sang hãng khác trước.");
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
