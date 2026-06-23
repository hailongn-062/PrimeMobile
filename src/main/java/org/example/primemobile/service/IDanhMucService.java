package org.example.primemobile.service;

import org.example.primemobile.entity.DanhMuc;

import java.util.List;

/**
 * Contract cho phân hệ Quản lý Danh mục sản phẩm.
 * <p>
 * Danh mục phân loại các dòng máy điện thoại
 * (ví dụ: "iPhone", "Android Cao Cấp", "Mid-range"...).
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Tên danh mục phải duy nhất (không phân biệt hoa thường).</li>
 *   <li>Slug được tự động sinh từ {@code tenDanhMuc} khi thêm hoặc sửa.</li>
 *   <li>Xóa mềm: cập nhật {@code kichHoat = false} thay vì DELETE vật lý,
 *       bảo toàn toàn vẹn dữ liệu quan hệ với SanPham.</li>
 * </ul>
 */
public interface IDanhMucService {

    /**
     * Lấy danh sách tất cả danh mục (kể cả đã vô hiệu hóa).
     *
     * @return Danh sách {@link DanhMuc}, có thể rỗng.
     */
    List<DanhMuc> layTatCa();

    /**
     * Lấy danh sách danh mục đang kích hoạt ({@code kichHoat = true}).
     *
     * @return Danh sách {@link DanhMuc} đang hoạt động.
     */
    List<DanhMuc> layDanhSachKichHoat();

    /**
     * Lấy thông tin một danh mục theo ID.
     *
     * @param id ID danh mục.
     * @return {@link DanhMuc} tương ứng.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    DanhMuc layTheoId(Integer id);

    /**
     * Thêm danh mục mới vào hệ thống.
     * <p>
     * Slug được tự động sinh từ {@code tenDanhMuc} (chuyển về dạng URL-friendly).
     *
     * @param danhMuc Đối tượng danh mục cần thêm (chưa có ID).
     * @return {@link DanhMuc} đã được lưu với ID và slug được gán.
     * @throws IllegalArgumentException nếu {@code tenDanhMuc} đã tồn tại.
     */
    DanhMuc them(DanhMuc danhMuc);

    /**
     * Cập nhật thông tin danh mục.
     * <p>
     * Slug được tự động cập nhật lại nếu {@code tenDanhMuc} thay đổi.
     *
     * @param id      ID của danh mục cần cập nhật.
     * @param danhMuc Dữ liệu mới cần ghi đè.
     * @return {@link DanhMuc} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException                    nếu tên mới trùng với danh mục khác.
     */
    DanhMuc capNhat(Integer id, DanhMuc danhMuc);

    /**
     * Vô hiệu hóa danh mục (xóa mềm — đặt {@code kichHoat = false}).
     * <p>
     * Không xóa vật lý để bảo toàn toàn vẹn tham chiếu với bảng {@code san_pham}.
     *
     * @param id ID danh mục cần vô hiệu hóa.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    void voHieuHoa(Integer id);

    /**
     * Kích hoạt lại danh mục đã vô hiệu hóa ({@code kichHoat = true}).
     *
     * @param id ID danh mục cần kích hoạt lại.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    void kichHoatLai(Integer id);
}
