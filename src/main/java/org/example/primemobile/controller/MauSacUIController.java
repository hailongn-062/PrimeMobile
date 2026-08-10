package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.MauSac;
import org.example.primemobile.service.IMauSacService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/mau-sac")
@RequiredArgsConstructor
public class MauSacUIController {

    private final IMauSacService mauSacService;

    @GetMapping
    public String index(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tuKhoa,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<MauSac> mauSacPage = mauSacService.layDanhSachPhanTrang(tuKhoa, pageable);

        model.addAttribute("mauSacPage", mauSacPage);
        model.addAttribute("tuKhoa", tuKhoa);
        
        model.addAttribute("pageTitle", "Quản lý Màu Sắc");
        model.addAttribute("activePage", "mau-sac");
        
        return "admin/mau-sac/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        MauSac mauSac;
        if (id != null) {
            mauSac = mauSacService.layTheoId(id);
        } else {
            mauSac = new MauSac();
        }
        model.addAttribute("mauSac", mauSac);
        model.addAttribute("pageTitle", id != null ? "Sửa Màu Sắc" : "Thêm mới Màu Sắc");
        model.addAttribute("activePage", "mau-sac");
        return "admin/mau-sac/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("mauSac") MauSac mauSac, RedirectAttributes redirectAttributes) {
        try {
            if (mauSac.getId() != null) {
                mauSacService.capNhat(mauSac.getId(), mauSac);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật màu sắc thành công!");
            } else {
                mauSacService.them(mauSac);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm mới màu sắc thành công!");
            }
            return "redirect:/admin/mau-sac";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            if (mauSac.getId() != null) {
                return "redirect:/admin/mau-sac/form?id=" + mauSac.getId();
            }
            return "redirect:/admin/mau-sac/form";
        }
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<MauSac>> layDanhSachApi() {
        return ResponseEntity.ok(mauSacService.layTatCa());
    }
}
