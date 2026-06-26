package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.service.IKhachHangService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/khach-hang")
@RequiredArgsConstructor
public class KhachHangUIController {

    private final IKhachHangService khachHangService;

    @GetMapping
    public String index(@RequestParam(required = false) String tuKhoa, Model model) {
        model.addAttribute("khachHangs", khachHangService.timKiem(tuKhoa));
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("pageTitle", "Quản lý Khách hàng");
        model.addAttribute("activePage", "khach-hang");
        return "admin/khach-hang/danh-sach";
    }
}
