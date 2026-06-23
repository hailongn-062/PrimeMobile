package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.service.ISanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý Sản phẩm (Model điện thoại).
 * <p>
 * Base path: {@code /api/admin/san-pham} — được bảo vệ bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET    /api/admin/san-pham                        → Lấy danh sách có phân trang + lọc
 *   GET    /api/admin/san-pham/{id}                   → Lấy chi tiết theo ID
 *   POST   /api/admin/san-pham                        → Thêm mới
 *   PUT    /api/admin/san-pham/{id}                   → Cập nhật thông tin
 *   PATCH  /api/admin/san-pham/{id}/trang-thai        → Đổi trạng thái
 * </pre>
 *
 * <h3>Ví dụ request GET có phân trang và lọc:</h3>
 * <pre>
 *   GET /api/admin/san-pham?page=0&amp;size=10&amp;sort=ngayTao,desc&amp;danhMucId=1&amp;hangSanXuatId=2
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/san-pham")
@RequiredArgsConstructor
public class SanPhamController {

    private static final Logger log = LoggerFactory.getLogger(SanPhamController.class);

    private final ISanPhamService sanPhamService;

    // =========================================================================
    // GET — Lấy dữ liệu
    // =========================================================================

    /**
     * Lấy danh sách sản phẩm có phân trang và lọc tùy chọn.
     *
     * @param danhMucId      (optional) Lọc theo ID danh mục.
     * @param hangSanXuatId  (optional) Lọc theo ID hãng sản xuất.
     * @param page           Số trang (bắt đầu từ 0, mặc định 0).
     * @param size           Số bản ghi mỗi trang (mặc định 10).
     * @param sort           Sắp xếp theo trường, ví dụ: {@code ngayTao,desc}.
     */
    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            @RequestParam(defaultValue = "0")   int page,
            @RequestParam(defaultValue = "10")  int size,
            @RequestParam(defaultValue = "ngayTao,desc") String sort) {

        // Parse sort parameter (format: "field,direction")
        Pageable pageable = buildPageable(page, size, sort);
        Page<SanPham> result = sanPhamService.layDanhSach(danhMucId, hangSanXuatId, pageable);
        return ResponseEntity.ok(result);
    }

    /**
     * Lấy chi tiết một sản phẩm theo ID.
     *
     * @param id ID sản phẩm.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layTheoId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(sanPhamService.layTheoId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST — Thêm mới
    // =========================================================================

    /**
     * Thêm sản phẩm mới.
     * <p>
     * Body JSON tối thiểu:
     * <pre>
     * {
     *   "maSanPham": "IP16PM",
     *   "tenSanPham": "iPhone 16 Pro Max",
     *   "danhMuc": {"id": 1},
     *   "hangSanXuat": {"id": 2},
     *   "namRaMat": 2024
     * }
     * </pre>
     */
    @PostMapping
    public ResponseEntity<?> them(@RequestBody SanPham sanPham) {
        log.info("[SanPhamController] Thêm sản phẩm — ma={}", sanPham.getMaSanPham());
        try {
            return ResponseEntity.ok(sanPhamService.them(sanPham));
        } catch (IllegalArgumentException | EntityNotFoundException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PUT — Cập nhật
    // =========================================================================

    /**
     * Cập nhật thông tin sản phẩm.
     *
     * @param id      ID sản phẩm cần cập nhật.
     * @param sanPham Dữ liệu mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody SanPham sanPham) {
        log.info("[SanPhamController] Cập nhật sản phẩm — id={}", id);
        try {
            return ResponseEntity.ok(sanPhamService.capNhat(id, sanPham));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PATCH — Đổi trạng thái
    // =========================================================================

    /**
     * Đổi trạng thái sản phẩm (soft-delete = ngung_ban).
     * <p>
     * Trạng thái hợp lệ: {@code "dang_ban"} | {@code "ngung_ban"} | {@code "sap_ra_mat"}.
     * <p>
     * Ví dụ: {@code PATCH /api/admin/san-pham/1/trang-thai?trangThai=ngung_ban}
     *
     * @param id        ID sản phẩm.
     * @param trangThai Trạng thái mới.
     */
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id,
                                          @RequestParam String trangThai) {
        log.info("[SanPhamController] Đổi trạng thái sản phẩm — id={}, trangThai={}", id, trangThai);
        try {
            return ResponseEntity.ok(sanPhamService.doiTrangThai(id, trangThai));
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

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    /**
     * Parse chuỗi sort "field,direction" thành {@link Pageable}.
     * Mặc định sort theo {@code ngayTao DESC} nếu format không hợp lệ.
     */
    private Pageable buildPageable(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            return PageRequest.of(page, size, Sort.by(dir, parts[0].trim()));
        } catch (Exception e) {
            return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayTao"));
        }
    }
}
