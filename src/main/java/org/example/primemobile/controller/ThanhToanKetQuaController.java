package org.example.primemobile.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ThanhToanKetQuaController {

    @GetMapping("/thanh-toan-ket-qua")
    public String ketQua(
            @RequestParam String maDonHang,
            @RequestParam String status,
            @RequestParam(required = false) String tongTien,
            @RequestParam(required = false) String message,
            Model model) {

        model.addAttribute("maDonHang", maDonHang);
        model.addAttribute("status", status);
        model.addAttribute("tongTien", tongTien);
        model.addAttribute("message", message);

        return "thanh-toan-ket-qua";
    }
}