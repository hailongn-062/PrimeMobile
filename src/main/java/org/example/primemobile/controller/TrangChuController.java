package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller cho Trang Chủ (Customer Site) của PrimeMobile.
 * <p>
 * Xử lý các request đến trang chủ ({@code GET /} và {@code GET /trang-chu})
 * và trả về view Thymeleaf {@code index.html}.
 *
 * <h3>Dữ liệu đẩy vào Model:</h3>
 * <ul>
 *   <li>{@code danhSachDanhMuc} — Toàn bộ danh mục đang kích hoạt (sidebar + nav pills).</li>
 *   <li>{@code sanPhamNoiBat}   — Top 8 sản phẩm đang bán (grid nổi bật).</li>
 *   <li>{@code pageTitle}       — Tiêu đề tab trình duyệt.</li>
 * </ul>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class TrangChuController {

    private final IDanhMucService  danhMucService;
    private final ISanPhamService  sanPhamService;

    /** Số sản phẩm hiển thị trong section "Nổi bật". */
    private static final int SO_LUONG_NOI_BAT = 8;

    /**
     * Xử lý request trang chủ — {@code GET /} và {@code GET /trang-chu}.
     *
     * @param model Spring Model để đẩy dữ liệu sang Thymeleaf template.
     * @return Tên view Thymeleaf: {@code "index"} → render {@code templates/index.html}.
     */
    @GetMapping({"/", "/trang-chu"})
    public String trangChu(Model model) {
        log.info("[TrangChu] GET /");

        // ── 1. Danh mục kích hoạt (dùng cho category pills) ──────
        model.addAttribute("danhSachDanhMuc", danhMucService.layDanhSachKichHoat());

        // ── 2. Top 8 sản phẩm nổi bật đang bán ──────────────────
        Page<SanPham> trangSanPham = sanPhamService.layDanhSach(
                null, // danhMucId = null → tất cả danh mục
                null, // hangSanXuatId = null → tất cả hãng
                PageRequest.of(0, SO_LUONG_NOI_BAT, Sort.by(Sort.Direction.DESC, "id"))
        );
        model.addAttribute("sanPhamNoiBat", trangSanPham.getContent());

        // ── 3. Meta ───────────────────────────────────────────────
        model.addAttribute("pageTitle", "Trang Chủ");
        model.addAttribute("pageDescription",
                "PrimeMobile — Mua điện thoại chính hãng, giá tốt nhất, giao hàng nhanh toàn quốc.");

        return "index";
    }
}
