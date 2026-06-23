package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.DangNhapAdminRequest;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.interceptor.AuthInterceptor;
import org.example.primemobile.service.IAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller xử lý xác thực Admin / NhanVien.
 * <p>
 * Base path: {@code /api/auth/admin} — được {@link org.example.primemobile.config.WebMvcConfig}
 * loại trừ khỏi {@link AuthInterceptor} (public endpoint, không cần đăng nhập trước).
 * <p>
 * Luồng đăng nhập (system_rules.md §1):
 * <ol>
 *   <li>Client gửi {@code POST /api/auth/admin/dang-nhap} với JSON body.</li>
 *   <li>Service xác minh email + mật khẩu băm SHA-256.</li>
 *   <li>Nếu hợp lệ, Controller lưu {@link SessionUser} vào {@code HttpSession}
 *       với key {@code "CURRENT_ADMIN"} để {@link AuthInterceptor} nhận diện.</li>
 *   <li>Trả về HTTP 200 kèm thông tin người dùng cho Front-end.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth/admin")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final IAuthService authService;

    // =========================================================================
    // POST /api/auth/admin/dang-nhap
    // =========================================================================

    /**
     * Đăng nhập cho Admin hoặc NhanVien.
     * <p>
     * Sau khi Service xác minh thành công, Controller tạo / lấy {@code HttpSession}
     * và đặt {@link SessionUser} vào attribute {@code "CURRENT_ADMIN"}.
     * Key này phải khớp với {@link AuthInterceptor#SESSION_KEY}.
     *
     * @param request     DTO chứa email và mật khẩu plain-text từ body JSON.
     * @param httpRequest Servlet request dùng để tạo / truy xuất HttpSession.
     * @return HTTP 200 kèm {@link SessionUser} nếu thành công;
     *         HTTP 400 nếu sai mật khẩu, tài khoản bị khóa, hoặc vai trò không hợp lệ;
     *         HTTP 404 nếu email không tồn tại.
     */
    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody DangNhapAdminRequest request,
                                      HttpServletRequest httpRequest) {
        log.info("[AuthController] Yêu cầu đăng nhập — email={}", request.getEmail());
        try {
            // 1. Xác thực thông tin — Service ném ngoại lệ nếu sai
            SessionUser sessionUser = authService.dangNhapAdmin(request);

            // 2. Tạo session mới (hoặc lấy session hiện tại) và lưu thông tin người dùng
            //    Dùng getSession(true) để tạo mới nếu chưa có
            HttpSession session = httpRequest.getSession(true);
            session.setAttribute(AuthInterceptor.SESSION_KEY, sessionUser);

            log.info("[AuthController] Đăng nhập thành công — userId={}, role={}",
                    sessionUser.getId(), sessionUser.getRole());

            // 3. Trả về thông tin người dùng cho Front-end (không bao gồm mật khẩu)
            return ResponseEntity.ok(sessionUser);

        } catch (IllegalArgumentException e) {
            // Mật khẩu sai, tài khoản bị khóa, hoặc vai trò không hợp lệ
            log.warn("[AuthController] Đăng nhập thất bại — email={}, lý do={}",
                    request.getEmail(), e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (EntityNotFoundException e) {
            // Email không tồn tại trong hệ thống
            log.warn("[AuthController] Đăng nhập thất bại — email không tồn tại: {}",
                    request.getEmail());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // POST /api/auth/admin/dang-xuat
    // =========================================================================

    /**
     * Đăng xuất — hủy toàn bộ {@code HttpSession} hiện tại.
     * <p>
     * Dùng {@code getSession(false)} để tránh tạo session mới không cần thiết
     * khi client gọi endpoint này mà không có session tồn tại.
     *
     * @param httpRequest Servlet request để lấy session hiện tại.
     * @return HTTP 200 với thông báo đăng xuất thành công.
     */
    @PostMapping("/dang-xuat")
    public ResponseEntity<?> dangXuat(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object sessionAttr = session.getAttribute(AuthInterceptor.SESSION_KEY);
            if (sessionAttr instanceof SessionUser currentUser) {
                log.info("[AuthController] Đăng xuất — userId={}, role={}",
                        currentUser.getId(), currentUser.getRole());
            }
            // Hủy toàn bộ session, xóa mọi attribute
            session.invalidate();
        }
        return ResponseEntity.ok("Đăng xuất thành công.");
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback cho các lỗi không mong muốn)
    // =========================================================================

    /**
     * Bắt các {@link IllegalArgumentException} không được xử lý trong method handler.
     * Trả về HTTP 400 kèm message lỗi rõ ràng cho client / Postman.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
