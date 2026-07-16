package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO (Data Transfer Object) cho đối tượng
 * {@link org.example.primemobile.entity.MayDienThoai}.
 *
 * <p>
 * Mục đích của DTO này là trả về thông tin IMEI của máy điện thoại trong các
 * API,
 * đặc biệt là cho màn hình Bán hàng tại quầy (POS) khi nhân viên cần chọn IMEI
 * từ danh sách có sẵn.
 * </p>
 *
 * <p>
 * Việc sử dụng DTO thay vì trả thẳng entity giúp tránh lỗi serialization do
 * Hibernate proxy (lazy loading) và chỉ expose các trường cần thiết, tăng hiệu
 * năng
 * và bảo mật.
 * </p>
 *
 * @see org.example.primemobile.entity.MayDienThoai
 * @see org.example.primemobile.controller.MayDienThoaiController
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImeiDto {

    /**
     * ID của bản ghi IMEI trong bảng may_dien_thoai.
     * Dùng để tham chiếu khi cần cập nhật trạng thái hoặc gán vào đơn hàng.
     */
    private Integer id;

    /**
     * Mã IMEI khe SIM 1 (bắt buộc, 15 chữ số).
     * Đây là định danh chính để nhận diện từng máy vật lý.
     */
    private String imei1;

    /**
     * Mã IMEI khe SIM 2 (tùy chọn, null nếu máy chỉ có 1 SIM).
     */
    private String imei2;

    /**
     * Trạng thái hiện tại của máy.
     * Các giá trị hợp lệ: 'trong_kho', 'da_ban', 'bao_hanh', 'loi_hong'
     */
    private String tinhTrang;
}