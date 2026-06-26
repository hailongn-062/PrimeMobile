package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.IHangSanXuatService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/admin/khuyen-mai")
@RequiredArgsConstructor
public class KhuyenMaiUIController {

    private final IKhuyenMaiService khuyenMaiService;
    private final ISanPhamService sanPhamService;
    private final IDanhMucService danhMucService;
    private final IHangSanXuatService hangSanXuatService;

    // ─── Danh sách ────────────────────────────────────────────────────────
    @GetMapping
    public String index(Model model) {
        model.addAttribute("ctkmList", khuyenMaiService.layDanhSach());
        model.addAttribute("pageTitle", "Quản lý Khuyến Mãi");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/danh-sach";
    }

    // ─── Form Tạo / Sửa ──────────────────────────────────────────────────
    @GetMapping("/form")
    public String form(@RequestParam(required = false) Integer id, Model model) {
        if (!model.containsAttribute("ctkm")) {
            ChuongTrinhKhuyenMai ctkm = (id != null) ? khuyenMaiService.layTheoId(id) : new ChuongTrinhKhuyenMai();
            model.addAttribute("ctkm", ctkm);
        }
        model.addAttribute("pageTitle", id != null ? "Sửa Chương Trình KM" : "Thêm Chương Trình KM");
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("ctkm") ChuongTrinhKhuyenMai ctkm, RedirectAttributes ra) {
        try {
            ChuongTrinhKhuyenMai saved;
            if (ctkm.getId() != null) {
                saved = khuyenMaiService.capNhat(ctkm.getId(), ctkm);
            } else {
                saved = khuyenMaiService.taoMoi(ctkm);
            }
            return "redirect:/admin/khuyen-mai/" + saved.getId() + "/chi-tiet";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
            ra.addFlashAttribute("ctkm", ctkm);
            return "redirect:/admin/khuyen-mai/form" + (ctkm.getId() != null ? "?id=" + ctkm.getId() : "");
        }
    }

    // ─── Chi tiết Master-Detail ───────────────────────────────────────────
    @GetMapping("/{id}/chi-tiet")
    public String chiTiet(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai ctkm = khuyenMaiService.layTheoId(id);
        model.addAttribute("ctkm", ctkm);

        String loai = ctkm.getLoai();
        if ("ma_code".equals(loai)) {
            model.addAttribute("maGiamGias", khuyenMaiService.layMaGiamGia(id));
        } else if ("flash_sale".equals(loai)) {
            model.addAttribute("chiTietFlashSales", khuyenMaiService.layChiTietFlashSale(id));
        } else {
            // giam_gia_truc_tiep / phan_tram / don_hang_toi_thieu
            model.addAttribute("phamViKhuyenMais", khuyenMaiService.layPhamVi(id));
            model.addAttribute("danhMucs", danhMucService.layDanhSachKichHoat());
            model.addAttribute("hangSanXuats", hangSanXuatService.layTatCa());
            model.addAttribute("danhSachSanPham", sanPhamService.layDanhSach(null, null,
                    org.springframework.data.domain.PageRequest.of(0, 500)));
        }

        model.addAttribute("pageTitle", "Chi tiết KM: " + ctkm.getTenCtkm());
        model.addAttribute("activePage", "khuyen-mai");
        return "admin/khuyen-mai/chi-tiet";
    }

    // ─── Sub-form: Mã giảm giá ────────────────────────────────────────────
    @PostMapping("/{id}/ma-giam-gia/sinh-ma")
    public String sinhMa(@PathVariable Integer id,
                         @RequestParam(defaultValue = "10") int soLuong,
                         @RequestParam(defaultValue = "CODE") String prefix,
                         RedirectAttributes ra) {
        try {
            khuyenMaiService.sinhMaGiamGia(id, soLuong, prefix);
            ra.addFlashAttribute("successMessage", "Sinh " + soLuong + " mã thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi sinh mã: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/" + id + "/chi-tiet";
    }

    // ─── Sub-form: Flash Sale items ───────────────────────────────────────
    @PostMapping("/{id}/flash-sale/them")
    public String themFlashSale(@PathVariable Integer id,
                                @RequestParam Integer bienTheSanPhamId,
                                @RequestParam BigDecimal giaFlash,
                                @RequestParam Integer soLuongGioiHan,
                                RedirectAttributes ra) {
        try {
            khuyenMaiService.themChiTietFlashSale(id, bienTheSanPhamId, giaFlash, soLuongGioiHan);
            ra.addFlashAttribute("successMessage", "Thêm biến thể Flash Sale thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/" + id + "/chi-tiet";
    }

    @GetMapping("/{id}/flash-sale/xoa/{fsId}")
    public String xoaFlashSale(@PathVariable Integer id, @PathVariable Integer fsId, RedirectAttributes ra) {
        khuyenMaiService.xoaChiTietFlashSale(fsId);
        ra.addFlashAttribute("successMessage", "Đã xóa biến thể khỏi Flash Sale.");
        return "redirect:/admin/khuyen-mai/" + id + "/chi-tiet";
    }

    // ─── Sub-form: Phạm vi ────────────────────────────────────────────────
    @PostMapping("/{id}/pham-vi/them")
    public String themPhamVi(@PathVariable Integer id,
                             @RequestParam(required = false) Integer sanPhamId,
                             @RequestParam(required = false) Integer danhMucId,
                             @RequestParam(required = false) Integer hangSanXuatId,
                             RedirectAttributes ra) {
        try {
            khuyenMaiService.themPhamVi(id, sanPhamId, danhMucId, hangSanXuatId);
            ra.addFlashAttribute("successMessage", "Thêm phạm vi áp dụng thành công.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/khuyen-mai/" + id + "/chi-tiet";
    }

    @GetMapping("/{id}/pham-vi/xoa/{pvId}")
    public String xoaPhamVi(@PathVariable Integer id, @PathVariable Integer pvId, RedirectAttributes ra) {
        khuyenMaiService.xoaPhamVi(pvId);
        ra.addFlashAttribute("successMessage", "Đã xóa phạm vi áp dụng.");
        return "redirect:/admin/khuyen-mai/" + id + "/chi-tiet";
    }
}
