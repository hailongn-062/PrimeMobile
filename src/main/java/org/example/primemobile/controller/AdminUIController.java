package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
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
}
