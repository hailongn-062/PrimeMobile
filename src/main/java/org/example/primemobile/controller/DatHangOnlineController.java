package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.VnPayPaymentResponse;
import org.example.primemobile.dto.request.DatHangRequest;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.service.IDatHangOnlineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller xử lý Đặt Hàng Online (Checkout).
 * <p>
 * Base path: {@code /api/public/dat-hang} — public endpoint, không bảo vệ bởi
 * AuthInterceptor.
 * <p>
 * Endpoints:
 *
 * <pre>
 *   POST /api/public/dat-hang       → Tạo đơn hàng từ giỏ hàng hiện tại (COD, chuyển khoản...)
 *   POST /api/public/dat-hang/vnpay → Tạo đơn hàng và trả về URL thanh toán VNPay
 * </pre>
 *
 * <h3>Phân luồng xử lý lỗi:</h3>
 * <ul>
 * <li>{@link IllegalArgumentException} → HTTP 400 Bad Request (hết hàng, giỏ
 * trống, mã giảm giá lỗi...)</li>
 * <li>{@link EntityNotFoundException} → HTTP 404 Not Found (khách hàng / địa
 * chỉ / PTTT không tồn tại)</li>
 * <li>{@link IllegalStateException} → HTTP 500 Internal (cấu hình kho lỗi — cần
 * Admin kiểm tra)</li>
 * <li>Các lỗi khác → HTTP 500 Internal Server Error</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/public/dat-hang")
@RequiredArgsConstructor
public class DatHangOnlineController {

    private static final Logger log = LoggerFactory.getLogger(DatHangOnlineController.class);

    private final IDatHangOnlineService datHangOnlineService;

    // =========================================================================
    // POST /api/public/dat-hang — Tạo đơn hàng (Checkout) cho COD và các phương thức khác
    // =========================================================================

    /**
     * Thực hiện toàn bộ quy trình checkout: Giỏ hàng → Đơn hàng chính thức.
     * <p>
     * Nhận {@link DatHangRequest} từ body JSON, gọi {@code taoDonHang} trong
     * service
     * và trả về thông tin đơn hàng vừa tạo.
     *
     * <h3>Request Body mẫu (dùng địa chỉ đã lưu):</h3>
     * <pre>{@code
     * {
     * "khachHangId": 1,
     * "diaChiGiaoId": 3,
     * "phuongThucThanhToanId": 2,
     * "phiShip": 30000,
     * "ghiChu": "Gọi trước khi giao"
     * }
     * }</pre>
     *
     * <h3>Request Body mẫu (nhập địa chỉ mới):</h3>
     * <pre>{@code
     * {
     * "khachHangId": 1,
     * "hoTenNguoiNhan": "Nguyễn Văn A",
     * "sdtNguoiNhan": "0901234567",
     * "diaChiGiaoCuThe": "123 Nguyễn Huệ",
     * "tinhThanhGiao": "TP. Hồ Chí Minh",
     * "quanHuyenGiao": "Quận 1",
     * "phuongXaGiao": "Phường Bến Nghé",
     * "phuongThucThanhToanId": 4,
     * "maGiamGiaId": 5,
     * "phiShip": 25000,
     * "ghiChu": null
     * }
     * }</pre>
     *
     * @param request DTO chứa thông tin checkout từ frontend.
     * @return HTTP 201 Created kèm thông tin đơn hàng vừa tạo.
     * HTTP 400 nếu giỏ trống, kho không đủ, mã giảm giá lỗi.
     * HTTP 404 nếu không tìm thấy entity liên quan.
     * HTTP 500 nếu có lỗi hệ thống (cấu hình kho...).
     */
    @PostMapping
    public ResponseEntity<?> datHang(@RequestBody DatHangRequest request) {

        log.info("[DatHangController] ▶ Nhận request checkout — khachHangId={}, sessionId={}, ptttId={}",
                request.khachHangId(), request.sessionId(), request.phuongThucThanhToanId());

        try {
            DonHang donHang = datHangOnlineService.taoDonHang(request);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", "Đặt hàng thành công!");
            response.put("maDonHang", donHang.getMaDonHang());
            response.put("donHangId", donHang.getId());
            response.put("trangThai", donHang.getTrangThai());
            response.put("trangThaiThanhToan", donHang.getTrangThaiThanhToan());
            response.put("tongTienHang", donHang.getTongTienHang());
            response.put("tienGiamGia", donHang.getTienGiamGia());
            response.put("phiShip", donHang.getPhiShip());
            // Tính tongThanhToan thủ công vì computed column chưa được DB fill lại
            response.put("tongThanhToan",
                    donHang.getTongTienHang()
                            .subtract(donHang.getTienGiamGia())
                            .add(donHang.getPhiShip()));
            response.put("thoiGianHetHanTt", donHang.getThoiGianHetHanTt());
            response.put("ngayDat", donHang.getNgayDat());

            log.info("[DatHangController] ✅ Checkout thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Lỗi nghiệp vụ có thể gặp: giỏ trống, kho không đủ, mã giảm giá hết lượt...
            log.warn("[DatHangController] ❌ Lỗi nghiệp vụ (400): {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(400, e.getMessage()));

        } catch (EntityNotFoundException e) {
            // Entity không tồn tại: khách hàng, địa chỉ, phương thức thanh toán...
            log.warn("[DatHangController] ❌ Không tìm thấy entity (404): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse(404, e.getMessage()));

        } catch (IllegalStateException e) {
            // Lỗi cấu hình hệ thống: kho t?ng chưa tồn tại trong DB...
            log.error("[DatHangController] ❌ Lỗi cấu hình hệ thống (500): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse(500, "Lỗi hệ thống nội bộ. Vui lòng liên hệ Admin."));

        } catch (Exception e) {
            log.error("[DatHangController] ❌ Lỗi không mong đợi (500): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse(500, "Đã xảy ra lỗi không mong đợi. Vui lòng thử lại."));
        }
    }

    // =========================================================================
    // POST /api/public/dat-hang/vnpay — Tạo đơn hàng và trả về URL thanh toán VNPay
    // =========================================================================

    /**
     * Thực hiện checkout cho phương thức thanh toán VNPay.
     * <p>
     * Khác với {@link #datHang(DatHangRequest)}, endpoint này sẽ:
     * <ol>
     *   <li>Tạo đơn hàng với trạng thái {@code trangThaiThanhToan = "dang_chuyen_huong"}.</li>
     *   <li>Tạo bản ghi thanh toán với trạng thái {@code "cho"}.</li>
     *   <li>Gọi VNPay để tạo URL thanh toán.</li>
     *   <li>Trả về URL để frontend redirect.</li>
     * </ol>
     *
     * <h3>Request Body mẫu:</h3>
     * <pre>{@code
     * {
     *   "khachHangId": 1,
     *   "diaChiGiaoId": 3,
     *   "phuongThucThanhToanId": 3,  // ID của VNPay (phải khớp với DB)
     *   "phiShip": 30000,
     *   "ghiChu": "Gọi trước khi giao"
     * }
     * }</pre>
     *
     * @param request    DTO chứa thông tin checkout từ frontend.
     * @param httpRequest HttpServletRequest để lấy địa chỉ IP client.
     * @return HTTP 200 kèm {@link VnPayPaymentResponse} chứa URL thanh toán.
     * HTTP 400 nếu giỏ trống, kho không đủ, hoặc phương thức thanh toán không phải VNPay.
     * HTTP 404 nếu không tìm thấy entity liên quan.
     * HTTP 500 nếu có lỗi hệ thống.
     */
    @PostMapping("/vnpay")
    public ResponseEntity<?> datHangVnPay(
            @RequestBody DatHangRequest request,
            HttpServletRequest httpRequest) {

        log.info("[DatHangController] ▶ Nhận request checkout VNPay — khachHangId={}, sessionId={}, ptttId={}",
                request.khachHangId(), request.sessionId(), request.phuongThucThanhToanId());

        try {
            // Lấy địa chỉ IP client
            String clientIp = getClientIp(httpRequest);
            log.info("[DatHangController] Client IP: {}", clientIp);

            // Gọi service tạo đơn và URL thanh toán
            VnPayPaymentResponse response = datHangOnlineService.taoDonHangVnPay(request, clientIp);

            log.info("[DatHangController] ✅ Checkout VNPay thành công — maDonHang={}, paymentUrl={}",
                    response.getMaDonHang(), response.getPaymentUrl());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.warn("[DatHangController] ❌ Lỗi nghiệp vụ VNPay (400): {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(400, e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[DatHangController] ❌ Không tìm thấy entity VNPay (404): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(buildErrorResponse(404, e.getMessage()));

        } catch (Exception e) {
            log.error("[DatHangController] ❌ Lỗi không mong đợi VNPay (500): {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse(500, "Đã xảy ra lỗi không mong đợi. Vui lòng thử lại."));
        }
    }

    // =========================================================================
    // Exception Handler cục bộ — fallback cho annotation-based throw
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[DatHangController] Handler toàn cục — IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildErrorResponse(400, e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Lấy địa chỉ IP thực tế của client từ HttpServletRequest.
     * <p>
     * Xử lý các trường hợp client đi qua proxy (X-Forwarded-For) để lấy IP chính xác.
     *
     * @param request HttpServletRequest hiện tại.
     * @return Địa chỉ IP của client.
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Trong trường hợp X-Forwarded-For có nhiều IP (proxy chain), lấy IP đầu tiên
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * Tạo response body lỗi theo chuẩn thống nhất của hệ thống.
     *
     * @param statusCode HTTP status code (400, 404, 500...).
     * @param message    Thông báo lỗi chi tiết.
     * @return Map với các field: {@code success}, {@code statusCode},
     *         {@code message}.
     */
    private Map<String, Object> buildErrorResponse(int statusCode, String message) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("success", false);
        error.put("statusCode", statusCode);
        error.put("message", message);
        return error;
    }
}
