package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

/**
 * Controller giao diện Admin cho Module Khuyến Mãi.
 *
 * URL base: /admin/khuyen-mai
 *
 * Luồng chính:
 *  - Danh sách : GET  /
 *  - Form theo loại : GET  /form/phan-tram | /form/don-hang-toi-thieu | /form/giam-gia-truc-tiep | /form/flash-sale
 *  - Lưu chung : POST /save  → redirect đến chi tiết tương ứng
 *  - Chi tiết  : GET  /chi-tiet/phan-tram/{id} | /chi-tiet/don-hang-toi-thieu/{id}
 *                     /chi-tiet/giam-gia-truc-tiep/{id} | /chi-tiet/flash-sale/{id}
 *  - Sub-forms : POST + GET (xoa) cho Flash Sale & Phạm vi
 */
@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class KhuyenMaiUIController {

    private final IKhuyenMaiService khuyenMaiService;
    private final ISanPhamService   sanPhamService;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

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
    // FORM 1: PHẦN TRĂM
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/phan-tram")
    public String formPhanTram(@RequestParam(required = false) Integer id,
                               @ModelAttribute("ctkm") ChuongTrinhKhuyenMai ctkmFlash,
                               Model model) {
        ChuongTrinhKhuyenMai ctkm;
        if (!model.containsAttribute("ctkm") || id != null) {
            ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "phan_tram");
        model.addAttribute("pageTitle", id != null ? "Sửa KM Phần trăm" : "Thêm KM Phần trăm");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-phan-tram";
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORM 2: ĐƠN HÀNG TỐI THIỂU
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/don-hang-toi-thieu")
    public String formDonHang(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm") || id != null) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "don_hang_toi_thieu");
        model.addAttribute("pageTitle", id != null ? "Sửa KM Đơn hàng tối thiểu" : "Thêm KM Đơn hàng tối thiểu");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-don-hang";
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORM 3: GIẢM GIÁ TRỰC TIẾP (theo sản phẩm)
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/giam-gia-truc-tiep")
    public String formGiamGia(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm") || id != null) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "giam_gia_truc_tiep");
        model.addAttribute("pageTitle", id != null ? "Sửa KM Giảm giá trực tiếp" : "Thêm KM Giảm giá trực tiếp");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-giam-gia";
    }

    // ══════════════════════════════════════════════════════════════════════
    // FORM 4: FLASH SALE
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/form/flash-sale")
    public String formFlashSale(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm") || id != null) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("loaiCung", "flash_sale");
        model.addAttribute("pageTitle", id != null ? "Sửa Flash Sale" : "Thêm Flash Sale");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form-flash-sale";
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
    // CHI TIẾT 1: PHẦN TRĂM
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/phan-tram/{id}")
    public String chiTietPhanTram(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/phan-tram?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-phan-tram";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHI TIẾT 2: ĐƠN HÀNG TỐI THIỂU
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/don-hang-toi-thieu/{id}")
    public String chiTietDonHang(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/don-hang-toi-thieu?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-don-hang";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHI TIẾT 3: GIẢM GIÁ TRỰC TIẾP
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/giam-gia-truc-tiep/{id}")
    public String chiTietGiamGia(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("phamViList", khuyenMaiService.layPhamVi(id));
        model.addAttribute("danhSachSanPham",
                sanPhamService.layDanhSach(null, null, PageRequest.of(0, 500, Sort.by("tenSanPham"))).getContent());
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/giam-gia-truc-tiep?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-giam-gia";
    }

    // ══════════════════════════════════════════════════════════════════════
    // CHI TIẾT 4: FLASH SALE
    // ══════════════════════════════════════════════════════════════════════

    @GetMapping("/chi-tiet/flash-sale/{id}")
    public String chiTietFlashSale(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);
        model.addAttribute("chiTietList", khuyenMaiService.layChiTietFlashSale(id));

        // Lấy tất cả biến thể để Admin chọn thêm vào Flash Sale
        List<BienTheSanPham> tatCaBienThe = bienTheSanPhamRepository.findAll(
                Sort.by("sanPham.tenSanPham", "maSku"));
        model.addAttribute("danhSachBienThe", tatCaBienThe);
        model.addAttribute("formUrl", "/admin/khuyen-mai/form/flash-sale?id=" + id);
        model.addAttribute("pageTitle", "Chi tiết: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet-flash-sale";
    }

    // ══════════════════════════════════════════════════════════════════════
    // SUB-FORM: THÊM BIẾN THỂ VÀO FLASH SALE
    // ══════════════════════════════════════════════════════════════════════

    @PostMapping("/{id}/flash-sale/them")
    public String themFlashSale(@PathVariable Integer id,
                                @RequestParam Integer bienTheSanPhamId,
                                @RequestParam BigDecimal phanTramGiam,
                                @RequestParam Integer soLuongGioiHan,
                                RedirectAttributes ra) {
        try {
            khuyenMaiService.themChiTietFlashSale(id, bienTheSanPhamId, phanTramGiam, soLuongGioiHan);
            ra.addFlashAttribute("successMessage", "Đã thêm biến thể vào Flash Sale.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/chi-tiet/flash-sale/" + id;
    }

    @GetMapping("/{id}/flash-sale/xoa/{fsId}")
    public String xoaFlashSale(@PathVariable Integer id, @PathVariable Integer fsId,
                                RedirectAttributes ra) {
        try {
            khuyenMaiService.xoaChiTietFlashSale(fsId);
            ra.addFlashAttribute("successMessage", "Đã xóa biến thể khỏi Flash Sale.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/chi-tiet/flash-sale/" + id;
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
        return "redirect:/admin/khuyen-mai/chi-tiet/giam-gia-truc-tiep/" + id;
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
        return "redirect:/admin/khuyen-mai/chi-tiet/giam-gia-truc-tiep/" + id;
    }

    // ══════════════════════════════════════════════════════════════════════
    // TOGGLE TRẠNG THÁI (Tạm dừng / Mở lại)
    // ══════════════════════════════════════════════════════════════════════

    /**
     * Đổi trạng thái Tạm dừng ↔ Hoạt động của 1 chương trình khuyến mãi.
     * Redirect về danh sách sau khi thực hiện.
     */
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


    /** Chuyển giá trị cột 'loai' sang đoạn URL tương ứng. */
    private String loaiToUrl(String loai) {
        if (loai == null) return "phan-tram";
        return switch (loai) {
            case "don_hang_toi_thieu" -> "don-hang-toi-thieu";
            case "giam_gia_truc_tiep" -> "giam-gia-truc-tiep";
            case "flash_sale"         -> "flash-sale";
            default                   -> "phan-tram";
        };
    }
}
