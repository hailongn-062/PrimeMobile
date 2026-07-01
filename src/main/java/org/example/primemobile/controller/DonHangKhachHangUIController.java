package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DonHangKhachHangUIController {

    @GetMapping({"/tra-cuu-don-hang", "/don-hang-cua-toi", "/don-hang"})
    public String donHangCuaToi(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Object customer = session == null ? null : session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(customer instanceof SessionKhachHang currentCustomer)) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("pageTitle", "Đơn hàng của tôi");
        model.addAttribute("currentCustomer", currentCustomer);
        return "don-hang/index";
    }
}
