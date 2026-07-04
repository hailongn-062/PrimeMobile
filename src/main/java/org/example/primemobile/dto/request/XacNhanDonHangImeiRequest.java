package org.example.primemobile.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO nhận request xác nhận đơn hàng với danh sách IMEI được chọn.
 * <p>
 * Sử dụng trong endpoint {@code POST /api/admin/don-hang/{id}/xac-nhan-imei}
 * để nhân viên gửi lên danh sách IMEI tương ứng với từng sản phẩm trong đơn hàng.
 *
 * <h3>Request Body mẫu:</h3>
 * <pre>{@code
 * {
 *   "selections": [
 *     {
 *       "chiTietDonHangId": 5,
 *       "imeiList": ["123456789012345", "987654321098765"]
 *     },
 *     {
 *       "chiTietDonHangId": 6,
 *       "imeiList": ["456789012345678"]
 *     }
 *   ]
 * }
 * }</pre>
 *
 * <p>
 * <b>Ràng buộc nghiệp vụ:</b>
 * <ul>
 *   <li>Mỗi {@code ImeiSelection} phải có số lượng IMEI trong {@code imeiList}
 *       bằng đúng số lượng sản phẩm của chi tiết đơn hàng đó.</li>
 *   <li>IMEI phải tồn tại trong hệ thống, có {@code tinhTrang = 'trong_kho'}
 *       và thuộc đúng biến thể sản phẩm.</li>
 *   <li>IMEI phải có {@code kho_id} = Kho Online.</li>
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class XacNhanDonHangImeiRequest {

    /**
     * Danh sách lựa chọn IMEI cho từng chi tiết đơn hàng.
     * Không được rỗng.
     */
    private List<ImeiSelection> selections;

    /**
     * DTO đại diện cho việc chọn IMEI cho một chi tiết đơn hàng.
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ImeiSelection {

        /**
         * ID của chi tiết đơn hàng (bảng chi_tiet_don_hang).
         * Bắt buộc, phải tồn tại và thuộc về đơn hàng cần xác nhận.
         */
        private Integer chiTietDonHangId;

        /**
         * Danh sách mã IMEI (imei1) được chọn cho chi tiết đơn hàng này.
         * Số lượng phần tử phải bằng {@code soLuong} của chi tiết đơn hàng.
         * Mỗi IMEI phải là chuỗi 15 chữ số hợp lệ.
         */
        private List<String> imeiList;
    }
}