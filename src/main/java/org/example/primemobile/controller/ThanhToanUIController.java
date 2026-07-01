package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.DiaChiKhachHang;
import org.example.primemobile.entity.PhuongThucThanhToan;
import org.example.primemobile.repository.DiaChiKhachHangRepository;
import org.example.primemobile.repository.PhuongThucThanhToanRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class ThanhToanUIController {

    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @GetMapping("/thanh-toan")
    public String thanhToan(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Object customer = session == null ? null : session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(customer instanceof SessionKhachHang currentCustomer)) {
            return "redirect:/dang-nhap";
        }

        List<DiaChiKhachHang> diaChis = diaChiKhachHangRepository
                .findByKhachHangIdOrderByMacDinhDesc(currentCustomer.getKhachHangId());
        List<PhuongThucThanhToan> phuongThucs = phuongThucThanhToanRepository.findAll().stream()
                .filter(pt -> Boolean.TRUE.equals(pt.getKichHoat()))
                .sorted(Comparator
                        .comparing((PhuongThucThanhToan pt) -> !"Tien mat".equalsIgnoreCase(pt.getTenPttt()))
                        .thenComparing(PhuongThucThanhToan::getId))
                .toList();

        model.addAttribute("pageTitle", "Thanh toán");
        model.addAttribute("currentCustomer", currentCustomer);
        model.addAttribute("diaChis", diaChis);
        model.addAttribute("phuongThucs", phuongThucs);
        return "thanh-toan/index";
    }
}
