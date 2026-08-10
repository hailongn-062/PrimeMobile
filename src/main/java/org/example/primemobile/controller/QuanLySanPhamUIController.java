package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.ThongSoKyThuat;
import org.example.primemobile.entity.HinhAnhSanPham;
import org.example.primemobile.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/san-pham")
@RequiredArgsConstructor
public class QuanLySanPhamUIController {

    private final ISanPhamService sanPhamService;
    private final IDanhMucService danhMucService;
    private final IHangSanXuatService hangSanXuatService;
    private final IBienTheSanPhamService bienTheSanPhamService;
    private final IThongSoKyThuatService thongSoKyThuatService;
    private final IHinhAnhSanPhamService hinhAnhSanPhamService;
    private final IFileStorageService fileStorageService;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("ngayTao").descending());
        Page<SanPham> sanPhamPage = sanPhamService.layDanhSach(tuKhoa, danhMucId, hangSanXuatId, pageable);

        model.addAttribute("sanPhamPage", sanPhamPage);
        model.addAttribute("danhMucs", danhMucService.layDanhSachKichHoat());
        model.addAttribute("hangSanXuats", hangSanXuatService.layTatCa());
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("selectedDanhMucId", danhMucId);
        model.addAttribute("selectedHangSanXuatId", hangSanXuatId);
        
        model.addAttribute("pageTitle", "Quản lý Sản Phẩm");
        model.addAttribute("activePage", "san-pham");
        return "admin/san-pham/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        SanPham sanPham;
        if (id != null) {
            sanPham = sanPhamService.layTheoId(id);
        } else {
            sanPham = new SanPham();
        }
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("danhMucs", danhMucService.layDanhSachKichHoat());
        model.addAttribute("hangSanXuats", hangSanXuatService.layTatCa());
        model.addAttribute("pageTitle", id != null ? "Sửa Sản Phẩm" : "Thêm mới Sản Phẩm");
        model.addAttribute("activePage", "san-pham");
        return "admin/san-pham/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("sanPham") SanPham sanPham, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        try {
            SanPham savedSanPham;
            if (sanPham.getId() != null) {
                savedSanPham = sanPhamService.capNhat(sanPham.getId(), sanPham);
            } else {
                savedSanPham = sanPhamService.them(sanPham);
            }
            return "redirect:/admin/san-pham/" + savedSanPham.getId() + "/chi-tiet";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("maSanPhamError", e.getMessage());
            if (sanPham.getId() != null) {
                return "redirect:/admin/san-pham/form?id=" + sanPham.getId();
            }
            return "redirect:/admin/san-pham/form";
        }
    }

    @GetMapping("/{id}/chi-tiet")
    public String chiTiet(@PathVariable("id") Integer id, Model model) {
        SanPham sanPham = sanPhamService.layTheoId(id);
        
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("bienTheSanPhams", bienTheSanPhamService.layTheoSanPhamId(id));
        model.addAttribute("thongSoKyThuats", thongSoKyThuatService.layTheoSanPham(id));
        
        // Khởi tạo object rỗng cho các form con thêm mới
        model.addAttribute("newThongSo", new ThongSoKyThuat());
        model.addAttribute("newBienThe", new BienTheSanPham());

        model.addAttribute("pageTitle", "Chi tiết: " + sanPham.getTenSanPham());
        model.addAttribute("activePage", "san-pham");
        return "admin/san-pham/chi-tiet";
    }

    @PostMapping("/thong-so/save")
    public String saveThongSo(@ModelAttribute("newThongSo") ThongSoKyThuat thongSo) {
        Integer sanPhamId = thongSo.getSanPham().getId();
        if (thongSo.getId() != null) {
            thongSoKyThuatService.sua(thongSo.getId(), thongSo);
        } else {
            thongSoKyThuatService.them(sanPhamId, thongSo);
        }
        return "redirect:/admin/san-pham/" + sanPhamId + "/chi-tiet";
    }

    @GetMapping("/thong-so/delete/{id}")
    public String deleteThongSo(@PathVariable("id") Integer id, @RequestParam("sanPhamId") Integer sanPhamId) {
        thongSoKyThuatService.xoa(id);
        return "redirect:/admin/san-pham/" + sanPhamId + "/chi-tiet";
    }

    @PostMapping("/bien-the/save")
    public String saveBienThe(@ModelAttribute("newBienThe") BienTheSanPham bienThe, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        Integer sanPhamId = bienThe.getSanPham().getId();
        try {
            if (bienThe.getId() != null) {
                bienTheSanPhamService.capNhat(bienThe.getId(), bienThe);
            } else {
                bienTheSanPhamService.them(sanPhamId, bienThe);
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorBienThe", "Lỗi lưu biến thể: " + e.getMessage());
        }
        return "redirect:/admin/san-pham/" + sanPhamId + "/chi-tiet";
    }

    public record BienTheDto(Integer sanPhamId, String maSku, String mauSac, Integer ramGb, Integer luuTruGb, java.math.BigDecimal giaBan) {}

    @PostMapping("/bien-the/save-multiple")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> saveMultipleBienThe(@RequestBody java.util.List<BienTheDto> dtoList) {
        try {
            for (BienTheDto dto : dtoList) {
                BienTheSanPham bt = new BienTheSanPham();
                bt.setMaSku(dto.maSku());
                bt.setMauSac(dto.mauSac());
                bt.setRamGb(dto.ramGb());
                bt.setLuuTruGb(dto.luuTruGb());
                bt.setGiaBan(dto.giaBan());
                bienTheSanPhamService.them(dto.sanPhamId(), bt);
            }
            return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("success", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/bien-the/{id}/hinh-anh")
    public String hinhAnhBienThe(@PathVariable("id") Integer id, Model model) {
        BienTheSanPham bienThe = bienTheSanPhamService.getBienTheSanPham(id);
        
        model.addAttribute("bienThe", bienThe);
        model.addAttribute("hinhAnhs", hinhAnhSanPhamService.layTheoBienThe(id));
        model.addAttribute("newHinhAnh", new HinhAnhSanPham());

        model.addAttribute("pageTitle", "Quản lý Ảnh: " + bienThe.getMaSku());
        model.addAttribute("activePage", "san-pham");
        return "admin/san-pham/hinh-anh";
    }

    @PostMapping("/bien-the/hinh-anh/save")
    public String saveHinhAnh(@RequestParam("bienTheSanPham.id") Integer bienTheId, 
                              @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        // Lưu file và lấy đường dẫn
        String fileUrl = fileStorageService.storeFile(file);

        // Tạo đối tượng HinhAnhSanPham
        HinhAnhSanPham hinhAnh = new HinhAnhSanPham();
        hinhAnh.setDuongDan(fileUrl);
        hinhAnh.setLaAnhChinh(false); // Mặc định là false

        // Lưu vào cơ sở dữ liệu
        hinhAnhSanPhamService.themAnh(bienTheId, hinhAnh);
        return "redirect:/admin/san-pham/bien-the/" + bienTheId + "/hinh-anh";
    }

    @GetMapping("/bien-the/hinh-anh/delete/{id}")
    public String deleteHinhAnh(@PathVariable("id") Integer id, @RequestParam("bienTheId") Integer bienTheId) {
        hinhAnhSanPhamService.xoaAnh(id);
        return "redirect:/admin/san-pham/bien-the/" + bienTheId + "/hinh-anh";
    }

    @PostMapping("/bien-the/hinh-anh/reorder")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> reorderHinhAnh(@RequestBody java.util.List<Integer> ids) {
        try {
            hinhAnhSanPhamService.sapXepThuTu(ids);
            return org.springframework.http.ResponseEntity.ok().body(java.util.Map.of("success", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/api/bien-the/{id}/hinh-anh")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> getHinhAnhs(@PathVariable("id") Integer id) {
        java.util.List<HinhAnhSanPham> list = hinhAnhSanPhamService.layTheoBienThe(id);
        java.util.List<java.util.Map<String, Object>> result = new java.util.ArrayList<>();
        for (HinhAnhSanPham img : list) {
            result.add(java.util.Map.of(
                "id", img.getId(),
                "duongDan", img.getDuongDan(),
                "laAnhChinh", img.getLaAnhChinh()
            ));
        }
        return org.springframework.http.ResponseEntity.ok(result);
    }

    @PostMapping("/api/bien-the/hinh-anh/upload")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> uploadHinhAnh(@RequestParam("bienTheId") Integer bienTheId, 
                                                                    @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        try {
            String fileUrl = fileStorageService.storeFile(file);
            HinhAnhSanPham hinhAnh = new HinhAnhSanPham();
            hinhAnh.setDuongDan(fileUrl);
            hinhAnh.setLaAnhChinh(false);
            hinhAnhSanPhamService.themAnh(bienTheId, hinhAnh);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/api/bien-the/hinh-anh/{id}")
    @ResponseBody
    public org.springframework.http.ResponseEntity<?> deleteHinhAnhAjax(@PathVariable("id") Integer id) {
        try {
            hinhAnhSanPhamService.xoaAnh(id);
            return org.springframework.http.ResponseEntity.ok(java.util.Map.of("success", true));
        } catch (Exception e) {
            return org.springframework.http.ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", e.getMessage()));
        }
    }
}
