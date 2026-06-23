package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.service.IDanhMucService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý Danh mục sản phẩm.
 * <p>
 * Base path: {@code /api/admin/danh-muc} — được bảo vệ bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET    /api/admin/danh-muc           → Lấy tất cả danh mục
 *   GET    /api/admin/danh-muc/kich-hoat → Lấy danh mục đang kích hoạt
 *   GET    /api/admin/danh-muc/{id}      → Lấy theo ID
 *   POST   /api/admin/danh-muc           → Thêm mới (slug tự sinh)
 *   PUT    /api/admin/danh-muc/{id}      → Cập nhật (slug tự cập nhật)
 *   PATCH  /api/admin/danh-muc/{id}/vo-hieu-hoa  → Vô hiệu hóa (soft-delete)
 *   PATCH  /api/admin/danh-muc/{id}/kich-hoat-lai → Kích hoạt lại
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/danh-muc")
@RequiredArgsConstructor
public class DanhMucController {

    private static final Logger log = LoggerFactory.getLogger(DanhMucController.class);

    private final IDanhMucService danhMucService;

    // =========================================================================
    // GET — Lấy dữ liệu
    // =========================================================================

    /**
     * Lấy tất cả danh mục (kể cả đã vô hiệu hóa).
     * Dùng cho màn hình quản trị Admin.
     */
    @GetMapping
    public ResponseEntity<?> layTatCa() {
        return ResponseEntity.ok(danhMucService.layTatCa());
    }

    /**
     * Lấy danh sách danh mục đang kích hoạt ({@code kichHoat = true}).
     * Dùng cho Front-end hiển thị menu/filter.
     */
    @GetMapping("/kich-hoat")
    public ResponseEntity<?> layDanhSachKichHoat() {
        return ResponseEntity.ok(danhMucService.layDanhSachKichHoat());
    }

    /**
     * Lấy chi tiết một danh mục theo ID.
     *
     * @param id ID danh mục.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layTheoId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(danhMucService.layTheoId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST — Thêm mới
    // =========================================================================

    /**
     * Thêm danh mục mới.
     * <p>
     * Body JSON tối thiểu: {@code {"tenDanhMuc": "iPhone"}}.
     * Slug được tự động sinh — KHÔNG cần truyền trong request.
     *
     * @param danhMuc Dữ liệu danh mục từ request body.
     */
    @PostMapping
    public ResponseEntity<?> them(@RequestBody DanhMuc danhMuc) {
        log.info("[DanhMucController] Thêm danh mục — ten={}", danhMuc.getTenDanhMuc());
        try {
            DanhMuc saved = danhMucService.them(danhMuc);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PUT — Cập nhật
    // =========================================================================

    /**
     * Cập nhật toàn bộ thông tin danh mục.
     * <p>
     * Slug sẽ tự động cập nhật theo {@code tenDanhMuc} mới.
     *
     * @param id      ID danh mục cần cập nhật.
     * @param danhMuc Dữ liệu mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody DanhMuc danhMuc) {
        log.info("[DanhMucController] Cập nhật danh mục — id={}, tenMoi={}", id, danhMuc.getTenDanhMuc());
        try {
            DanhMuc updated = danhMucService.capNhat(id, danhMuc);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PATCH — Thay đổi trạng thái
    // =========================================================================

    /**
     * Vô hiệu hóa danh mục (soft-delete — đặt {@code kichHoat = false}).
     * Không xóa vật lý để bảo toàn dữ liệu sản phẩm liên kết.
     *
     * @param id ID danh mục cần vô hiệu hóa.
     */
    @PatchMapping("/{id}/vo-hieu-hoa")
    public ResponseEntity<?> voHieuHoa(@PathVariable Integer id) {
        log.info("[DanhMucController] Vô hiệu hóa danh mục — id={}", id);
        try {
            danhMucService.voHieuHoa(id);
            return ResponseEntity.ok("Đã vô hiệu hóa danh mục ID: " + id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Kích hoạt lại danh mục đã vô hiệu hóa (đặt {@code kichHoat = true}).
     *
     * @param id ID danh mục cần kích hoạt lại.
     */
    @PatchMapping("/{id}/kich-hoat-lai")
    public ResponseEntity<?> kichHoatLai(@PathVariable Integer id) {
        log.info("[DanhMucController] Kích hoạt lại danh mục — id={}", id);
        try {
            danhMucService.kichHoatLai(id);
            return ResponseEntity.ok("Đã kích hoạt lại danh mục ID: " + id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
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
