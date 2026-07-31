package org.example.primemobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin/thong-ke")
public class ThongKeUIController {

    @GetMapping("/doanh-thu")
    public String doanhThuPage(org.springframework.ui.Model model) {
        model.addAttribute("activePage", "thong-ke-doanh-thu");
        return "admin/thong-ke/doanh-thu";
    }

    @GetMapping("/san-pham")
    public String sanPhamPage(org.springframework.ui.Model model) {
        model.addAttribute("activePage", "thong-ke-san-pham");
        return "admin/thong-ke/thong-ke-san-pham";
    }
}
