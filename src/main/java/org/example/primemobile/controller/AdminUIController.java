package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller UI cho khu vực Quản trị (Admin Dashboard).
 * <p>
 * Mọi endpoint đều được bảo vệ bởi {@code AuthInterceptor} — chặn request
 * không có session hợp lệ tại {@code /api/admin/**} và {@code /admin/**}.
 *
 * <p>Controller này KHÔNG tự kiểm tra session — đó là trách nhiệm của Interceptor.
 * Chỉ cần lấy {@code SessionUser} để hiển thị thông tin người dùng trên UI.
 */
@Slf4j
@Controller
@RequestMapping("/admin")
public class AdminUIController {

    private static final String SESSION_KEY = "CURRENT_ADMIN";

    /**
     * Trang tổng quan Admin Dashboard.
     *
     * @param model   Spring Model.
     * @param session HttpSession để lấy thông tin người dùng hiển thị trên sidebar.
     * @return View "admin/dashboard".
     */
    @GetMapping({"/dashboard", ""})
    public String dashboard(Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle",   "Tổng quan");
        model.addAttribute("activePage",  "dashboard");
        log.info("[AdminUI] Dashboard — user={}", currentUser != null ? currentUser.getEmail() : "unknown");
        return "admin/dashboard";
    }

    /**
     * Trang báo lỗi 403 — Không có quyền truy cập.
     * <p>
     * Được redirect đến từ {@code AuthInterceptor} khi NhanVien cố truy cập
     * các module độc quyền của Admin (ví dụ: Quản lý Nhân Viên).
     *
     * @param model   Spring Model.
     * @param session HttpSession để lấy thông tin người dùng.
     * @return View "admin/error-403".
     */
    @GetMapping("/error-403")
    public String error403(Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle",   "Không có quyền truy cập");
        model.addAttribute("activePage",  "");
        log.warn("[AdminUI] 403 Error Page — user={}",
                currentUser != null ? currentUser.getEmail() : "unknown");
        return "admin/error-403";
    }

    /**
     * Trang Quản lý Sản phẩm
     */
//    @GetMapping("/san-pham")
//    public String sanPham(Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Quản lý Sản phẩm");
//        model.addAttribute("activePage",  "san-pham");
//        return "admin/san-pham/index";
//    }
//
//    /**
//     * Trang Quản lý Biến thể (SKU)
//     */
//    @GetMapping("/bien-the-san-pham")
//    public String bienTheSanPham(Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Quản lý Biến thể Sản phẩm");
//        model.addAttribute("activePage",  "san-pham");
//        return "admin/san-pham/bien-the";
//    }
//
//    /**
//     * Quản lý biến thể của một sản phẩm cụ thể
//     */
//    @GetMapping("/san-pham/{id}/bien-the")
//    public String chiTietBienThe(@PathVariable Integer id, Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Quản lý Biến thể");
//        model.addAttribute("activePage",  "san-pham");
//        model.addAttribute("sanPhamId", id);
//        return "admin/san-pham/bien-the-chi-tiet";
//    }
//
//    @GetMapping("/thong-so-ky-thuat")
//    public String thongSoKyThuat(Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Thông số kỹ thuật");
//        model.addAttribute("activePage",  "san-pham");
//        return "admin/san-pham/thong-so";
//    }

//    @GetMapping("/hang-san-xuat")
//    public String hangSanXuat(Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Hãng sản xuất");
//        model.addAttribute("activePage",  "san-pham");
//        return "admin/san-pham/hang-san-xuat";
//    }

//    @GetMapping("/danh-muc")
//    public String danhMuc(Model model, HttpSession session) {
//        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
//        model.addAttribute("currentUser", currentUser);
//        model.addAttribute("pageTitle",   "Danh mục Sản phẩm");
//        model.addAttribute("activePage",  "san-pham");
//        return "admin/san-pham/danh-muc";
//    }
}
