package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.IHangSanXuatService;
import org.example.primemobile.service.ISanPhamService;
import org.example.primemobile.service.IThongSoKyThuatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller công khai — Catalog dữ liệu sản phẩm dành cho Frontend (Khách vãng lai).
 * <p>
 * Base path: {@code /api/public/catalog} — KHÔNG được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (chỉ chặn
 * {@code /api/admin/**} và {@code /admin/**}).
 * <p>
 * Mọi endpoint ở đây chỉ là <b>READ-ONLY</b> (GET). Không có write operation.
 * Các endpoint tương ứng trên {@code /api/admin/} vẫn còn nguyên và yêu cầu session.
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   ── Sản phẩm ──────────────────────────────────────────────────────────────
 *   GET /api/public/catalog/san-pham
 *       ?danhMucId=&hangSanXuatId=&page=0&size=10&sort=ngayTao,desc
 *       → Danh sách sản phẩm đang bán, có phân trang và lọc
 *
 *   GET /api/public/catalog/san-pham/{id}
 *       → Chi tiết 1 sản phẩm theo ID
 *
 *   ── Biến thể SKU ──────────────────────────────────────────────────────────
 *   GET /api/public/catalog/bien-the/san-pham/{sanPhamId}
 *       → Tất cả biến thể (màu sắc, RAM, ROM, giá) của 1 sản phẩm
 *
 *   GET /api/public/catalog/bien-the/{id}
 *       → Chi tiết 1 biến thể theo ID (bao gồm giá, tồn kho...)
 *
 *   ── Thông số kỹ thuật ─────────────────────────────────────────────────────
 *   GET /api/public/catalog/thong-so/{sanPhamId}
 *       → Toàn bộ thông số kỹ thuật của 1 sản phẩm
 *
 *   ── Danh mục & Hãng (dùng cho thanh lọc Frontend) ────────────────────────
 *   GET /api/public/catalog/danh-muc
 *       → Danh sách danh mục đang kích hoạt (dùng làm filter nav)
 *
 *   GET /api/public/catalog/hang-san-xuat
 *       → Danh sách tất cả hãng sản xuất (dùng làm filter)
 * </pre>
 */
@RestController
@RequestMapping("/api/public/catalog")
@RequiredArgsConstructor
public class CatalogPublicController {

    private static final Logger log = LoggerFactory.getLogger(CatalogPublicController.class);

    private final ISanPhamService          sanPhamService;
    private final IBienTheSanPhamService   bienTheSanPhamService;
    private final IThongSoKyThuatService   thongSoKyThuatService;
    private final IDanhMucService          danhMucService;
    private final IHangSanXuatService      hangSanXuatService;

    // =========================================================================
    // SẢN PHẨM
    // =========================================================================

    /**
     * Lấy danh sách sản phẩm có phân trang và lọc — dành cho trang chủ / trang danh sách.
     * <p>
     * Ví dụ:
     * <ul>
     *   <li>{@code GET /api/public/catalog/san-pham} → Tất cả</li>
     *   <li>{@code GET /api/public/catalog/san-pham?danhMucId=1&page=0&size=12}</li>
     *   <li>{@code GET /api/public/catalog/san-pham?hangSanXuatId=2&sort=giaBan,asc}</li>
     * </ul>
     *
     * @param danhMucId     (optional) Lọc theo ID danh mục.
     * @param hangSanXuatId (optional) Lọc theo ID hãng sản xuất.
     * @param page          Số trang, bắt đầu từ 0 (mặc định 0).
     * @param size          Số bản ghi mỗi trang (mặc định 12).
     * @param sort          Sắp xếp theo trường, ví dụ {@code "giaBan,asc"} (mặc định mới nhất lên đầu).
     */
    @GetMapping("/san-pham")
    public ResponseEntity<?> layDanhSachSanPham(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            @RequestParam(defaultValue = "0")              int    page,
            @RequestParam(defaultValue = "12")             int    size,
            @RequestParam(defaultValue = "ngayTao,desc")   String sort) {

        log.debug("[CatalogPublic] GET /san-pham — danhMucId={}, hangSanXuatId={}, page={}, size={}",
                danhMucId, hangSanXuatId, page, size);

        Pageable pageable = buildPageable(page, size, sort);
        // layDanhSachCongKhai chỉ trả sản phẩm trangThai = 'dang_ban'
        return ResponseEntity.ok(sanPhamService.layDanhSachCongKhai(danhMucId, hangSanXuatId, pageable));
    }

    /**
     * Lấy chi tiết một sản phẩm theo ID — dành cho trang chi tiết sản phẩm.
     *
     * @param id ID sản phẩm.
     * @return HTTP 200 kèm {@code SanPham}. HTTP 404 nếu không tìm thấy.
     */
    @GetMapping("/san-pham/{id}")
    public ResponseEntity<?> layChiTietSanPham(@PathVariable Integer id) {
        log.debug("[CatalogPublic] GET /san-pham/{}", id);
        try {
            return ResponseEntity.ok(sanPhamService.layTheoId(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // BIẾN THỂ SKU
    // =========================================================================

    /**
     * Lấy tất cả biến thể (màu sắc, RAM, ROM, giá bán) của một sản phẩm.
     * <p>
     * Frontend gọi endpoint này khi người dùng mở trang chi tiết sản phẩm
     * để render bộ chọn màu sắc và dung lượng.
     *
     * @param sanPhamId ID sản phẩm cha.
     * @return HTTP 200 kèm danh sách {@code BienTheSanPham}. HTTP 404 nếu sản phẩm không tồn tại.
     */
    @GetMapping("/bien-the/san-pham/{sanPhamId}")
    public ResponseEntity<?> layBienTheCuaSanPham(@PathVariable Integer sanPhamId) {
        log.debug("[CatalogPublic] GET /bien-the/san-pham/{}", sanPhamId);
        try {
            return ResponseEntity.ok(bienTheSanPhamService.layTheoSanPhamId(sanPhamId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Lấy chi tiết một biến thể SKU theo ID.
     * <p>
     * Frontend gọi khi người dùng chọn 1 cấu hình cụ thể (ví dụ: iPhone 15 Pro Max Titan Đen 256GB)
     * để hiển thị giá và tình trạng hàng.
     *
     * @param id ID biến thể.
     * @return HTTP 200 kèm {@code BienTheSanPham}. HTTP 404 nếu không tìm thấy.
     */
    @GetMapping("/bien-the/{id}")
    public ResponseEntity<?> layChiTietBienThe(@PathVariable Integer id) {
        log.debug("[CatalogPublic] GET /bien-the/{}", id);
        try {
            return ResponseEntity.ok(bienTheSanPhamService.getBienTheSanPham(id));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // THÔNG SỐ KỸ THUẬT
    // =========================================================================

    /**
     * Lấy toàn bộ thông số kỹ thuật của một sản phẩm.
     * <p>
     * Frontend dùng để render bảng thông số kỹ thuật ở trang chi tiết sản phẩm
     * (màn hình, pin, camera, chip...).
     *
     * @param sanPhamId ID sản phẩm.
     * @return HTTP 200 kèm {@code List<ThongSoKyThuat>}. HTTP 404 nếu sản phẩm không tồn tại.
     */
    @GetMapping("/thong-so/{sanPhamId}")
    public ResponseEntity<?> layThongSoKyThuat(@PathVariable Integer sanPhamId) {
        log.debug("[CatalogPublic] GET /thong-so/{}", sanPhamId);
        try {
            return ResponseEntity.ok(thongSoKyThuatService.layTheoSanPham(sanPhamId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // DANH MỤC (dùng cho thanh lọc)
    // =========================================================================

    /**
     * Lấy danh sách danh mục đang kích hoạt — dùng cho thanh lọc / navigation menu.
     * <p>
     * Ví dụ: iPhone, Samsung Galaxy, Xiaomi, Oppo...
     *
     * @return HTTP 200 kèm {@code List<DanhMuc>} đang kích hoạt.
     */
    @GetMapping("/danh-muc")
    public ResponseEntity<?> layDanhMucKichHoat() {
        log.debug("[CatalogPublic] GET /danh-muc");
        return ResponseEntity.ok(danhMucService.layDanhSachKichHoat());
    }

    // =========================================================================
    // HÃNG SẢN XUẤT (dùng cho thanh lọc)
    // =========================================================================

    /**
     * Lấy danh sách tất cả hãng sản xuất — dùng cho bộ lọc theo thương hiệu.
     * <p>
     * Ví dụ: Apple, Samsung, Xiaomi...
     *
     * @return HTTP 200 kèm {@code List<HangSanXuat>}.
     */
    @GetMapping("/hang-san-xuat")
    public ResponseEntity<?> layDanhSachHangSanXuat() {
        log.debug("[CatalogPublic] GET /hang-san-xuat");
        return ResponseEntity.ok(hangSanXuatService.layTatCa());
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
