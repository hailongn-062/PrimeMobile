package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.repository.DiaChiKhachHangRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class TaiKhoanKhachHangUIController {

    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final org.example.primemobile.service.IYeuThichService yeuThichService;

    @GetMapping({"/toi", "/dia-chi"})
    public String taiKhoan(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Object customer = session == null ? null : session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(customer instanceof SessionKhachHang currentCustomer)) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("pageTitle", "Tài khoản của tôi");
        model.addAttribute("currentCustomer", currentCustomer);
        model.addAttribute("diaChis", diaChiKhachHangRepository
                .findByKhachHangIdOrderByMacDinhDesc(currentCustomer.getKhachHangId()));
        return "tai-khoan/index";
    }

    @GetMapping("/yeu-thich")
    public String yeuThich(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Object customer = session == null ? null : session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(customer instanceof SessionKhachHang currentCustomer)) {
            return "redirect:/dang-nhap";
        }

        model.addAttribute("pageTitle", "Sản phẩm yêu thích");
        model.addAttribute("currentCustomer", currentCustomer);
        model.addAttribute("danhSachYeuThich", yeuThichService.layDanhSach(currentCustomer.getKhachHangId()));
        return "tai-khoan/yeu-thich";
    }
}
