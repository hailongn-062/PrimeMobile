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
import java.util.*;

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

    /** Service ID mặc định (fallback) — Chuyển phát chuẩn. */
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
     *   "service_id":       <service_id động>
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
            // Lấy service_id động cho tuyến đường
            Integer serviceId = layServiceId(toDistrictId);
            if (serviceId == null) {
                log.warn("[GhnService] Không lấy được service_id, fallback về mặc định {}", SERVICE_ID_STANDARD);
                serviceId = SERVICE_ID_STANDARD;
            }

            // --- Build request body ---
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("from_district_id", ghnFromDistrictId);
            body.put("to_district_id",   toDistrictId);
            body.put("to_ward_code",     toWardCode);
            body.put("weight",           WEIGHT_GRAM);
            body.put("service_id",       serviceId);

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
     *   "to_ward_code":     "<toWardCode>",
     *   "service_id":       <service_id động (nếu có)>
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
            // Lấy service_id động (nếu có) để tăng độ chính xác
            Integer serviceId = layServiceId(toDistrictId);

            // --- Build request body ---
            Map<String, Object> body = new HashMap<>();
            body.put("from_district_id", ghnFromDistrictId);
            body.put("to_district_id",   toDistrictId);
            body.put("to_ward_code",     toWardCode);
            if (serviceId != null) {
                body.put("service_id", serviceId);
            }

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
    // PUBLIC METHOD 3: Lấy danh sách tỉnh/thành phố (Master Data)
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Gọi endpoint GHN:</h3>
     * <pre>{@code
     * GET /v2/master-data/province
     * Header: Token = <ghn.api.token>, ShopId = <ghn.api.shop-id>
     * }</pre>
     *
     * <h3>Response GHN thành công:</h3>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "data": [
     *     { "ProvinceID": 201, "ProvinceName": "Hà Nội", "CountryID": 1, ... },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @return Danh sách Map chứa thông tin tỉnh/thành. Trả về danh sách rỗng nếu lỗi.
     */
    @Override
    public List<Map<String, Object>> getProvinces() {
        String url = ghnApiUrl + "/master-data/province";
        log.info("[GhnService] Lấy danh sách tỉnh/thành từ GHN");

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildGhnHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");
                if (data instanceof List<?> list && !list.isEmpty()) {
                    // Ép kiểu an toàn sang List<Map<String, Object>>
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            // Chuyển đổi key từ String sang String
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                row.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            result.add(row);
                        }
                    }
                    log.info("[GhnService] Lấy thành công {} tỉnh/thành", result.size());
                    return result;
                }
                log.warn("[GhnService] Response GHN không có dữ liệu tỉnh/thành. Response: {}", responseBody);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi lấy tỉnh/thành.", response.getStatusCode());
            }
        } catch (Exception e) {
            // ⚠️ FAIL-SAFE: Không để lỗi nổi lên
            log.error("[GhnService] ❌ Lỗi khi gọi API lấy tỉnh/thành GHN ({}). Fallback về danh sách rỗng. Chi tiết: {}", url, e.getMessage());
        }

        // Fallback: danh sách rỗng
        log.info("[GhnService] Fallback: trả về danh sách tỉnh/thành rỗng.");
        return Collections.emptyList();
    }

    // =========================================================================
    // PUBLIC METHOD 4: Lấy danh sách quận/huyện theo tỉnh/thành
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Gọi endpoint GHN:</h3>
     * <pre>{@code
     * GET /v2/master-data/district?province_id={provinceId}
     * Header: Token = <ghn.api.token>, ShopId = <ghn.api.shop-id>
     * }</pre>
     *
     * <h3>Response GHN thành công:</h3>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "data": [
     *     { "DistrictID": 1482, "DistrictName": "Quận Cầu Giấy", "ProvinceID": 201, ... },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @param provinceId ID của tỉnh/thành (lấy từ {@link #getProvinces()}).
     * @return Danh sách Map chứa thông tin quận/huyện. Trả về danh sách rỗng nếu lỗi.
     */
    @Override
    public List<Map<String, Object>> getDistricts(Integer provinceId) {
        String url = ghnApiUrl + "/master-data/district?province_id=" + provinceId;
        log.info("[GhnService] Lấy danh sách quận/huyện theo tỉnh — provinceId={}", provinceId);

        if (provinceId == null) {
            log.warn("[GhnService] provinceId is null, trả về danh sách rỗng.");
            return Collections.emptyList();
        }

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildGhnHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");
                if (data instanceof List<?> list && !list.isEmpty()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                row.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            result.add(row);
                        }
                    }
                    log.info("[GhnService] Lấy thành công {} quận/huyện cho tỉnh {}", result.size(), provinceId);
                    return result;
                }
                log.warn("[GhnService] Response GHN không có dữ liệu quận/huyện cho tỉnh {}. Response: {}", provinceId, responseBody);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi lấy quận/huyện cho tỉnh {}.", response.getStatusCode(), provinceId);
            }
        } catch (Exception e) {
            // ⚠️ FAIL-SAFE: Không để lỗi nổi lên
            log.error("[GhnService] ❌ Lỗi khi gọi API lấy quận/huyện GHN ({}). Fallback về danh sách rỗng. Chi tiết: {}", url, e.getMessage());
        }

        log.info("[GhnService] Fallback: trả về danh sách quận/huyện rỗng.");
        return Collections.emptyList();
    }

    // =========================================================================
    // PUBLIC METHOD 5: Lấy danh sách phường/xã theo quận/huyện
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Gọi endpoint GHN:</h3>
     * <pre>{@code
     * GET /v2/master-data/ward?district_id={districtId}
     * Header: Token = <ghn.api.token>, ShopId = <ghn.api.shop-id>
     * }</pre>
     *
     * <h3>Response GHN thành công:</h3>
     * <pre>{@code
     * {
     *   "code": 200,
     *   "data": [
     *     { "WardCode": "1A03", "WardName": "Phường Dịch Vọng", "DistrictID": 1482, ... },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @param districtId ID của quận/huyện (lấy từ {@link #getDistricts(Integer)}).
     * @return Danh sách Map chứa thông tin phường/xã. Trả về danh sách rỗng nếu lỗi.
     */
    @Override
    public List<Map<String, Object>> getWards(Integer districtId) {
        String url = ghnApiUrl + "/master-data/ward?district_id=" + districtId;
        log.info("[GhnService] Lấy danh sách phường/xã theo quận — districtId={}", districtId);

        if (districtId == null) {
            log.warn("[GhnService] districtId is null, trả về danh sách rỗng.");
            return Collections.emptyList();
        }

        try {
            HttpEntity<Void> entity = new HttpEntity<>(buildGhnHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");
                if (data instanceof List<?> list && !list.isEmpty()) {
                    List<Map<String, Object>> result = new ArrayList<>();
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> map) {
                            Map<String, Object> row = new LinkedHashMap<>();
                            for (Map.Entry<?, ?> entry : map.entrySet()) {
                                row.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                            result.add(row);
                        }
                    }
                    log.info("[GhnService] Lấy thành công {} phường/xã cho quận {}", result.size(), districtId);
                    return result;
                }
                log.warn("[GhnService] Response GHN không có dữ liệu phường/xã cho quận {}. Response: {}", districtId, responseBody);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi lấy phường/xã cho quận {}.", response.getStatusCode(), districtId);
            }
        } catch (Exception e) {
            // ⚠️ FAIL-SAFE: Không để lỗi nổi lên
            log.error("[GhnService] ❌ Lỗi khi gọi API lấy phường/xã GHN ({}). Fallback về danh sách rỗng. Chi tiết: {}", url, e.getMessage());
        }

        log.info("[GhnService] Fallback: trả về danh sách phường/xã rỗng.");
        return Collections.emptyList();
    }

    // =========================================================================
    // PRIVATE HELPER: Lấy service_id động cho tuyến đường
    // =========================================================================

    /**
     * Gọi API GHN để lấy danh sách service khả dụng cho tuyến từ from_district đến to_district.
     *
     * @param toDistrictId ID quận/huyện đích.
     * @return service_id phù hợp (ưu tiên service có service_type_id = 2 nếu có), hoặc null nếu không tìm thấy.
     */
    private Integer layServiceId(Integer toDistrictId) {
        if (toDistrictId == null) {
            return null;
        }

        String url = ghnApiUrl + "/v2/shipping-order/available-services";
        log.debug("[GhnService] Gọi available-services để lấy service_id cho toDistrictId={}", toDistrictId);

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            // Sử dụng shop_id từ config (không hardcode)
            body.put("shop_id", Integer.parseInt(ghnShopId));
            body.put("from_district", ghnFromDistrictId);
            body.put("to_district", toDistrictId);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, buildGhnHeaders());
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<?, ?> responseBody = response.getBody();
                Object data = responseBody.get("data");
                if (data instanceof List<?> list && !list.isEmpty()) {
                    // Ưu tiên service có service_type_id = 2 (hàng nhẹ / chuyển phát nhanh)
                    // Nếu không có, lấy service đầu tiên
                    Integer preferredServiceId = null;
                    Integer firstServiceId = null;

                    for (Object item : list) {
                        if (item instanceof Map<?, ?> serviceMap) {
                            Object serviceIdObj = serviceMap.get("service_id");
                            Object serviceTypeIdObj = serviceMap.get("service_type_id");
                            if (serviceIdObj instanceof Number) {
                                Integer sid = ((Number) serviceIdObj).intValue();
                                if (firstServiceId == null) {
                                    firstServiceId = sid;
                                }
                                // Ưu tiên service_type_id = 2 (thường là chuyển phát nhanh cho hàng nhẹ)
                                if (serviceTypeIdObj instanceof Number && ((Number) serviceTypeIdObj).intValue() == 2) {
                                    preferredServiceId = sid;
                                    break;
                                }
                            }
                        }
                    }

                    Integer result = preferredServiceId != null ? preferredServiceId : firstServiceId;
                    if (result != null) {
                        log.info("[GhnService] Lấy service_id={} cho toDistrictId={}", result, toDistrictId);
                        return result;
                    }
                }
                log.warn("[GhnService] Không tìm thấy service_id cho toDistrictId={}", toDistrictId);
            } else {
                log.warn("[GhnService] GHN trả HTTP {} khi gọi available-services", response.getStatusCode());
            }
        } catch (Exception e) {
            // ⚠️ FAIL-SAFE: Không để lỗi nổi lên, trả về null để fallback
            log.error("[GhnService] ❌ Lỗi khi gọi API available-services: {}", e.getMessage());
        }

        return null;
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