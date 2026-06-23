package org.example.primemobile.service;

import org.example.primemobile.entity.HangSanXuat;

import java.util.List;

/**
 * Contract cho phân hệ Quản lý Hãng sản xuất (thương hiệu điện thoại).
 * <p>
 * Quản lý các thương hiệu như Apple, Samsung, Xiaomi, Oppo...
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Tên hãng phải duy nhất (không phân biệt hoa thường).</li>
 *   <li>Không có xóa vật lý — nếu cần loại bỏ hãng, sẽ xử lý thông qua
 *       logic nghiệp vụ cấp cao hơn (ví dụ: kiểm tra sản phẩm liên kết).</li>
 * </ul>
 */
public interface IHangSanXuatService {

    /**
     * Lấy danh sách tất cả hãng sản xuất.
     *
     * @return Danh sách {@link HangSanXuat}, có thể rỗng.
     */
    List<HangSanXuat> layTatCa();

    /**
     * Lấy thông tin một hãng sản xuất theo ID.
     *
     * @param id ID hãng sản xuất.
     * @return {@link HangSanXuat} tương ứng.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    HangSanXuat layTheoId(Integer id);

    /**
     * Thêm hãng sản xuất mới vào hệ thống.
     *
     * @param hangSanXuat Đối tượng hãng cần thêm (chưa có ID).
     * @return {@link HangSanXuat} đã được lưu với ID được gán.
     * @throws IllegalArgumentException nếu {@code tenHang} đã tồn tại.
     */
    HangSanXuat them(HangSanXuat hangSanXuat);

    /**
     * Cập nhật thông tin hãng sản xuất.
     *
     * @param id          ID của hãng cần cập nhật.
     * @param hangSanXuat Dữ liệu mới cần ghi đè.
     * @return {@link HangSanXuat} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException                    nếu tên mới trùng với hãng khác.
     */
    HangSanXuat capNhat(Integer id, HangSanXuat hangSanXuat);

    /**
     * Xóa hãng sản xuất khỏi hệ thống.
     * <p>
     * <b>Cảnh báo:</b> Chỉ xóa được khi hãng chưa có sản phẩm liên kết.
     * Nếu đang có SanPham tham chiếu đến hãng này, DB sẽ ném lỗi constraint.
     *
     * @param id ID hãng cần xóa.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    void xoa(Integer id);
}
