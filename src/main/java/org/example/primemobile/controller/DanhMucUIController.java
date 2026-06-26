package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.service.IDanhMucService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/danh-muc")
@RequiredArgsConstructor
public class DanhMucUIController {

    private final IDanhMucService danhMucService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("danhMucs", danhMucService.layTatCa());
        model.addAttribute("pageTitle", "Quản lý Danh mục");
        model.addAttribute("activePage", "danh-muc");
        return "admin/danh-muc/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        DanhMuc danhMuc;
        if (id != null) {
            danhMuc = danhMucService.layTheoId(id);
        } else {
            danhMuc = new DanhMuc();
            danhMuc.setKichHoat(true); // Default true for new
        }
        model.addAttribute("danhMuc", danhMuc);
        model.addAttribute("pageTitle", id != null ? "Sửa Danh mục" : "Thêm mới Danh mục");
        model.addAttribute("activePage", "danh-muc");
        return "admin/danh-muc/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("danhMuc") DanhMuc danhMuc) {
        if (danhMuc.getId() != null) {
            danhMucService.capNhat(danhMuc.getId(), danhMuc);
        } else {
            danhMucService.them(danhMuc);
        }
        return "redirect:/admin/danh-muc";
    }
}
