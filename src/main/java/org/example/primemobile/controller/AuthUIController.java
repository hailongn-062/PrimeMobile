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

/**
 * Controller UI cho luồng Đăng nhập / Đăng xuất của Admin & NhanVien.
 * <p>
 * Tuân thủ system_rules.md §1: Dùng {@code HttpSession}, KHÔNG Spring Security.
 *
 * <h3>Luồng đăng nhập:</h3>
 * <ol>
 * <li>GET /dang-nhap → Hiển thị trang login.html</li>
 * <li>POST /dang-nhap → Gọi IAuthService.dangNhapAdmin()</li>
 * <li>Thành công → Lưu SessionUser vào session → Redirect /admin/dashboard</li>
 * <li>Thất bại → Redirect /dang-nhap?error=true</li>
 * <li>GET /dang-xuat → Invalidate session → Redirect /dang-nhap</li>
 * </ol>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class AuthUIController {

    private static final String SESSION_KEY = "CURRENT_ADMIN";

    private final IAuthService authService;
    private final IKhachHangAuthService khachHangAuthService;

    // =========================================================================
    // GET /dang-nhap — Hiển thị trang đăng nhập
    // =========================================================================

    /**
     * Hiển thị trang đăng nhập.
     * <p>
     * Nếu đã đăng nhập (session tồn tại), tự động redirect về Dashboard.
     *
     * @param error   Có trong URL nếu đăng nhập thất bại (?error=true).
     * @param model   Model Thymeleaf.
     * @param request HttpServletRequest để kiểm tra session hiện tại.
     * @return View "login" hoặc redirect.
     */
    @GetMapping("/dang-nhap")
    public String showLoginPage(
            @RequestParam(required = false) String error,
            Model model,
            HttpServletRequest request) {

        // Nếu đã có session hợp lệ của Admin/NhanVien → redirect Dashboard
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null && existingSession.getAttribute(SESSION_KEY) instanceof SessionUser) {
            log.info("[AuthUI] Đã đăng nhập Admin → Redirect /admin/dashboard");
            return "redirect:/admin/dashboard";
        }

        // Nếu đã có session Khách Hàng → redirect trang chủ
        if (existingSession != null && existingSession.getAttribute(SessionKhachHang.SESSION_KEY) instanceof SessionKhachHang) {
            log.info("[AuthUI] Đã đăng nhập Khách Hàng → Redirect /");
            return "redirect:/";
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
     * @param email   Email đăng nhập (từ form input name="email").
     * @param matKhau Mật khẩu (từ form input name="matKhau").
     * @param request HttpServletRequest để tạo/lấy session.
     * @return Redirect Dashboard nếu thành công, redirect login?error nếu thất bại.
     */
    @PostMapping("/dang-nhap")
    public String processLogin(
            @RequestParam String email,
            @RequestParam String matKhau,
            HttpServletRequest request) {

        log.info("[AuthUI] POST /dang-nhap — identifier={}", email);

        try {
            // Cố gắng đăng nhập như Khách Hàng trước
            try {
                SessionKhachHang sessionKH = khachHangAuthService.dangNhap(email, matKhau);

                // Session Fixation Protection
                HttpSession oldSession = request.getSession(false);
                if (oldSession != null)
                    oldSession.invalidate();

                HttpSession newSession = request.getSession(true);
                newSession.setAttribute(SessionKhachHang.SESSION_KEY, sessionKH);
                newSession.setMaxInactiveInterval(8 * 3600); // 8 giờ

                log.info("[AuthUI] Đăng nhập Khách Hàng thành công — id={}", sessionKH.getKhachHangId());
                return "redirect:/"; // Redirect về trang chủ cho Khách Hàng

            } catch (IllegalArgumentException e) {
                // Nếu sai thông tin hoặc không phải Khách Hàng, thử tiếp luồng Admin
                log.info("[AuthUI] Không phải Khách Hàng hoặc sai mật khẩu, chuyển sang thử Admin.");
            }

            // Gọi AuthService để xác thực Admin/NhanVien
            DangNhapAdminRequest loginRequest = new DangNhapAdminRequest();
            loginRequest.setEmail(email.trim());
            loginRequest.setMatKhau(matKhau);

            SessionUser sessionUser = authService.dangNhapAdmin(loginRequest);

            // Session Fixation Protection
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null)
                oldSession.invalidate();

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(SESSION_KEY, sessionUser);
            newSession.setMaxInactiveInterval(8 * 3600); // 8 giờ

            log.info("[AuthUI] Đăng nhập Admin thành công — id={}, role={}", sessionUser.getId(),
                    sessionUser.getRole());
            return "redirect:/admin/dashboard";

        } catch (IllegalArgumentException e) {
            log.warn("[AuthUI] Đăng nhập thất bại — identifier={}, reason={}", email, e.getMessage());
            return "redirect:/dang-nhap?error=true";
        } catch (Exception e) {
            log.error("[AuthUI] Lỗi hệ thống khi đăng nhập — identifier={}", email, e);
            return "redirect:/dang-nhap?error=true";
        }
    }

    // =========================================================================
    // GET /dang-ky — Hiển thị trang đăng ký
    // =========================================================================

    @GetMapping("/dang-ky")
    public String showRegisterPage(
            @RequestParam(required = false) String error,
            Model model,
            HttpServletRequest request) {

        // Nếu đã có session Khách Hàng → redirect trang chủ
        HttpSession existingSession = request.getSession(false);
        if (existingSession != null
                && existingSession.getAttribute(SessionKhachHang.SESSION_KEY) instanceof SessionKhachHang) {
            return "redirect:/";
        }

        if ("true".equals(error)) {
            model.addAttribute("errorMessage", "Thông tin đăng ký không hợp lệ hoặc đã tồn tại.");
        }

        model.addAttribute("pageTitle", "Đăng ký");
        return "register";
    }

    // =========================================================================
    // POST /dang-ky — Xử lý form đăng ký
    // =========================================================================

    @PostMapping("/dang-ky")
    public String processRegister(
            @RequestParam String hoTen,
            @RequestParam String soDienThoai,
            @RequestParam String email,
            @RequestParam String matKhau,
            HttpServletRequest request) {

        log.info("[AuthUI] POST /dang-ky — email={}", email);

        try {
            SessionKhachHang sessionKH = khachHangAuthService.dangKy(hoTen, email, soDienThoai, matKhau);

            // Tự động đăng nhập
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null)
                oldSession.invalidate();

            HttpSession newSession = request.getSession(true);
            newSession.setAttribute(SessionKhachHang.SESSION_KEY, sessionKH);
            newSession.setMaxInactiveInterval(8 * 3600);

            log.info("[AuthUI] Đăng ký thành công — id={}", sessionKH.getKhachHangId());
            return "redirect:/";

        } catch (IllegalArgumentException e) {
            log.warn("[AuthUI] Đăng ký thất bại — email={}, reason={}", email, e.getMessage());
            return "redirect:/dang-ky?error=true";
        } catch (Exception e) {
            log.error("[AuthUI] Lỗi hệ thống khi đăng ký — email={}", email, e);
            return "redirect:/dang-ky?error=true";
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
