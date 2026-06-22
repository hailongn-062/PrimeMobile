package org.example.primemobile.config;

import org.example.primemobile.interceptor.AuthInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cấu hình Spring Web MVC — đăng ký interceptor bảo vệ khu vực Admin.
 * <p>
 * Tuân thủ system_rules.md §1: Phân quyền bằng {@code HandlerInterceptor},
 * KHÔNG dùng Spring Security.
 *
 * <h2>Quy tắc mapping đường dẫn:</h2>
 * <ul>
 *   <li><b>Chặn:</b>
 *     <ul>
 *       <li>{@code /admin/**}      — Các trang Thymeleaf Admin Dashboard</li>
 *       <li>{@code /api/admin/**}  — Các REST endpoint dành cho Admin/NhanVien</li>
 *     </ul>
 *   </li>
 *   <li><b>Bỏ qua (excludePathPatterns):</b>
 *     <ul>
 *       <li>{@code /api/auth/**}            — Endpoint đăng nhập / đăng xuất (public)</li>
 *       <li>{@code /css/**}, {@code /js/**} — File tĩnh CSS, JavaScript</li>
 *       <li>{@code /images/**}              — Hình ảnh tĩnh</li>
 *       <li>{@code /favicon.ico}            — Icon trình duyệt</li>
 *     </ul>
 *   </li>
 * </ul>
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    /**
     * Constructor injection thay vì @Autowired — chuẩn best practice Spring.
     *
     * @param authInterceptor Bean interceptor đã khai báo @Component.
     */
    public WebMvcConfig(AuthInterceptor authInterceptor) {
        this.authInterceptor = authInterceptor;
    }

    /**
     * Đăng ký {@link AuthInterceptor} vào chuỗi interceptor của Spring MVC.
     * <p>
     * Thứ tự thêm interceptor vào registry quyết định thứ tự thực thi.
     * AuthInterceptor được thêm đầu tiên để đảm bảo mọi request vào
     * khu vực admin đều bị kiểm tra trước khi đến bất kỳ interceptor nào khác.
     *
     * @param registry Registry do Spring cung cấp để đăng ký interceptors.
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                // ── Các path cần bảo vệ ──────────────────────────────────────
                .addPathPatterns(
                        "/admin/**",        // Trang Thymeleaf Admin Dashboard
                        "/api/admin/**"     // REST API dành cho Admin/NhanVien
                )
                // ── Các path được bỏ qua (không cần xác thực) ───────────────
                .excludePathPatterns(
                        "/api/auth/**",     // Đăng nhập, đăng xuất (public)
                        "/css/**",          // File CSS tĩnh
                        "/js/**",           // File JavaScript tĩnh
                        "/images/**",       // Hình ảnh tĩnh
                        "/fonts/**",        // Font chữ tĩnh
                        "/favicon.ico"      // Icon trình duyệt
                );
    }
}
