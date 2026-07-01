package org.example.primemobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class GioHangUIController {

    @GetMapping("/gio-hang")
    public String gioHang(Model model) {
        model.addAttribute("pageTitle", "Giỏ hàng");
        return "gio-hang/index";
    }
}
