package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.service.IHangSanXuatService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/hang-san-xuat")
@RequiredArgsConstructor
public class HangSanXuatUIController {

    private final IHangSanXuatService hangSanXuatService;

    @GetMapping
    public String index(Model model) {
        model.addAttribute("hangSanXuats", hangSanXuatService.layTatCa());
        model.addAttribute("pageTitle", "Quản lý Hãng Sản Xuất");
        model.addAttribute("activePage", "hang-san-xuat");
        return "admin/hang-san-xuat/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        HangSanXuat hangSanXuat;
        if (id != null) {
            hangSanXuat = hangSanXuatService.layTheoId(id);
        } else {
            hangSanXuat = new HangSanXuat();
        }
        model.addAttribute("hangSanXuat", hangSanXuat);
        model.addAttribute("pageTitle", id != null ? "Sửa Hãng Sản Xuất" : "Thêm mới Hãng Sản Xuất");
        model.addAttribute("activePage", "hang-san-xuat");
        return "admin/hang-san-xuat/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("hangSanXuat") HangSanXuat hangSanXuat) {
        if (hangSanXuat.getId() != null) {
            hangSanXuatService.capNhat(hangSanXuat.getId(), hangSanXuat);
        } else {
            hangSanXuatService.them(hangSanXuat);
        }
        return "redirect:/admin/hang-san-xuat";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            hangSanXuatService.xoa(id);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa hãng này vì đang có sản phẩm liên kết.");
        }
        return "redirect:/admin/hang-san-xuat";
    }
}
