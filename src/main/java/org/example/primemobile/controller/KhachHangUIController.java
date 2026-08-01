package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.KhachHangRowDto;
import org.example.primemobile.service.IKhachHangService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Controller giao diện Admin cho Module Khách hàng.
 */
@Slf4j
@Controller
@RequestMapping("/admin/khach-hang")
@RequiredArgsConstructor
public class KhachHangUIController {

    private final IKhachHangService khachHangService;

    /**
     * Danh sách khách hàng kèm thống kê — hỗ trợ tìm kiếm và lọc trạng thái.
     */
    @GetMapping
    public String index(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String trangThai,
            Model model) {

        String finalTuKhoa    = (tuKhoa    != null && !tuKhoa.isBlank())    ? tuKhoa.trim()    : null;
        String finalTrangThai = (trangThai != null && !trangThai.isBlank()) ? trangThai.trim() : null;

        List<KhachHangRowDto> danhSach = khachHangService.timKiemVoiThongKe(finalTuKhoa, finalTrangThai);

        model.addAttribute("danhSachKhachHang", danhSach);
        model.addAttribute("tuKhoa",    tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("pageTitle", "Quản lý Khách hàng");
        model.addAttribute("activePage", "khach-hang");
        return "admin/khach-hang/danh-sach";
    }

    /**
     * Khóa / Mở tài khoản khách hàng.
     * Chỉ áp dụng với khách đã có tài khoản (nguoiDungId != null).
     */
    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            khachHangService.doiTrangThaiTaiKhoan(id);
            ra.addFlashAttribute("successMessage", "Đổi trạng thái tài khoản khách hàng thành công.");
        } catch (IllegalStateException e) {
            ra.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/khach-hang";
    }
}
