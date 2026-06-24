package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.service.IQuanLyDonHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/**
 * Controller UI (Thymeleaf) cho phân hệ Quản lý Đơn Hàng.
 * Base path: {@code /admin/don-hang}
 *
 * <p>Tách biệt hoàn toàn với {@link QuanLyDonHangController} (REST API).
 * Controller này chỉ render HTML — mọi thao tác nghiệp vụ (xác nhận, hủy...)
 * được thực hiện qua JavaScript fetch() gọi đến REST API.
 */
@Slf4j
@Controller
@RequestMapping("/admin/don-hang")
@RequiredArgsConstructor
public class QuanLyDonHangUIController {

    private static final String SESSION_KEY     = "CURRENT_ADMIN";
    private static final int    PAGE_SIZE       = 15;

    private final IQuanLyDonHangService quanLyDonHangService;

    // =========================================================================
    // GET /admin/don-hang — Danh sách đơn hàng
    // =========================================================================

    @GetMapping
    public String danhSach(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maDonHang,
            @RequestParam(required = false) String soDienThoai,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        log.info("[DonHangUI] Danh sách — user={}, trangThai={}, page={}", 
                 currentUser != null ? currentUser.getEmail() : "?", trangThai, page);

        Page<DonHang> result = quanLyDonHangService.layDanhSachDonHang(
                trangThai, maDonHang, soDienThoai,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "ngayDat"))
        );

        model.addAttribute("currentUser",    currentUser);
        model.addAttribute("pageTitle",      "Quản lý Đơn hàng");
        model.addAttribute("activePage",     "don-hang");
        model.addAttribute("danhSachDonHang", result.getContent());
        model.addAttribute("tongSoTrang",    result.getTotalPages());
        model.addAttribute("trangHienTai",   result.getNumber());
        model.addAttribute("tongSoDonHang",  result.getTotalElements());

        // Giữ lại filter để hiển thị lại trên form
        model.addAttribute("filterTrangThai",   trangThai);
        model.addAttribute("filterMaDonHang",   maDonHang);
        model.addAttribute("filterSoDienThoai", soDienThoai);

        return "admin/don-hang/danh-sach";
    }

    // =========================================================================
    // GET /admin/don-hang/{id} — Chi tiết đơn hàng
    // =========================================================================

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Integer id, Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        log.info("[DonHangUI] Chi tiết — donHangId={}, user={}", id,
                 currentUser != null ? currentUser.getEmail() : "?");

        DonHang donHang = quanLyDonHangService.layChiTietDonHang(id);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle",   "Chi tiết Đơn hàng #" + donHang.getMaDonHang());
        model.addAttribute("activePage",  "don-hang");
        model.addAttribute("donHang",     donHang);

        return "admin/don-hang/chi-tiet";
    }
}
