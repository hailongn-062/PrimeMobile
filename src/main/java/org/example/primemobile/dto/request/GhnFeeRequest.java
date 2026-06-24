package org.example.primemobile.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đầu vào cho chức năng Tính phí vận chuyển GHN.
 * <p>
 * Ánh xạ tới endpoint GHN: {@code /v2/shipping-order/fee}
 * <p>
 * Các tham số cố định (hardcode tại Service):
 * <ul>
 *   <li>{@code from_district_id} — Lấy từ {@code ghn.api.from-district-id} (application.properties).</li>
 *   <li>{@code weight}           — Fix cứng 500 gram (bán điện thoại, system_rules.md §6).</li>
 *   <li>{@code service_id}       — Fix cứng 53320 (Chuyển phát chuẩn GHN).</li>
 * </ul>
 * Frontend chỉ cần gửi 2 tham số: {@code toDistrictId} và {@code toWardCode}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnFeeRequest {

    /**
     * ID quận/huyện của người nhận hàng.
     * Lấy từ GHN API danh sách quận/huyện.
     */
    @JsonProperty("to_district_id")
    private Integer toDistrictId;

    /**
     * Mã phường/xã của người nhận hàng.
     * Lấy từ GHN API danh sách phường/xã theo quận/huyện.
     */
    @JsonProperty("to_ward_code")
    private String toWardCode;
}
