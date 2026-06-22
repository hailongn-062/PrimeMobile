package org.example.primemobile.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.primemobile.dto.auth.SessionUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor bảo vệ toàn bộ các endpoint Admin ({@code /admin/**} và {@code /api/admin/**}).
 * <p>
 * Đây là cơ chế bảo mật duy nhất của hệ thống — thay thế Spring Security theo
 * system_rules.md §1: <i>"Bắt buộc quản lý phiên đăng nhập bằng HttpSession kết hợp
 * với HandlerInterceptor"</i>.
 * <p>
 * Chiến lược kiểm tra (theo thứ tự):
 * <ol>
 *   <li>Session không tồn tại hoặc không có attribute {@code "CURRENT_ADMIN"}
 *       → Trả về HTTP {@code 401 Unauthorized}.</li>
 *   <li>Role không phải {@code "Admin"} hoặc {@code "NhanVien"}
 *       → Trả về HTTP {@code 403 Forbidden}.</li>
 *   <li>Hợp lệ → Cho phép request đi qua ({@code return true}).</li>
 * </ol>
 *
 * @see org.example.primemobile.config.WebMvcConfig
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /** Key attribute lưu thông tin người dùng trong HttpSession. */
    public static final String SESSION_KEY = "CURRENT_ADMIN";

    /** Các vai trò được phép truy cập khu vực Admin Dashboard. */
    private static final String ROLE_ADMIN     = "Admin";
    private static final String ROLE_NHAN_VIEN = "NhanVien";

    /**
     * Chặn request TRƯỚC khi đến Controller.
     * <p>
     * Dùng {@code request.getSession(false)} (không tạo session mới nếu chưa có)
     * để tránh tạo session rỗng không cần thiết trên server.
     *
     * @param request  Request đến từ client.
     * @param response Response sẽ gửi về client.
     * @param handler  Handler (thường là Controller method) sẽ xử lý request.
     * @return {@code true} nếu được phép tiếp tục, {@code false} nếu đã xử lý response (chặn).
     */
    @Override
    public boolean preHandle(HttpServletRequest  request,
                             HttpServletResponse response,
                             Object              handler) throws Exception {

        String requestUri = request.getRequestURI();

        // Lấy session hiện có, KHÔNG tạo mới nếu chưa tồn tại
        HttpSession session = request.getSession(false);

        // ------------------------------------------------------------------
        // Kiểm tra 1: Session tồn tại và có attribute CURRENT_ADMIN
        // ------------------------------------------------------------------
        if (session == null || session.getAttribute(SESSION_KEY) == null) {
            log.warn("[AuthInterceptor] 401 Unauthorized — Chưa đăng nhập. URI={}", requestUri);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\"," +
                    "\"message\":\"Bạn chưa đăng nhập. Vui lòng đăng nhập để tiếp tục.\"," +
                    "\"path\":\"" + requestUri + "\"}"
            );
            return false;
        }

        // ------------------------------------------------------------------
        // Kiểm tra 2: Đúng kiểu SessionUser
        // ------------------------------------------------------------------
        Object sessionAttr = session.getAttribute(SESSION_KEY);
        if (!(sessionAttr instanceof SessionUser currentUser)) {
            log.error("[AuthInterceptor] 401 — Attribute session không hợp lệ. URI={}", requestUri);
            session.invalidate();  // Xóa session bẩn
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write(
                    "{\"status\":401,\"error\":\"Unauthorized\"," +
                    "\"message\":\"Phiên làm việc không hợp lệ. Vui lòng đăng nhập lại.\"," +
                    "\"path\":\"" + requestUri + "\"}"
            );
            return false;
        }

        // ------------------------------------------------------------------
        // Kiểm tra 3: Vai trò phải là Admin hoặc NhanVien
        // ------------------------------------------------------------------
        String role = currentUser.getRole();
        if (!ROLE_ADMIN.equals(role) && !ROLE_NHAN_VIEN.equals(role)) {
            log.warn("[AuthInterceptor] 403 Forbidden — Vai trò không đủ quyền. " +
                     "userId={}, role={}, URI={}", currentUser.getId(), role, requestUri);
            response.setContentType("application/json;charset=UTF-8");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.getWriter().write(
                    "{\"status\":403,\"error\":\"Forbidden\"," +
                    "\"message\":\"Bạn không có quyền truy cập vào khu vực này.\"," +
                    "\"path\":\"" + requestUri + "\"}"
            );
            return false;
        }

        // ------------------------------------------------------------------
        // Hợp lệ — Cho phép request tiếp tục đến Controller
        // ------------------------------------------------------------------
        log.debug("[AuthInterceptor] Cho phép — userId={}, role={}, URI={}",
                currentUser.getId(), role, requestUri);
        return true;
    }
}
