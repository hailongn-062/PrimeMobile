package org.example.primemobile.exception;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bộ xử lý ngoại lệ tập trung cho toàn bộ REST API của PrimeMobile.
 * <p>
 * Thay vì để mỗi Controller tự xử lý lỗi riêng lẻ (gây code trùng lặp),
 * class này bắt tất cả ngoại lệ không được xử lý và trả về cấu trúc JSON
 * thống nhất, thân thiện với Frontend.
 *
 * <h2>Cấu trúc JSON response lỗi:</h2>
 * <pre>{@code
 * {
 *   "status":    404,
 *   "error":     "Not Found",
 *   "message":   "Không tìm thấy sản phẩm ID: 99",
 *   "path":      "/api/admin/san-pham/99",
 *   "timestamp": "2024-06-23T22:53:00"
 * }
 * }</pre>
 *
 * <h2>Thứ tự ưu tiên bắt lỗi (từ cụ thể → tổng quát):</h2>
 * <ol>
 *   <li>{@link EntityNotFoundException}  → HTTP 404 Not Found</li>
 *   <li>{@link IllegalArgumentException} → HTTP 400 Bad Request</li>
 *   <li>{@link Exception}               → HTTP 500 Internal Server Error</li>
 * </ol>
 *
 * <h2>Lưu ý thiết kế:</h2>
 * Các Controller đã có try-catch cục bộ sẽ KHÔNG bị bắt bởi handler này
 * (lỗi đã được xử lý trước). Handler này chỉ bắt các lỗi "thoát ra" khỏi
 * Controller mà không được xử lý — đóng vai trò tuyến phòng thủ cuối cùng.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter TIMESTAMP_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    // =========================================================================
    // HTTP 404 — Entity không tồn tại trong database
    // =========================================================================

    /**
     * Bắt {@link EntityNotFoundException} — xảy ra khi tìm kiếm entity
     * theo ID nhưng không có kết quả (vd: sản phẩm, đơn hàng, khách hàng...).
     * <p>
     * Được ném từ: {@code repository.findById(...).orElseThrow(EntityNotFoundException::new)}
     * hoặc từ các Service Layer dùng pattern Fail-Fast.
     *
     * @return HTTP 404 Not Found kèm thông điệp lỗi cụ thể.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEntityNotFound(
            EntityNotFoundException ex,
            WebRequest request) {

        log.warn("[GlobalExHandler] 404 EntityNotFound: {}", ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // =========================================================================
    // HTTP 400 — Vi phạm quy tắc nghiệp vụ
    // =========================================================================

    /**
     * Bắt {@link IllegalArgumentException} — xảy ra khi dữ liệu đầu vào
     * hoặc trạng thái nghiệp vụ vi phạm quy tắc hệ thống.
     *
     * <h3>Các tình huống điển hình trong PrimeMobile:</h3>
     * <ul>
     *   <li>Đặt hàng quá số lượng tồn kho (system_rules.md §3.1).</li>
     *   <li>Số IMEI vượt tổng tồn kho (system_rules.md §3.3).</li>
     *   <li>Trùng mã SKU / mã IMEI / mã đơn hàng.</li>
     *   <li>Chuyển kho vi phạm Safety Stock Rule.</li>
     *   <li>Mã giảm giá không hợp lệ / hết hạn / hết lượt.</li>
     *   <li>Đơn hàng không đủ điều kiện thay đổi trạng thái.</li>
     * </ul>
     *
     * @return HTTP 400 Bad Request kèm mô tả lỗi nghiệp vụ cụ thể.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex,
            WebRequest request) {

        log.warn("[GlobalExHandler] 400 IllegalArgument: {}", ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    // =========================================================================
    // HTTP 500 — Lỗi hệ thống không lường trước
    // =========================================================================

    /**
     * Bắt mọi {@link Exception} chưa được xử lý ở tầng Controller/Service.
     * <p>
     * Đây là tuyến phòng thủ cuối cùng — ngăn việc Spring trả về trang
     * lỗi mặc định (HTML/stack trace) cho Frontend thay vì JSON.
     * <p>
     * ⚠️ Log ở mức {@code ERROR} (khác với 404/400 log ở mức {@code WARN})
     * để dễ phát hiện trong môi trường production.
     * <p>
     * Không trả về {@code ex.getMessage()} trực tiếp cho client vì có thể
     * chứa thông tin nhạy cảm (SQL, stack trace...).
     *
     * @return HTTP 500 Internal Server Error kèm thông báo thân thiện.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex,
            WebRequest request) {

        log.error("[GlobalExHandler] 500 Unexpected error — URI={}, class={}, message={}",
                request.getDescription(false),
                ex.getClass().getSimpleName(),
                ex.getMessage(), ex);

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "Hệ thống gặp sự cố. Chúng tôi đang xử lý, vui lòng thử lại sau.",
                request
        );
    }

    // =========================================================================
    // PRIVATE HELPER — Xây dựng cấu trúc JSON thống nhất
    // =========================================================================

    /**
     * Tạo {@link ResponseEntity} với body JSON thống nhất cho mọi loại lỗi.
     *
     * <h3>Cấu trúc JSON:</h3>
     * <pre>{@code
     * {
     *   "status":    <HTTP status code>,
     *   "error":     <HTTP status reason phrase>,
     *   "message":   <Thông điệp lỗi cụ thể cho người dùng>,
     *   "path":      <URI của request gây ra lỗi>,
     *   "timestamp": <Thời điểm lỗi xảy ra — ISO 8601>
     * }
     * }</pre>
     *
     * @param status   HTTP status sẽ trả về.
     * @param error    Tên ngắn của lỗi (vd: "Not Found", "Bad Request").
     * @param message  Thông điệp chi tiết cho người dùng.
     * @param request  WebRequest — dùng để lấy URI của request gây lỗi.
     * @return ResponseEntity đã được đóng gói hoàn chỉnh.
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status,
            String     error,
            String     message,
            WebRequest request) {

        // Lấy URI sạch (bỏ "uri=" prefix mà WebRequest.getDescription trả về)
        String path = request.getDescription(false).replace("uri=", "");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status",    status.value());
        body.put("error",     error);
        body.put("message",   message);
        body.put("path",      path);
        body.put("timestamp", LocalDateTime.now().format(TIMESTAMP_FMT));

        return ResponseEntity.status(status).body(body);
    }
}
