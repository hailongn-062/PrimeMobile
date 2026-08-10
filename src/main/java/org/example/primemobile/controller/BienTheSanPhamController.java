package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller quản lý Biến thể Sản phẩm (SKU).
 * <p>
 * Base path: {@code /api/admin/bien-the} — được bảo vệ bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET    /api/admin/bien-the/san-pham/{sanPhamId}   → Lấy tất cả biến thể của 1 sản phẩm
 *   GET    /api/admin/bien-the/{id}                   → Lấy chi tiết biến thể theo ID
 *   POST   /api/admin/bien-the/san-pham/{sanPhamId}   → Thêm biến thể mới cho sản phẩm
 *                                                         (tự động khởi tạo tồn kho = 0 tại tất cả kho)
 *   PUT    /api/admin/bien-the/{id}                   → Cập nhật thông tin biến thể
 *   PATCH  /api/admin/bien-the/{id}/trang-thai        → Đổi trạng thái biến thể
 * </pre>
 *
 * <h3>Luật quan trọng — Khởi tạo Tồn kho:</h3>
 * <p>
 * Khi gọi {@code POST /api/admin/bien-the/san-pham/{sanPhamId}}, Service sẽ tự động
 * tạo bản ghi {@code ton_kho} với {@code so_luong = 0} tại tất cả kho đang hoạt động.
 * Client KHÔNG cần gọi thêm API nào để khởi tạo tồn kho.
 */
@RestController
@RequestMapping("/api/admin/bien-the")
@RequiredArgsConstructor
public class BienTheSanPhamController {

    private static final Logger log = LoggerFactory.getLogger(BienTheSanPhamController.class);

    private final IBienTheSanPhamService bienTheSanPhamService;

    // =========================================================================
    // GET — Lấy dữ liệu
    // =========================================================================

    /**
     * Lấy tất cả biến thể của một sản phẩm.
     *
     * @param sanPhamId ID sản phẩm cha.
     */
    @GetMapping("/san-pham/{sanPhamId}")
    public ResponseEntity<?> layTheoSanPhamId(@PathVariable Integer sanPhamId) {
        try {
            return ResponseEntity.ok(bienTheSanPhamService.layTheoSanPhamId(sanPhamId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy chi tiết một biến thể theo ID.
     *
     * @param id ID biến thể.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layTheoId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(bienTheSanPhamService.getBienTheSanPham(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST — Thêm mới
    // =========================================================================

    /**
     * Thêm biến thể mới cho sản phẩm và tự động khởi tạo tồn kho.
     * <p>
     * Body JSON tối thiểu:
     * <pre>
     * {
     *   "maSku": "IP16PM-TIT-8-256",
     *   "mauSac": "Titan Đen",
     *   "ramGb": 8,
     *   "luuTruGb": 256,
     *   "giaNhap": 25000000,
     *   "giaBan": 34990000
     * }
     * </pre>
     * Sau khi gọi API này thành công:
     * <ul>
     *   <li>Biến thể được tạo với trạng thái mặc định {@code "con_hang"}.</li>
     *   <li>Tồn kho được khởi tạo {@code so_luong = 0} tại TẤT CẢ kho đang hoạt động.</li>
     * </ul>
     *
     * @param sanPhamId      ID sản phẩm cha (truyền qua URL).
     * @param bienTheSanPham Dữ liệu biến thể từ request body.
     */
    @PostMapping("/san-pham/{sanPhamId}")
    public ResponseEntity<?> them(@PathVariable Integer sanPhamId,
                                  @RequestBody BienTheSanPham bienTheSanPham) {
        log.info("[BienTheSanPhamController] Thêm biến thể — sanPhamId={}, maSku={}",
                sanPhamId, bienTheSanPham.getMaSku());
        try {
            BienTheSanPham saved = bienTheSanPhamService.them(sanPhamId, bienTheSanPham);
            return ResponseEntity.ok(saved);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PUT — Cập nhật
    // =========================================================================

    /**
     * Cập nhật thông tin biến thể.
     * <p>
     * Cho phép cập nhật: giá bán, giá khuyến mãi, RAM, ROM, màu sắc, trọng lượng, pin...
     *
     * @param id             ID biến thể cần cập nhật.
     * @param bienTheSanPham Dữ liệu mới.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody BienTheSanPham bienTheSanPham) {
        log.info("[BienTheSanPhamController] Cập nhật biến thể — id={}", id);
        try {
            BienTheSanPham updated = bienTheSanPhamService.capNhat(id, bienTheSanPham);
            return ResponseEntity.ok(java.util.Map.of(
                    "id", updated.getId(),
                    "maSku", updated.getMaSku(),
                    "message", "Cập nhật thành công"
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
        }
    }

    // =========================================================================
    // PATCH — Đổi trạng thái
    // =========================================================================

    /**
     * Đổi trạng thái biến thể.
     * <p>
     * Trạng thái hợp lệ: {@code "con_hang"} | {@code "het_hang"} | {@code "ngung_kinh_doanh"}.
     * <p>
     * Ví dụ: {@code PATCH /api/admin/bien-the/5/trang-thai?trangThai=het_hang}
     *
     * @param id        ID biến thể.
     * @param trangThai Trạng thái mới.
     */
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id,
                                          @RequestParam String trangThai) {
        log.info("[BienTheSanPhamController] Đổi trạng thái biến thể — id={}, trangThai={}", id, trangThai);
        try {
            BienTheSanPham updated = bienTheSanPhamService.doiTrangThai(id, trangThai);
            return ResponseEntity.ok(java.util.Map.of(
                    "id", updated.getId(),
                    "trangThai", updated.getTrangThai(),
                    "message", "Đổi trạng thái thành công"
            ));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", e.getMessage()));
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
