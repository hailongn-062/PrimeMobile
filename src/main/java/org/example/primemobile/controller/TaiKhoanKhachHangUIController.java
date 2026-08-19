package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.repository.DiaChiKhachHangRepository;
import org.example.primemobile.repository.KhachHangRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class TaiKhoanKhachHangUIController {

    private final DiaChiKhachHangRepository diaChiKhachHangRepository;
    private final org.example.primemobile.service.IYeuThichService yeuThichService;
    private final KhachHangRepository khachHangRepository;
    private final org.example.primemobile.service.IKhuyenMaiService khuyenMaiService;

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
        
        var danhSachYeuThich = yeuThichService.layDanhSach(currentCustomer.getKhachHangId());
        model.addAttribute("danhSachYeuThich", danhSachYeuThich);

        java.util.Map<Integer, java.math.BigDecimal> giaSauKhuyenMaiTheoSanPham = new java.util.HashMap<>();
        for (var yt : danhSachYeuThich) {
            var sp = yt.getSanPham();
            if (sp != null && sp.getBienTheSanPhams() != null && !sp.getBienTheSanPhams().isEmpty()) {
                var firstBt = sp.getBienTheSanPhams().get(0);
                java.math.BigDecimal giaSauKM = khuyenMaiService.tinhGiaSauKhuyenMai(firstBt.getId(), null);
                giaSauKhuyenMaiTheoSanPham.put(sp.getId(), giaSauKM != null ? giaSauKM : firstBt.getGiaBan());
            }
        }
        model.addAttribute("giaSauKhuyenMaiTheoSanPham", giaSauKhuyenMaiTheoSanPham);

        return "tai-khoan/yeu-thich";
    }

    @PostMapping("/toi/cap-nhat")
    public String capNhatThongTin(HttpServletRequest request,
                                  @RequestParam("hoTen") String hoTen,
                                  RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        Object customer = session == null ? null : session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(customer instanceof SessionKhachHang currentCustomer)) {
            return "redirect:/dang-nhap";
        }

        if (hoTen == null || hoTen.trim().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Họ tên không được để trống.");
            return "redirect:/toi";
        }

        KhachHang khachHang = khachHangRepository.findById(currentCustomer.getKhachHangId()).orElse(null);
        if (khachHang != null) {
            khachHang.setHoTen(hoTen.trim());
            khachHang.setUpdatedAt(LocalDateTime.now());
            khachHangRepository.save(khachHang);

            // Cập nhật lại session
            currentCustomer.setHoTen(hoTen.trim());
            session.setAttribute(SessionKhachHang.SESSION_KEY, currentCustomer);

            redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thông tin khách hàng.");
        }

        return "redirect:/toi";
    }
}
