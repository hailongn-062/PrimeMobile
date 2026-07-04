package org.example.primemobile.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Cấu hình và tiện ích tích hợp thanh toán VNPay.
 * <p>
 * Đọc các thông số từ file application.properties:
 * <ul>
 *   <li>vnpay.tmn-code       - Mã website/merchant trên VNPay</li>
 *   <li>vnpay.secret-key     - Khóa bí mật dùng để tạo chữ ký</li>
 *   <li>vnpay.pay-url        - URL thanh toán (sandbox hoặc production)</li>
 *   <li>vnpay.return-url     - URL khách hàng được redirect về sau khi thanh toán</li>
 *   <li>vnpay.ipn-url        - URL VNPay gọi server-to-server để xác nhận giao dịch</li>
 * </ul>
 *
 * <h3>Cách sử dụng:</h3>
 * <pre>
 *   // Tạo URL thanh toán
 *   String paymentUrl = vnPayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;
 *
 *   // Xác minh chữ ký từ VNPay IPN/Return
 *   boolean isValid = vnPayConfig.verifySignature(params, params.get("vnp_SecureHash"));
 * </pre>
 */
@Getter
@Component
public class VnPayConfig {

    // ────────────────────────────────────────────────────────────────────────
    // CẤU HÌNH TỪ application.properties
    // ────────────────────────────────────────────────────────────────────────

    /** Mã website/merchant do VNPay cấp. */
    @Value("${vnpay.tmn-code}")
    private String tmnCode;

    /** Khóa bí mật dùng để tạo chữ ký HMAC SHA512. */
    @Value("${vnpay.secret-key}")
    private String secretKey;

    /** URL thanh toán của VNPay (sandbox hoặc production). */
    @Value("${vnpay.pay-url}")
    private String payUrl;

    /** URL khách hàng được redirect về sau khi thanh toán. */
    @Value("${vnpay.return-url}")
    private String returnUrl;

    /** URL VNPay gọi để xác nhận giao dịch (IPN). */
    @Value("${vnpay.ipn-url}")
    private String ipnUrl;

    // ────────────────────────────────────────────────────────────────────────
    // PHƯƠNG THỨC TIỆN ÍCH
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Tạo chữ ký HMAC SHA512 từ dữ liệu và khóa bí mật.
     * <p>
     * Dùng để tạo {@code vnp_SecureHash} gửi sang VNPay hoặc kiểm tra chữ ký
     * từ VNPay trong IPN/Return.
     *
     * @param key  Khóa bí mật (lấy từ {@link #secretKey}).
     * @param data Chuỗi dữ liệu cần ký (thường là query string đã sắp xếp).
     * @return Chữ ký dạng hex (chữ thường, 128 ký tự).
     * @throws RuntimeException nếu có lỗi khi tạo chữ ký (thường do thuật toán không được hỗ trợ).
     */
    public String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(keySpec);
            byte[] bytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi tạo HMAC SHA512: " + e.getMessage(), e);
        }
    }

    /**
     * Xây dựng chuỗi query từ Map tham số, sắp xếp theo thứ tự alphabet.
     * <p>
     * Quy tắc của VNPay: các tham số phải được sắp xếp theo thứ tự từ điển
     * trước khi tạo chữ ký và trước khi gửi đi.
     * <p>
     * <b>Lưu ý:</b> Tất cả giá trị tham số đều được URL-encode để đảm bảo
     * chữ ký đúng với chuẩn VNPay.
     *
     * @param params Map chứa các tham số (key-value).
     * @return Chuỗi query string dạng {@code key1=value1&key2=value2&...} với
     *         các giá trị đã được URL-encode.
     */
    public String buildQueryString(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder sb = new StringBuilder();
        try {
            for (String field : fieldNames) {
                String value = params.get(field);
                if (value != null && !value.isEmpty()) {
                    if (sb.length() > 0) {
                        sb.append('&');
                    }
                    sb.append(field).append('=').append(URLEncoder.encode(value, StandardCharsets.UTF_8.name()));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi URL-encode query string: " + e.getMessage(), e);
        }
        return sb.toString();
    }

    /**
     * Kiểm tra chữ ký từ VNPay trong IPN/Return.
     * <p>
     * Loại bỏ các tham số {@code vnp_SecureHash} và {@code vnp_SecureHashType}
     * khỏi danh sách trước khi tính toán chữ ký, sau đó so sánh với giá trị
     * {@code secureHash} được gửi đến.
     *
     * @param params     Map chứa toàn bộ tham số từ VNPay (bao gồm cả secure hash).
     * @param secureHash Giá trị chữ ký được gửi từ VNPay (thường là {@code vnp_SecureHash}).
     * @return {@code true} nếu chữ ký hợp lệ, ngược lại {@code false}.
     */
    public boolean verifySignature(Map<String, String> params, String secureHash) {
        // Tạo bản sao và loại bỏ các tham số liên quan đến chữ ký
        Map<String, String> sortedParams = new TreeMap<>(params);
        sortedParams.remove("vnp_SecureHash");
        sortedParams.remove("vnp_SecureHashType");

        // Xây dựng query string từ các tham số còn lại
        String queryString = buildQueryString(sortedParams);

        // Tính chữ ký từ query string
        String myHash = hmacSHA512(secretKey, queryString);

        // So sánh (không phân biệt hoa thường)
        return myHash.equalsIgnoreCase(secureHash);
    }
}