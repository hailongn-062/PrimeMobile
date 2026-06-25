package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.repository.DonHangRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller lịch sử đơn hàng dành cho Khách Hàng đã đăng nhập.
 * <p>
 * Base path: {@code /api/public/don-hang-cua-toi} — Public endpoint, không bảo
 * vệ
 * bởi {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Bảo mật được thực hiện thủ công tại từng method:
 * <ul>
 * <li>Kiểm tra {@code CURRENT_CUSTOMER} session → HTTP 401 nếu chưa đăng
 * nhập.</li>
 * <li>Kiểm tra đơn hàng thuộc đúng khách hàng trong session → HTTP 403 nếu
 * IDOR.</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * 
 * <pre>
 *   GET /api/public/don-hang-cua-toi            → Danh sách đơn hàng của khách đang đăng nhập
 *   GET /api/public/don-hang-cua-toi/{donHangId} → Chi tiết 1 đơn hàng (kiểm tra ownership)
 * </pre>
 */
@RestController
@RequestMapping("/api/public/don-hang-cua-toi")
@RequiredArgsConstructor
public class DonHangKhachHangController {

    private static final Logger log = LoggerFactory.getLogger(DonHangKhachHangController.class);

    private final DonHangRepository donHangRepository;

    // =========================================================================
    // GET /api/public/don-hang-cua-toi — Lịch sử đơn hàng
    // =========================================================================

    /**
     * Lấy toàn bộ lịch sử đơn hàng của khách hàng đang đăng nhập.
     * <p>
     * Kết quả sắp xếp mới nhất lên đầu (ORDER BY ngay_dat DESC).
     * <p>
     * Ví dụ: {@code GET /api/public/don-hang-cua-toi}
     * <p>
     * Yêu cầu: Session {@code "CURRENT_CUSTOMER"} phải tồn tại (đã đăng nhập).
     *
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm danh sách {@link DonHang}; HTTP 401 nếu chưa đăng nhập.
     */
    @GetMapping
    public ResponseEntity<?> layLichSuDonHang(HttpServletRequest httpRequest) {

        SessionKhachHang session = requireLogin(httpRequest);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(buildErrorResponse("Bạn cần đăng nhập để xem lịch sử đơn hàng."));
        }

        log.info("[DonHangKhachHang] Lấy lịch sử — khachHangId={}", session.getKhachHangId());

        List<DonHang> danhSach = donHangRepository.findByKhachHangIdOrderByNgayDatDesc(session.getKhachHangId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("khachHangId", session.getKhachHangId());
        response.put("tongSoDonHang", danhSach.size());
        response.put("danhSachDonHang", danhSach);

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/public/don-hang-cua-toi/{donHangId} — Chi tiết đơn hàng
    // =========================================================================

    /**
     * Lấy chi tiết 1 đơn hàng của khách hàng đang đăng nhập.
     * <p>
     * Bảo vệ IDOR: Kiểm tra đơn hàng phải thuộc đúng khách hàng trong session.
     * Trả về HTTP 403 nếu khách cố xem đơn của người khác.
     * <p>
     * Ví dụ: {@code GET /api/public/don-hang-cua-toi/42}
     *
     * @param donHangId   ID đơn hàng cần xem.
     * @param httpRequest Servlet request để lấy session.
     * @return HTTP 200 kèm {@link DonHang} chi tiết (kèm danh sách sản phẩm);
     *         HTTP 401 nếu chưa đăng nhập;
     *         HTTP 403 nếu đơn không thuộc về mình;
     *         HTTP 404 nếu đơn không tồn tại.
     */
    @GetMapping("/{donHangId}")
    public ResponseEntity<?> layChiTietDonHang(
            @PathVariable Integer donHangId,
            HttpServletRequest httpRequest) {

        SessionKhachHang session = requireLogin(httpRequest);
        if (session == null) {
            return ResponseEntity.status(401)
                    .body(buildErrorResponse("Bạn cần đăng nhập để xem chi tiết đơn hàng."));
        }

        log.info("[DonHangKhachHang] Xem chi tiết — donHangId={}, khachHangId={}",
                donHangId, session.getKhachHangId());

        // Lấy chi tiết đơn hàng kèm danh sách sản phẩm (FETCH JOIN để tránh N+1)
        DonHang donHang = donHangRepository.findByIdWithDetails(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng #" + donHangId));

        // Bảo vệ IDOR: Đơn hàng phải thuộc về khách hàng đang đăng nhập
        if (!donHang.getKhachHang().getId().equals(session.getKhachHangId())) {
            log.warn("[DonHangKhachHang] 403 IDOR — donHangId={} không thuộc khachHangId={}",
                    donHangId, session.getKhachHangId());
            return ResponseEntity.status(403)
                    .body(buildErrorResponse("Bạn không có quyền xem đơn hàng này."));
        }

        return ResponseEntity.ok(donHang);
    }

    // =========================================================================
    // Exception Handler cục bộ
    // =========================================================================

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Lấy {@link SessionKhachHang} từ HttpSession hiện tại.
     * Trả về {@code null} nếu khách chưa đăng nhập hoặc session không hợp lệ.
     */
    private SessionKhachHang requireLogin(HttpServletRequest httpRequest) {
        HttpSession session = httpRequest.getSession(false);
        if (session == null)
            return null;
        Object attr = session.getAttribute(SessionKhachHang.SESSION_KEY);
        return (attr instanceof SessionKhachHang kh) ? kh : null;
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", false);
        r.put("message", message);
        return r;
    }
}
