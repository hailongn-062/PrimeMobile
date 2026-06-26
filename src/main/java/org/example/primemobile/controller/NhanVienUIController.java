package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.service.INhanVienService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Slf4j
@Controller
@RequestMapping("/admin/nhan-vien")
@RequiredArgsConstructor
public class NhanVienUIController {

    private static final String SESSION_KEY = "CURRENT_ADMIN";

    private final INhanVienService nhanVienService;

    @GetMapping
    public String danhSach(Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        List<NguoiDung> danhSach = nhanVienService.layDanhSachNhanVien();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle", "Quản lý Nhân viên");
        model.addAttribute("activePage", "nhan-vien");
        model.addAttribute("danhSachNhanVien", danhSach);

        return "admin/nhan-vien/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        if (!model.containsAttribute("nguoiDung")) {
            NguoiDung nguoiDung;
            if (id != null) {
                nguoiDung = nhanVienService.layTheoId(id);
            } else {
                nguoiDung = new NguoiDung();
            }
            model.addAttribute("nguoiDung", nguoiDung);
        }
        
        model.addAttribute("pageTitle", id != null ? "Sửa Nhân Viên" : "Thêm mới Nhân Viên");
        model.addAttribute("activePage", "nhan-vien");
        return "admin/nhan-vien/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("nguoiDung") NguoiDung nguoiDung, RedirectAttributes redirectAttributes) {
        try {
            if (nguoiDung.getId() != null) {
                nhanVienService.capNhat(nguoiDung.getId(), nguoiDung);
                redirectAttributes.addFlashAttribute("successMessage", "Cập nhật nhân viên thành công.");
            } else {
                nhanVienService.themMoi(nguoiDung);
                redirectAttributes.addFlashAttribute("successMessage", "Thêm mới nhân viên thành công.");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addFlashAttribute("nguoiDung", nguoiDung);
            return "redirect:/admin/nhan-vien/form" + (nguoiDung.getId() != null ? "?id=" + nguoiDung.getId() : "");
        }
        return "redirect:/admin/nhan-vien";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        nhanVienService.doiTrangThai(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đổi trạng thái tài khoản thành công.");
        return "redirect:/admin/nhan-vien";
    }
}
