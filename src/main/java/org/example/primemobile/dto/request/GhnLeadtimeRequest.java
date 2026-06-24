package org.example.primemobile.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO đầu vào cho chức năng Dự kiến ngày giao hàng GHN.
 * <p>
 * Ánh xạ tới endpoint GHN: {@code /v2/shipping-order/leadtime}
 * <p>
 * Tham số cố định (hardcode tại Service):
 * <ul>
 *   <li>{@code from_district_id} — Lấy từ {@code ghn.api.from-district-id} (application.properties).</li>
 * </ul>
 * Frontend chỉ cần gửi 2 tham số: {@code toDistrictId} và {@code toWardCode}.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GhnLeadtimeRequest {

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
