package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.service.IKhachHangAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller xử lý Xác thực Khách Hàng (đăng nhập / đăng ký / đăng xuất).
 * <p>
 * Base path: {@code /api/auth/khach-hang} — Public endpoint, KHÔNG bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (chỉ chặn {@code /api/admin/**}).
 * <p>
 * Session khách hàng được lưu với key {@link SessionKhachHang#SESSION_KEY} = {@code "CURRENT_CUSTOMER"},
 * hoàn toàn độc lập với session Admin ({@code "CURRENT_ADMIN"}).
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   POST /api/auth/khach-hang/dang-nhap  → Đăng nhập bằng email + mật khẩu
 *   POST /api/auth/khach-hang/dang-ky    → Đăng ký tài khoản mới (tự động đăng nhập sau đó)
 *   POST /api/auth/khach-hang/dang-xuat  → Đăng xuất (hủy session)
 *   GET  /api/auth/khach-hang/toi        → Lấy thông tin khách hàng đang đăng nhập
 * </pre>
 *
 * <h3>Request body mẫu — Đăng nhập:</h3>
 * <pre>{@code
 * {
 *   "email": "nguyen.van.a@gmail.com",
 *   "matKhau": "123456"
 * }
 * }</pre>
 *
 * <h3>Request body mẫu — Đăng ký:</h3>
 * <pre>{@code
 * {
 *   "hoTen": "Nguyễn Văn A",
 *   "email": "nguyen.van.a@gmail.com",
 *   "soDienThoai": "0901234567",
 *   "matKhau": "123456"
 * }
 * }</pre>
 */
@RestController
@RequestMapping("/api/auth/khach-hang")
@RequiredArgsConstructor
public class KhachHangAuthController {

    private static final Logger log = LoggerFactory.getLogger(KhachHangAuthController.class);

    private final IKhachHangAuthService khachHangAuthService;

    // =========================================================================
    // POST /api/auth/khach-hang/dang-nhap
    // =========================================================================

    /**
     * Đăng nhập khách hàng bằng email + mật khẩu.
     * <p>
     * Sau khi xác thực thành công, lưu {@link SessionKhachHang} vào HttpSession
     * với key {@code "CURRENT_CUSTOMER"} để các request sau có thể nhận diện.
     *
     * @param body        JSON chứa {@code email} và {@code matKhau}.
     * @param httpRequest Servlet request để tạo / truy xuất HttpSession.
     * @return HTTP 200 kèm thông tin khách hàng; HTTP 400 nếu sai thông tin.
     */
    @PostMapping("/dang-nhap")
    public ResponseEntity<?> dangNhap(@RequestBody Map<String, String> body,
                                      HttpServletRequest httpRequest) {

        String email   = body.get("email");
        String matKhau = body.get("matKhau");

        log.info("[KhachHangAuth] Yêu cầu đăng nhập — email={}", email);

        try {
            SessionKhachHang session = khachHangAuthService.dangNhap(email, matKhau);

            // Lưu session (tạo mới nếu chưa có)
            HttpSession httpSession = httpRequest.getSession(true);
            httpSession.setAttribute(SessionKhachHang.SESSION_KEY, session);

            log.info("[KhachHangAuth] Đăng nhập thành công — khachHangId={}", session.getKhachHangId());

            return ResponseEntity.ok(buildSuccessResponse("Đăng nhập thành công!", session));

        } catch (IllegalArgumentException e) {
            log.warn("[KhachHangAuth] Đăng nhập thất bại — email={}, lý do={}", email, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        } catch (IllegalStateException e) {
            log.error("[KhachHangAuth] Lỗi hệ thống khi đăng nhập — email={}: {}", email, e.getMessage());
            return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/auth/khach-hang/dang-ky
    // =========================================================================

    /**
     * Đăng ký tài khoản mới và tự động đăng nhập sau đó.
     * <p>
     * Tạo đồng thời 2 bản ghi trong 1 transaction: {@code NguoiDung} + {@code KhachHang}.
     * Nếu đăng ký thành công, trả về session giống như đăng nhập.
     *
     * @param body        JSON chứa {@code hoTen}, {@code email}, {@code soDienThoai}, {@code matKhau}.
     * @param httpRequest Servlet request để tạo HttpSession.
     * @return HTTP 201 kèm thông tin session; HTTP 400 nếu email/SĐT đã tồn tại.
     */
    @PostMapping("/dang-ky")
    public ResponseEntity<?> dangKy(@RequestBody Map<String, String> body,
                                    HttpServletRequest httpRequest) {

        String hoTen       = body.get("hoTen");
        String email       = body.get("email");
        String soDienThoai = body.get("soDienThoai");
        String matKhau     = body.get("matKhau");

        log.info("[KhachHangAuth] Yêu cầu đăng ký — email={}, sdt={}", email, soDienThoai);

        try {
            SessionKhachHang session = khachHangAuthService.dangKy(hoTen, email, soDienThoai, matKhau);

            // Tự động đăng nhập sau đăng ký thành công
            HttpSession httpSession = httpRequest.getSession(true);
            httpSession.setAttribute(SessionKhachHang.SESSION_KEY, session);

            log.info("[KhachHangAuth] Đăng ký thành công — khachHangId={}", session.getKhachHangId());

            return ResponseEntity.status(201)
                    .body(buildSuccessResponse("Đăng ký tài khoản thành công! Chào mừng bạn đến với PrimeMobile.", session));

        } catch (IllegalArgumentException e) {
            log.warn("[KhachHangAuth] Đăng ký thất bại — email={}, lý do={}", email, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/auth/khach-hang/dang-xuat
    // =========================================================================

    /**
     * Đăng xuất — hủy toàn bộ HttpSession hiện tại.
     *
     * @param httpRequest Servlet request để lấy session hiện tại.
     * @return HTTP 200 với thông báo đăng xuất thành công.
     */
    @PostMapping("/dang-xuat")
    public ResponseEntity<?> dangXuat(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session != null) {
            Object attr = session.getAttribute(SessionKhachHang.SESSION_KEY);
            if (attr instanceof SessionKhachHang current) {
                log.info("[KhachHangAuth] Đăng xuất — khachHangId={}", current.getKhachHangId());
            }
            session.invalidate();
        }
        return ResponseEntity.ok(Map.of("success", true, "message", "Đăng xuất thành công."));
    }

    // =========================================================================
    // GET /api/auth/khach-hang/toi
    // =========================================================================

    /**
     * Lấy thông tin khách hàng đang đăng nhập từ session.
     * <p>
     * Frontend gọi endpoint này khi load trang để kiểm tra trạng thái đăng nhập
     * và hiển thị tên người dùng.
     *
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm thông tin session nếu đã đăng nhập; HTTP 401 nếu chưa.
     */
    @GetMapping("/toi")
    public ResponseEntity<?> layThongTinHienTai(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).body(buildErrorResponse("Bạn chưa đăng nhập."));
        }

        Object attr = session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (!(attr instanceof SessionKhachHang current)) {
            return ResponseEntity.status(401).body(buildErrorResponse("Phiên làm việc không hợp lệ."));
        }

        return ResponseEntity.ok(buildSuccessResponse("OK", current));
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", message);
        r.put("data",    data);
        return r;
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }
}
