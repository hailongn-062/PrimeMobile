package org.example.primemobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/danh-gia")
public class DanhGiaUIController {

    @GetMapping
    public String trangQuanLyDanhGia(Model model) {
        model.addAttribute("pageTitle", "Quản lý đánh giá");
        model.addAttribute("activePage", "danh-gia");
        return "admin/danh-gia/danh-sach";
    }
}
