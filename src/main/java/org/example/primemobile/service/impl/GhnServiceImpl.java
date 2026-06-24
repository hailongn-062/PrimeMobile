package org.example.primemobile.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.service.IGhnService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Triển khai tích hợp API Giao Hàng Nhanh (GHN) cho PrimeMobile.
 *
 * <h2>Tuân thủ tuyệt đối system_rules.md §6:</h2>
 * <ul>
 *   <li>Cấu hình API (Token, ShopId, v.v.) ĐỌC ĐỘNG từ {@code application.properties}
 *       qua {@code @Value} — KHÔNG hardcode bất kỳ key nào vào code Java.</li>
 *   <li>Chỉ gọi 2 endpoint Read-only: {@code /fee} và {@code /leadtime}.</li>
 *   <li>⛔ <b>NGHIÊM CẤM</b> gọi {@code /v2/shipping-order/create}.</li>
 *   <li>Mọi khối call HTTP phải được bọc {@code try-catch} — lỗi bất kỳ phải
 *       được log và trả về giá trị Fail-Safe mà KHÔNG ném ngoại lệ.</li>
 * </ul>
 *
 * <h2>Giá trị Fail-Safe mặc định:</h2>
 * <ul>
 *   <li>Phí ship: {@code 0} VNĐ (không tính phí, ưu tiên trải nghiệm khách hàng).</li>
 *   <li>Ngày giao dự kiến: {@code LocalDate.now() + 3 ngày}.</li>
 * </ul>
 *
 * <h2>Chuẩn HTTP Header GHN (bắt buộc):</h2>
 * <pre>
 *   Token:   {@code <ghn.api.token>}
 *   ShopId:  {@code <ghn.api.shop-id>}
 *   Content-Type: application/json
 * </pre>
 */
@Slf4j
@Service
public class GhnServiceImpl implements IGhnService {

    // ───────────────────────────────────────────────────────────────────────
    // CẤU HÌNH GHN — Đọc động từ application.properties (system_rules.md §6)
    // ───────────────────────────────────────────────────────────────────────

    /** Base URL của GHN API Production. */
    @Value("${ghn.api.url}")
    private String ghnApiUrl;

    /** Token xác thực GHN — PHẢI đọc từ properties, KHÔNG được hardcode. */
    @Value("${ghn.api.token}")
    private String ghnToken;

    /** Shop ID trên hệ thống GHN của cửa hàng PrimeMobile. */
    @Value("${ghn.api.shop-id}")
    private String ghnShopId;

    /** District ID của kho xuất hàng (kho shop PrimeMobile). */
    @Value("${ghn.api.from-district-id}")
    private Integer ghnFromDistrictId;

    // ───────────────────────────────────────────────────────────────────────
    // HẰNG SỐ NGHIỆP VỤ (fix cứng theo bài toán bán điện thoại)
    // ───────────────────────────────────────────────────────────────────────

    /** Cân nặng mặc định của 1 đơn điện thoại (gram). */
    private static final int WEIGHT_GRAM = 500;

    /** Service ID GHN — 53320: Chuyển phát chuẩn (J-Standard). */
    private static final int SERVICE_ID_STANDARD = 53320;

    /** Số ngày fallback khi GHN API lỗi (system_rules.md §6). */
    private static final int FALLBACK_LEADTIME_DAYS = 3;

    // ───────────────────────────────────────────────────────────────────────
    // DEPENDENCIES
    // ───────────────────────────────────────────────────────────────────────

    private final RestTemplate restTemplate;

    /**
     * Constructor injection RestTemplate.
     * RestTemplate bean được khai báo tại {@code @Bean} trong lớp config hoặc
     * dùng {@code new RestTemplate()} nếu chưa có bean.
     */
    public GhnServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // =========================================================================
    // PUBLIC METHOD 1: Tính phí vận chuyển
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Request body gửi GHN:</h3>
     * <pre>{@code
     * POST /v2/shipping-order/fee
     * Header: Token = <ghn.api.token>, ShopId = <ghn.api.shop-id>
     * Body: {
     *   "from_district_id": <ghn.api.from-district-id>,
     *   "to_district_id":   <toDistrictId>,
     *   "to_ward_code":     "<toWardCode>",
     *   "weight":           500,
     *   "service_id":       53320
     * }
     * }</pre>
     *
     * <h3>Response GHN thành công:</h3>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "data": { "total": 25000, ... }
     * }
     * }</pre>
     *
     * @return Phí ship (VNĐ). Trả về {@code BigDecimal.ZERO} nếu GHN trả lỗi, timeout,
     *         hoặc response không đúng cấu trúc.
     */
    @Override
    public BigDecimal tinhPhiShip(Integer toDistrictId, String toWardCode) {
        String url = ghnApiUrl + "/v2/shipping-order/fee";

        log.info("[GhnService] Tính phí ship — toDistrictId={}, toWardCode={}", toDistrictId, toWardCode);

        try {
            // --- Build request body ---
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from_district_id", ghnFromDistrictId);
            body.put("to_district_id",   toDistrictId);
            body.put("to_ward_code",     toWardCode);
            body.put("weight",           WEIGHT_GRAM);
            body.put("service_id",       SERVICE_ID_STANDARD);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildGhnHeaders());

            // --- Gọi GHN API ---
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            // --- Parse response ---
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");

                if (data instanceof Map<?, ?> dataMap) {
                    Object total = dataMap.get("total");
                    if (total instanceof Number number) {
                        BigDecimal phiShip = BigDecimal.valueOf(number.longValue());
                        log.info("[GhnService] Phí ship nhận được từ GHN: {} VNĐ", phiShip);
                        return phiShip;
                    }
                }
                log.warn("[GhnService] Response GHN không có trường 'data.total'. Response: {}", responseBody);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi tính phí ship.", response.getStatusCode());
            }

        } catch (Exception e) {
            // ⚠️ FAIL-SAFE RULE §6: Bắt mọi lỗi, KHÔNG để nổi lên làm sập luồng đặt hàng
            log.error("[GhnService] ❌ Lỗi khi gọi API tính phí ship GHN ({}). " +
                      "Fallback về 0 VNĐ. Chi tiết: {}", url, e.getMessage());
        }

        // Fallback an toàn: 0 VNĐ
        log.info("[GhnService] Fallback phí ship = 0 VNĐ.");
        return BigDecimal.ZERO;
    }

    // =========================================================================
    // PUBLIC METHOD 2: Dự kiến ngày giao hàng
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Request body gửi GHN:</h3>
     * <pre>{@code
     * POST /v2/shipping-order/leadtime
     * Header: Token = <ghn.api.token>, ShopId = <ghn.api.shop-id>
     * Body: {
     *   "from_district_id": <ghn.api.from-district-id>,
     *   "to_district_id":   <toDistrictId>,
     *   "to_ward_code":     "<toWardCode>"
     * }
     * }</pre>
     *
     * <h3>Response GHN thành công:</h3>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "data": { "leadtime": 1718000000 }   ← Unix timestamp (giây)
     * }
     * }</pre>
     *
     * @return Ngày giao dự kiến. Trả về {@code LocalDate.now().plusDays(3)} nếu GHN lỗi.
     */
    @Override
    public LocalDate duKienNgayGiao(Integer toDistrictId, String toWardCode) {
        String url = ghnApiUrl + "/v2/shipping-order/leadtime";

        log.info("[GhnService] Tính leadtime — toDistrictId={}, toWardCode={}", toDistrictId, toWardCode);

        try {
            // --- Build request body ---
            Map<String, Object> body = new HashMap<>();
            body.put("from_district_id", ghnFromDistrictId);
            body.put("to_district_id",   toDistrictId);
            body.put("to_ward_code",     toWardCode);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildGhnHeaders());

            // --- Gọi GHN API ---
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            // --- Parse response ---
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");

                if (data instanceof Map<?, ?> dataMap) {
                    Object leadtime = dataMap.get("leadtime");
                    if (leadtime instanceof Number number) {
                        // GHN trả về Unix timestamp dạng giây → convert sang LocalDate
                        LocalDate ngayGiao = Instant.ofEpochSecond(number.longValue())
                                .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
                                .toLocalDate();
                        log.info("[GhnService] Ngày giao dự kiến từ GHN: {}", ngayGiao);
                        return ngayGiao;
                    }
                }
                log.warn("[GhnService] Response GHN không có trường 'data.leadtime'. Response: {}", responseBody);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi lấy leadtime.", response.getStatusCode());
            }

        } catch (Exception e) {
            // ⚠️ FAIL-SAFE RULE §6: Bắt mọi lỗi, KHÔNG để nổi lên làm sập luồng đặt hàng
            log.error("[GhnService] ❌ Lỗi khi gọi API leadtime GHN ({}). " +
                      "Fallback về now + {} ngày. Chi tiết: {}", url, FALLBACK_LEADTIME_DAYS, e.getMessage());
        }

        // Fallback an toàn: hôm nay + 3 ngày
        LocalDate fallback = LocalDate.now().plusDays(FALLBACK_LEADTIME_DAYS);
        log.info("[GhnService] Fallback ngày giao dự kiến = {}", fallback);
        return fallback;
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    /**
     * Xây dựng HTTP Headers chuẩn GHN (bắt buộc theo tài liệu GHN API).
     * <p>
     * Header phải có:
     * <ul>
     *   <li>{@code Token}        — API Key xác thực.</li>
     *   <li>{@code ShopId}       — ID shop trên GHN.</li>
     *   <li>{@code Content-Type} — application/json.</li>
     * </ul>
     *
     * @return HttpHeaders đã được gán Token, ShopId và Content-Type.
     */
    private HttpHeaders buildGhnHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Token",  ghnToken);   // GHN dùng "Token" (không phải "Authorization: Bearer")
        headers.set("ShopId", ghnShopId);
        return headers;
    }
}
