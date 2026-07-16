package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller giao diện Admin cho Module Khuyến Mãi.
 */
@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class KhuyenMaiUIController {

    private final IKhuyenMaiService khuyenMaiService;
    private final ISanPhamService   sanPhamService;

    // ══════════════════════════════════════════════════════════════════════
    // DANH SÁCH
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping
    public String index(Model model) {
        model.addAttribute("ctkmList", khuyenMaiService.layDanhSach());
        model.addAttribute("pageTitle", "Quản lý Khuyến Mãi");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/danh-sach";
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORM: THEO ĐƠN HÀNG
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/theo-don-hang")
    public String formTheoDonHang(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm") || id != null) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "theo_don_hang");
        model.addAttribute("pageTitle", id != null ? "Sửa KM Theo Đơn Hàng" : "Thêm KM Theo Đơn Hàng");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-theo-don-hang";
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORM: THEO SẢN PHẨM
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/theo-san-pham")
    public String formTheoSanPham(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm") || id != null) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "theo_san_pham");
        model.addAttribute("pageTitle", id != null ? "Sửa KM Theo Sản Phẩm" : "Thêm KM Theo Sản Phẩm");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-theo-san-pham";
    }

    // ══════════════════════════════════════════════════════════════════════
    // LƯU CHUNG (POST /save)
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/save")
    public String save(@ModelAttribute ChuongTrinhKhuyenMai ctkm, RedirectAttributes ra) {
        try {
            ChuongTrinhKhuyenMai saved = khuyenMaiService.luu(ctkm);
            ra.addFlashAttribute("successMessage", "Lưu chương trình khuyến mãi thành công!");
            return "redirect:/admin/khuyen-mai/chi-tiet/" + loaiToUrl(saved.getLoai()) + "/" + saved.getId();
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("ctkm", ctkm);
            // Redirect về đúng form theo loai
            String formUrl = "/admin/khuyen-mai/form/" + loaiToUrl(ctkm.getLoai());
            if (ctkm.getId() != null) formUrl += "?id=" + ctkm.getId();
            return "redirect:" + formUrl;
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHI TIẾT: THEO ĐƠN HÀNG
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/theo-don-hang/{id}")
    public String chiTietTheoDonHang(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/theo-don-hang?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-theo-don-hang";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHI TIẾT: THEO SẢN PHẨM
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/theo-san-pham/{id}")
    public String chiTietTheoSanPham(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("phamViList", khuyenMaiService.layPhamVi(id));
        model.addAttribute("danhSachSanPham",
                sanPhamService.layDanhSach(null, null, PageRequest.of(0, 500, Sort.by("tenSanPham"))).getContent());
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/theo-san-pham?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-theo-san-pham";
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUB-FORM: THÊM SẢN PHẨM VÀO PHẠM VI ÁP DỤNG
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/pham-vi/them")
    public String themPhamVi(@PathVariable Integer id,
                             @RequestParam Integer sanPhamId,
                             RedirectAttributes ra) {
        try {
            khuyenMaiService.themPhamVi(id, sanPhamId);
            ra.addFlashAttribute("successMessage", "Đã thêm sản phẩm vào phạm vi áp dụng.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/chi-tiet/theo-san-pham/" + id;
    }

    @GetMapping("/{id}/pham-vi/xoa/{pvId}")
    public String xoaPhamVi(@PathVariable Integer id, @PathVariable Integer pvId,
                            RedirectAttributes ra) {
        try {
            khuyenMaiService.xoaPhamVi(pvId);
            ra.addFlashAttribute("successMessage", "Đã xóa sản phẩm khỏi phạm vi áp dụng.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/chi-tiet/theo-san-pham/" + id;
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOGGLE TRẠNG THÁI (Tạm dừng / Mở lại)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/toggle-trang-thai/{id}")
    public String toggleTrangThai(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            khuyenMaiService.toggleTrangThai(id);
            ra.addFlashAttribute("successMessage", "Đã cập nhật trạng thái chương trình khuyến mãi.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai";
    }

    // ══════════════════════════════════════════════════════════════════════
    // HELPER
    // ══════════════════════════════════════════════════════════════════════

    private String loaiToUrl(String loai) {
        if (loai == null) return "theo-san-pham";
        return switch (loai) {
            case "theo_don_hang" -> "theo-don-hang";
            default              -> "theo-san-pham";
        };
    }
}
