package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.DangNhapAdminRequest;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.service.IAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Controller UI cho luồng Đăng nhập / Đăng xuất của Admin & NhanVien.
 * <p>
 * Tuân thủ system_rules.md §1: Dùng {@code HttpSession}, KHÔNG Spring Security.
 *
 * <h3>Luồng đăng nhập:</h3>
 * <ol>
 *   <li>GET /dang-nhap         → Hiển thị trang login.html</li>
 *   <li>POST /dang-nhap        → Gọi IAuthService.dangNhapAdmin()</li>
 *   <li>Thành công             → Lưu SessionUser vào session → Redirect /admin/dashboard</li>
 *   <li>Thất bại               → Redirect /dang-nhap?error=true</li>
 *   <li>GET /dang-xuat         → Invalidate session → Redirect /dang-nhap</li>
 * </ol>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthUIController {

    /** Session attribute key — phải khớp với AuthInterceptor.SESSION_KEY. */
    private static final String SESSION_KEY = "CURRENT_ADMIN";

    private final IAuthService authService;

    // =========================================================================
    // GET /dang-nhap — Hiển thị trang đăng nhập
    // =========================================================================

    /**
     * Hiển thị trang đăng nhập.
     * <p>
     * Nếu đã đăng nhập (session tồn tại), tự động redirect về Dashboard.
     *
     * @param error  Có trong URL nếu đăng nhập thất bại (?error=true).
     * @param model  Model Thymeleaf.
     * @param request HttpServletRequest để kiểm tra session hiện tại.
     * @return View "login" hoặc redirect.
     */
    @GetMapping("/dang-nhap")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            Model model,
            HttpServletRequest request) {

        // Nếu đã có session hợp lệ → redirect Dashboard, không show login lại
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null && existingSession.getAttribute(SESSION_KEY) instanceof SessionUser) {
            log.info("[AuthUI] Đã đăng nhập → Redirect /admin/dashboard");
            return "redirect:/admin/dashboard";
        }

        // Truyền flag lỗi sang template
        if ("true".equals(error)) {
            model.addAttribute("errorMessage", "Email hoặc mật khẩu không chính xác. Vui lòng thử lại.");
        }

        model.addAttribute("pageTitle", "Đăng nhập Quản trị");
        return "login";
    }

    // =========================================================================
    // POST /dang-nhap — Xử lý form đăng nhập
    // =========================================================================

    /**
     * Xử lý form đăng nhập.
     * <p>
     * Tạo session mới (session fixation protection) trước khi ghi attribute.
     *
     * @param email     Email đăng nhập (từ form input name="email").
     * @param matKhau   Mật khẩu (từ form input name="matKhau").
     * @param request   HttpServletRequest để tạo/lấy session.
     * @return Redirect Dashboard nếu thành công, redirect login?error nếu thất bại.
     */
    @PostMapping("/dang-nhap")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String matKhau,
            HttpServletRequest request) {

        log.info("[AuthUI] POST /dang-nhap — email={}", email);

        try {
            // Gọi AuthService để xác thực (4 bước fail-fast)
            DangNhapAdminRequest loginRequest = new DangNhapAdminRequest();
            loginRequest.setEmail(email.trim());
            loginRequest.setMatKhau(matKhau);

            SessionUser sessionUser = authService.dangNhapAdmin(loginRequest);

            // Session Fixation Protection: Invalidate session cũ, tạo session mới
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) oldSession.invalidate();

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(SESSION_KEY, sessionUser);
            newSession.setMaxInactiveInterval(8 * 3600); // 8 giờ

            log.info("[AuthUI] Đăng nhập thành công — id={}, role={}", sessionUser.getId(), sessionUser.getRole());
            return "redirect:/admin/dashboard";

        } catch (IllegalArgumentException e) {
            log.warn("[AuthUI] Đăng nhập thất bại — email={}, reason={}", email, e.getMessage());
            return "redirect:/dang-nhap?error=true";
        } catch (Exception e) {
            log.error("[AuthUI] Lỗi hệ thống khi đăng nhập — email={}", email, e);
            return "redirect:/dang-nhap?error=true";
        }
    }

    // =========================================================================
    // GET /dang-xuat — Đăng xuất
    // =========================================================================

    /**
     * Đăng xuất: xóa toàn bộ session và redirect về trang đăng nhập.
     *
     * @param request HttpServletRequest để lấy và invalidate session.
     * @return Redirect /dang-nhap.
     */
    @GetMapping("/dang-xuat")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object user = session.getAttribute(SESSION_KEY);
            if (user instanceof SessionUser su) {
                log.info("[AuthUI] Đăng xuất — id={}, email={}", su.getId(), su.getEmail());
            }
            session.invalidate();
        }
        return "redirect:/dang-nhap";
    }
}
