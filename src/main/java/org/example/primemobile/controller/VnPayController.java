package org.example.primemobile.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.service.IVnPayService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller xử lý callback từ VNPay.
 * <p>
 * Base path: {@code /api/vnpay}
 * <p>
 * Endpoints:
 * <ul>
 *   <li><b>IPN (Instant Payment Notification):</b> VNPay gọi server-to-server để xác nhận giao dịch.</li>
 *   <li><b>Return URL:</b> Khách hàng được redirect về sau khi thanh toán thành công/thất bại.</li>
 * </ul>
 *
 * <h3>Luồng xử lý:</h3>
 * <ol>
 *   <li>Khách hàng thanh toán trên VNPay.</li>
 *   <li>VNPay gọi IPN để thông báo kết quả → cập nhật trạng thái đơn hàng trong DB.</li>
 *   <li>Trình duyệt khách hàng được redirect về Return URL → hiển thị trang kết quả.</li>
 * </ol>
 */
@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VnPayController {

    private final IVnPayService vnPayService;

    // =========================================================================
    // POST /api/vnpay/ipn — Xử lý IPN từ VNPay
    // =========================================================================

    /**
     * Xử lý IPN (Instant Payment Notification) từ VNPay.
     * <p>
     * VNPay gọi endpoint này (server-to-server) ngay sau khi giao dịch hoàn tất
     * để thông báo kết quả. Phương thức này sẽ xác thực chữ ký, tìm giao dịch,
     * và cập nhật trạng thái đơn hàng.
     * <p>
     * <b>Quy tắc phản hồi:</b>
     * <ul>
     *   <li>Phản hồi phải là JSON với các trường {@code RspCode} và {@code Message}.</li>
     *   <li>Nếu xử lý thành công (dù giao dịch thành công hay thất bại), trả về {@code RspCode: 00}.</li>
     *   <li>Nếu có lỗi (chữ ký sai, không tìm thấy giao dịch...), trả về mã lỗi tương ứng.</li>
     * </ul>
     *
     * @param request HttpServletRequest chứa toàn bộ tham số từ VNPay.
     * @return ResponseEntity với chuỗi JSON phản hồi.
     */
    @PostMapping(value = "/ipn", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> ipn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("[VNPay] IPN nhận được với {} tham số", params.size());

        try {
            String response = vnPayService.processIpn(params);
            log.info("[VNPay] IPN phản hồi: {}", response);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("[VNPay] IPN xử lý thất bại: {}", e.getMessage(), e);
            // Trong trường hợp lỗi không mong đợi, trả về mã lỗi chung
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"RspCode\":\"99\",\"Message\":\"Internal server error\"}");
        }
    }

    // =========================================================================
    // GET /api/vnpay/vnpay-return — Xử lý Return URL từ VNPay
    // =========================================================================

    /**
     * Xử lý return URL từ VNPay (khách hàng được redirect về).
     * <p>
     * Sau khi khách hàng thanh toán trên VNPay, trình duyệt sẽ được chuyển hướng
     * về endpoint này với các tham số kết quả. Phương thức này sẽ:
     * <ol>
     *   <li>Xác thực chữ ký.</li>
     *   <li>Cập nhật trạng thái đơn hàng (nếu IPN chưa kịp xử lý).</li>
     *   <li>Redirect đến trang kết quả thanh toán với thông tin đơn hàng.</li>
     * </ol>
     *
     * @param request HttpServletRequest chứa các tham số từ VNPay.
     * @return Redirect đến trang kết quả thanh toán.
     */
    @GetMapping("/vnpay-return")
    public ResponseEntity<Void> vnpayReturn(HttpServletRequest request) {
        Map<String, String> params = extractParams(request);
        log.info("[VNPay] Return nhận được với {} tham số", params.size());

        try {
            // Xử lý return và lấy đơn hàng đã cập nhật
            DonHang donHang = vnPayService.processReturn(params);

            // Xây dựng URL redirect đến trang kết quả
            String redirectUrl = "/thanh-toan-ket-qua?maDonHang=" + donHang.getMaDonHang()
                    + "&status=" + donHang.getTrangThaiThanhToan()
                    + "&tongTien=" + donHang.getTongThanhToan();

            log.info("[VNPay] Redirect đến: {}", redirectUrl);

            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", redirectUrl)
                    .build();

        } catch (IllegalArgumentException e) {
            // Lỗi xác thực hoặc không tìm thấy giao dịch
            log.warn("[VNPay] Return xử lý thất bại: {}", e.getMessage());
            String errorUrl = "/thanh-toan-ket-qua?status=that_bai&message="
                    + java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorUrl)
                    .build();

        } catch (Exception e) {
            // Lỗi không mong đợi
            log.error("[VNPay] Return xử lý lỗi: {}", e.getMessage(), e);
            String errorUrl = "/thanh-toan-ket-qua?status=that_bai&message=He_thong_loi";
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header("Location", errorUrl)
                    .build();
        }
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Trích xuất tất cả tham số từ HttpServletRequest thành Map<String, String>.
     * <p>
     * VNPay gửi tất cả tham số dưới dạng query parameters (GET) hoặc form data (POST),
     * phương thức này đọc tất cả và chuyển thành Map.
     *
     * @param request HttpServletRequest hiện tại.
     * @return Map chứa tất cả tham số (key-value).
     */
    private Map<String, String> extractParams(HttpServletRequest request) {
        return request.getParameterMap().entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> {
                            String[] values = e.getValue();
                            return values != null && values.length > 0 ? values[0] : "";
                        },
                        (v1, v2) -> v1 // Trong trường hợp trùng key (không xảy ra)
                ));
    }
}