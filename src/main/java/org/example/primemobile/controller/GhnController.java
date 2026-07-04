package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.service.IGhnService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller tích hợp API Giao Hàng Nhanh (GHN) — phục vụ Frontend Đặt hàng Online.
 * <p>
 * Base path: {@code /api/public/giao-hang-nhanh} — <b>Public endpoint</b>, không yêu cầu
 * đăng nhập. Được Frontend gọi trực tiếp khi khách hàng điền địa chỉ nhận hàng.
 *
 * <h3>Tuân thủ tuyệt đối system_rules.md §6:</h3>
 * <ul>
 *   <li>Chỉ proxy 2 chức năng Read-only: tính phí ship và dự kiến ngày giao.</li>
 *   <li>Controller KHÔNG xử lý logic GHN trực tiếp — toàn bộ ủy quyền cho {@link IGhnService}.</li>
 *   <li>Controller luôn trả về HTTP 200 ngay cả khi GHN API lỗi
 *       (Service đã fallback an toàn theo §6 — không để sập luồng đặt hàng).</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   GET /api/public/giao-hang-nhanh/phi-ship
 *       ?toDistrictId={id}&toWardCode={code}
 *       → Trả về phí vận chuyển (VNĐ)
 *
 *   GET /api/public/giao-hang-nhanh/thoi-gian-giao
 *       ?toDistrictId={id}&toWardCode={code}
 *       → Trả về ngày giao dự kiến (ISO date)
 *
 *   GET /api/public/giao-hang-nhanh/provinces
 *       → Lấy danh sách tỉnh/thành phố
 *
 *   GET /api/public/giao-hang-nhanh/districts?provinceId={id}
 *       → Lấy danh sách quận/huyện theo tỉnh
 *
 *   GET /api/public/giao-hang-nhanh/wards?districtId={id}
 *       → Lấy danh sách phường/xã theo quận
 * </pre>
 */
@RestController
@RequestMapping("/api/public/giao-hang-nhanh")
@RequiredArgsConstructor
public class GhnController {

    private static final Logger log = LoggerFactory.getLogger(GhnController.class);

    private final IGhnService ghnService;

    // =========================================================================
    // GET /api/public/giao-hang-nhanh/phi-ship
    // =========================================================================

    /**
     * Tính phí vận chuyển từ kho PrimeMobile đến địa chỉ người nhận.
     * <p>
     * Frontend gọi endpoint này mỗi khi khách hàng hoàn tất nhập địa chỉ
     * để hiển thị phí ship trước khi khách ấn "Đặt hàng".
     *
     * <h3>Ví dụ request:</h3>
     * <pre>
     *   GET /api/public/giao-hang-nhanh/phi-ship?toDistrictId=1442&toWardCode=21012
     * </pre>
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * {
     *   "success":       true,
     *   "phiShip":       25000,
     *   "donVi":         "VNĐ",
     *   "nguonDuLieu":   "GHN API",
     *   "toDistrictId":  1442,
     *   "toWardCode":    "21012"
     * }
     * }</pre>
     *
     * <h3>Response khi GHN lỗi (Fail-Safe §6):</h3>
     * <pre>{@code
     * {
     *   "success":       true,
     *   "phiShip":       0,
     *   "donVi":         "VNĐ",
     *   "nguonDuLieu":   "Fallback (GHN không phản hồi)",
     *   ...
     * }
     * }</pre>
     *
     * @param toDistrictId ID quận/huyện người nhận (lấy từ GHN district API).
     * @param toWardCode   Mã phường/xã người nhận (lấy từ GHN ward API).
     * @return Luôn HTTP 200. Phí ship = 0 nếu GHN lỗi (Fail-Safe §6).
     */
    @GetMapping("/phi-ship")
    public ResponseEntity<?> tinhPhiShip(
            @RequestParam Integer toDistrictId,
            @RequestParam String  toWardCode) {

        log.info("[GhnController] Tính phí ship — toDistrictId={}, toWardCode={}",
                toDistrictId, toWardCode);

        BigDecimal phiShip = ghnService.tinhPhiShip(toDistrictId, toWardCode);

        boolean laFallback = phiShip.compareTo(BigDecimal.ZERO) == 0;

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",     true);
        response.put("phiShip",     phiShip);
        response.put("donVi",       "VNĐ");
        response.put("nguonDuLieu", laFallback ? "Fallback (GHN không phản hồi)" : "GHN API");
        response.put("toDistrictId", toDistrictId);
        response.put("toWardCode",   toWardCode);

        log.info("[GhnController] Trả phí ship = {} VNĐ (fallback={})", phiShip, laFallback);
        // Luôn HTTP 200 — Frontend không bị sập dù GHN lỗi
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/public/giao-hang-nhanh/thoi-gian-giao
    // =========================================================================

    /**
     * Dự kiến ngày giao hàng đến địa chỉ người nhận.
     * <p>
     * Frontend gọi endpoint này để hiển thị "Dự kiến giao hàng: Thứ X, ngày X/X/XXXX"
     * trong trang checkout, giúp khách hàng quyết định đặt hàng.
     *
     * <h3>Ví dụ request:</h3>
     * <pre>
     *   GET /api/public/giao-hang-nhanh/thoi-gian-giao?toDistrictId=1442&toWardCode=21012
     * </pre>
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * {
     *   "success":          true,
     *   "ngayGiaoDuKien":  "2024-06-25",
     *   "nguonDuLieu":      "GHN API",
     *   "toDistrictId":     1442,
     *   "toWardCode":       "21012"
     * }
     * }</pre>
     *
     * <h3>Response khi GHN lỗi (Fail-Safe §6):</h3>
     * Ngày giao = {@code LocalDate.now() + 3 ngày}.
     *
     * @param toDistrictId ID quận/huyện người nhận.
     * @param toWardCode   Mã phường/xã người nhận.
     * @return Luôn HTTP 200. Ngày giao = today+3 nếu GHN lỗi (Fail-Safe §6).
     */
    @GetMapping("/thoi-gian-giao")
    public ResponseEntity<?> duKienNgayGiao(
            @RequestParam Integer toDistrictId,
            @RequestParam String  toWardCode) {

        log.info("[GhnController] Dự kiến ngày giao — toDistrictId={}, toWardCode={}",
                toDistrictId, toWardCode);

        LocalDate ngayGiao = ghnService.duKienNgayGiao(toDistrictId, toWardCode);

        // Phát hiện fallback: nếu ngày giao = today+3 (có thể false positive nhưng chấp nhận được)
        boolean laFallback = ngayGiao.isEqual(LocalDate.now().plusDays(3));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success",         true);
        response.put("ngayGiaoDuKien",  ngayGiao.toString());   // Format ISO: "YYYY-MM-DD"
        response.put("nguonDuLieu",     laFallback ? "Fallback (GHN không phản hồi)" : "GHN API");
        response.put("toDistrictId",    toDistrictId);
        response.put("toWardCode",      toWardCode);

        log.info("[GhnController] Trả ngày giao = {} (fallback={})", ngayGiao, laFallback);
        // Luôn HTTP 200 — Frontend không bị sập dù GHN lỗi
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // MASTER DATA ENDPOINTS — Lấy danh sách tỉnh/thành, quận/huyện, phường/xã
    // Các endpoint này là Read-only, an toàn, không tạo đơn hàng.
    // =========================================================================

    /**
     * Lấy danh sách tỉnh/thành phố từ GHN.
     * <p>
     * Frontend gọi endpoint này khi load trang thanh toán để đổ dữ liệu
     * vào dropdown chọn tỉnh/thành.
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": [
     *     { "ProvinceID": 201, "ProvinceName": "Hà Nội", "CountryID": 1 },
     *     { "ProvinceID": 202, "ProvinceName": "TP. Hồ Chí Minh", "CountryID": 1 },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @return Luôn HTTP 200. Trả về danh sách rỗng nếu GHN lỗi.
     */
    @GetMapping("/provinces")
    public ResponseEntity<?> getProvinces() {
        log.info("[GhnController] Lấy danh sách tỉnh/thành");

        List<Map<String, Object>> data = ghnService.getProvinces();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách quận/huyện theo tỉnh/thành từ GHN.
     * <p>
     * Frontend gọi endpoint này khi khách hàng chọn một tỉnh/thành
     * để đổ dữ liệu vào dropdown chọn quận/huyện.
     *
     * <h3>Ví dụ request:</h3>
     * <pre>
     *   GET /api/public/giao-hang-nhanh/districts?provinceId=201
     * </pre>
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": [
     *     { "DistrictID": 1482, "DistrictName": "Quận Cầu Giấy", "ProvinceID": 201 },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @param provinceId ID của tỉnh/thành (lấy từ {@code /provinces}).
     * @return Luôn HTTP 200. Trả về danh sách rỗng nếu GHN lỗi hoặc provinceId null.
     */
    @GetMapping("/districts")
    public ResponseEntity<?> getDistricts(@RequestParam(required = false) Integer provinceId) {
        log.info("[GhnController] Lấy danh sách quận/huyện theo tỉnh — provinceId={}", provinceId);

        List<Map<String, Object>> data = ghnService.getDistricts(provinceId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy danh sách phường/xã theo quận/huyện từ GHN.
     * <p>
     * Frontend gọi endpoint này khi khách hàng chọn một quận/huyện
     * để đổ dữ liệu vào dropdown chọn phường/xã.
     *
     * <h3>Ví dụ request:</h3>
     * <pre>
     *   GET /api/public/giao-hang-nhanh/wards?districtId=1482
     * </pre>
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * {
     *   "success": true,
     *   "data": [
     *     { "WardCode": "1A03", "WardName": "Phường Dịch Vọng", "DistrictID": 1482 },
     *     ...
     *   ]
     * }
     * }</pre>
     *
     * @param districtId ID của quận/huyện (lấy từ {@code /districts}).
     * @return Luôn HTTP 200. Trả về danh sách rỗng nếu GHN lỗi hoặc districtId null.
     */
    @GetMapping("/wards")
    public ResponseEntity<?> getWards(@RequestParam(required = false) Integer districtId) {
        log.info("[GhnController] Lấy danh sách phường/xã theo quận — districtId={}", districtId);

        List<Map<String, Object>> data = ghnService.getWards(districtId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}