package org.example.primemobile.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Interface định nghĩa hợp đồng tích hợp API Giao Hàng Nhanh (GHN).
 *
 * <h2>Tuân thủ nghiêm ngặt system_rules.md §6:</h2>
 * <ul>
 *   <li>Chỉ được dùng 2 endpoint Read-only: {@code /fee} và {@code /leadtime}.</li>
 *   <li><b>⛔ CẤM HOÀN TOÀN:</b> Không gọi {@code /v2/shipping-order/create}.</li>
 *   <li>Mọi lỗi HTTP/Timeout đều phải được bắt và trả về giá trị an toàn mặc định,
 *       tuyệt đối không để ngoại lệ nổi lên làm sập luồng đặt hàng.</li>
 * </ul>
 *
 * <h2>Giá trị mặc định (Fail-Safe) khi GHN API lỗi:</h2>
 * <ul>
 *   <li>Phí ship: {@code BigDecimal.ZERO} (0 VNĐ).</li>
 *   <li>Ngày giao dự kiến: {@code LocalDate.now().plusDays(3)}.</li>
 * </ul>
 *
 * @see org.example.primemobile.service.impl.GhnServiceImpl
 */
public interface IGhnService {

    /**
     * Tính phí vận chuyển từ kho shop đến địa chỉ khách hàng.
     * <p>
     * Gọi endpoint GHN: {@code GET /v2/shipping-order/fee}
     * <p>
     * Tham số cố định (inject từ application.properties / hardcode theo §6):
     * <ul>
     *   <li>{@code from_district_id} — district của shop (từ config).</li>
     *   <li>{@code weight}           — 500 gram (điện thoại, fix cứng).</li>
     *   <li>{@code service_id}       — 53320 (Chuyển phát chuẩn GHN).</li>
     * </ul>
     *
     * @param toDistrictId ID quận/huyện người nhận (từ GHN district API).
     * @param toWardCode   Mã phường/xã người nhận (từ GHN ward API).
     * @return Phí vận chuyển (VNĐ). Trả về {@code BigDecimal.ZERO} nếu GHN API lỗi.
     */
    BigDecimal tinhPhiShip(Integer toDistrictId, String toWardCode);

    /**
     * Dự kiến ngày giao hàng từ kho shop đến địa chỉ khách hàng.
     * <p>
     * Gọi endpoint GHN: {@code GET /v2/shipping-order/leadtime}
     * <p>
     * Tham số cố định:
     * <ul>
     *   <li>{@code from_district_id} — district của shop (từ config).</li>
     * </ul>
     *
     * @param toDistrictId ID quận/huyện người nhận.
     * @param toWardCode   Mã phường/xã người nhận.
     * @return Ngày giao dự kiến. Trả về {@code LocalDate.now().plusDays(3)} nếu GHN API lỗi.
     */
    LocalDate duKienNgayGiao(Integer toDistrictId, String toWardCode);

    // =========================================================================
    // API MASTER DATA — Lấy danh sách tỉnh/thành, quận/huyện, phường/xã
    // Các endpoint này là Read-only, an toàn, không tạo đơn hàng.
    // =========================================================================

    /**
     * Lấy danh sách tỉnh/thành phố từ GHN.
     * <p>
     * Gọi endpoint: {@code GET /v2/master-data/province}
     * <p>
     * Dùng để hiển thị dropdown chọn tỉnh/thành trên form địa chỉ.
     *
     * @return Danh sách các tỉnh/thành, mỗi phần tử là Map chứa các key: "ProvinceID", "ProvinceName", "CountryID", ...
     *         Trả về danh sách rỗng nếu GHN API lỗi.
     */
    List<Map<String, Object>> getProvinces();

    /**
     * Lấy danh sách quận/huyện theo tỉnh/thành từ GHN.
     * <p>
     * Gọi endpoint: {@code GET /v2/master-data/district} với tham số province_id.
     *
     * @param provinceId ID của tỉnh/thành (lấy từ {@link #getProvinces()}).
     * @return Danh sách các quận/huyện thuộc tỉnh đó, mỗi phần tử là Map chứa các key:
     *         "DistrictID", "DistrictName", "ProvinceID", ...
     *         Trả về danh sách rỗng nếu GHN API lỗi hoặc không có dữ liệu.
     */
    List<Map<String, Object>> getDistricts(Integer provinceId);

    /**
     * Lấy danh sách phường/xã theo quận/huyện từ GHN.
     * <p>
     * Gọi endpoint: {@code GET /v2/master-data/ward} với tham số district_id.
     *
     * @param districtId ID của quận/huyện (lấy từ {@link #getDistricts(Integer)}).
     * @return Danh sách các phường/xã thuộc quận đó, mỗi phần tử là Map chứa các key:
     *         "WardCode", "WardName", "DistrictID", ...
     *         Trả về danh sách rỗng nếu GHN API lỗi hoặc không có dữ liệu.
     */
    List<Map<String, Object>> getWards(Integer districtId);
}