package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.DangNhapAdminRequest;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.service.IAuthService;
import org.example.primemobile.service.IKhachHangAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthUIController {

    private static final String SESSION_KEY = "CURRENT_ADMIN";

    private final IAuthService authService;
    private final IKhachHangAuthService khachHangAuthService;

    @GetMapping("/dang-nhap")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            Model model,
            HttpServletRequest request) {

        HttpSession existingSession = request.getSession(false);
        if (existingSession != null && existingSession.getAttribute(SESSION_KEY) instanceof SessionUser) {
            log.info("[AuthUI] Admin already logged in, redirect to dashboard");
            return "redirect:/admin/dashboard";
        }

        if (existingSession != null
                && existingSession.getAttribute(SessionKhachHang.SESSION_KEY) instanceof SessionKhachHang) {
            log.info("[AuthUI] Customer already logged in, redirect to home");
            return "redirect:/";
        }

        if ("true".equals(error)) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.");
        }

        model.addAttribute("pageTitle", "Đăng nhập");
        return "login";
    }

    @PostMapping("/dang-nhap")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String matKhau,
            HttpServletRequest request) {

        log.info("[AuthUI] POST /dang-nhap identifier={}", email);

        try {
            try {
                SessionKhachHang sessionKhachHang = khachHangAuthService.dangNhap(email, matKhau);
                resetSession(request).setAttribute(SessionKhachHang.SESSION_KEY, sessionKhachHang);
                request.getSession(false).setMaxInactiveInterval(8 * 3600);

                log.info("[AuthUI] Customer login success id={}", sessionKhachHang.getKhachHangId());
                return "redirect:/";
            } catch (IllegalArgumentException e) {
                log.info("[AuthUI] Customer login failed, trying admin flow");
            }

            DangNhapAdminRequest loginRequest = new DangNhapAdminRequest();
            loginRequest.setEmail(email.trim());
            loginRequest.setMatKhau(matKhau);

            SessionUser sessionUser = authService.dangNhapAdmin(loginRequest);
            resetSession(request).setAttribute(SESSION_KEY, sessionUser);
            request.getSession(false).setMaxInactiveInterval(8 * 3600);

            log.info("[AuthUI] Admin login success id={}, role={}", sessionUser.getId(), sessionUser.getRole());
            return "redirect:/admin/dashboard";
        } catch (IllegalArgumentException e) {
            log.warn("[AuthUI] Login failed identifier={}, reason={}", email, e.getMessage());
            return "redirect:/dang-nhap?error=true";
        } catch (Exception e) {
            log.error("[AuthUI] System error while logging in identifier={}", email, e);
            return "redirect:/dang-nhap?error=true";
        }
    }

    @GetMapping({"/dang-ky", "/register"})
    public String showRegisterPage(
            @RequestParam(required = false) String error,
            Model model,
            HttpServletRequest request) {

        HttpSession existingSession = request.getSession(false);
        // Kiểm tra Admin
        if (existingSession != null && existingSession.getAttribute(SESSION_KEY) instanceof SessionUser) {
            log.info("[AuthUI] Admin already logged in, redirect to dashboard from register page");
            return "redirect:/admin/dashboard";
        }
        // Kiểm tra Khách hàng
        if (existingSession != null
                && existingSession.getAttribute(SessionKhachHang.SESSION_KEY) instanceof SessionKhachHang) {
            log.info("[AuthUI] Customer already logged in, redirect to home from register page");
            return "redirect:/";
        }

        if ("true".equals(error)) {
            model.addAttribute("errorMessage", "Thông tin đăng ký không hợp lệ hoặc đã tồn tại.");
        }

        model.addAttribute("pageTitle", "Đăng ký");
        return "register";
    }

    @PostMapping("/dang-ky")
    public String processRegister(
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam String email,
            @RequestParam String matKhau,
            HttpServletRequest request) {

        log.info("[AuthUI] POST /dang-ky email={}", email);

        try {
            SessionKhachHang sessionKhachHang = khachHangAuthService.dangKy(hoTen, email, soDienThoai, matKhau);
            resetSession(request).setAttribute(SessionKhachHang.SESSION_KEY, sessionKhachHang);
            request.getSession(false).setMaxInactiveInterval(8 * 3600);

            log.info("[AuthUI] Customer register success id={}", sessionKhachHang.getKhachHangId());
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            log.warn("[AuthUI] Register failed email={}, reason={}", email, e.getMessage());
            return "redirect:/dang-ky?error=true";
        } catch (Exception e) {
            log.error("[AuthUI] System error while registering email={}", email, e);
            return "redirect:/dang-ky?error=true";
        }
    }

    @GetMapping("/dang-xuat")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object user = session.getAttribute(SESSION_KEY);
            if (user instanceof SessionUser sessionUser) {
                log.info("[AuthUI] Logout admin id={}, email={}", sessionUser.getId(), sessionUser.getEmail());
            }
            session.invalidate();
        }
        return "redirect:/dang-nhap";
    }

    private HttpSession resetSession(HttpServletRequest request) {
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            oldSession.invalidate();
        }
        return request.getSession(true);
    }
}
