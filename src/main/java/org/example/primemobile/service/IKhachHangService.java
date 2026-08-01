package org.example.primemobile.service;

import org.example.primemobile.dto.KhachHangRowDto;
import org.example.primemobile.entity.KhachHang;

import java.util.List;

/**
 * Contract cho phân hệ Quản lý Khách hàng — dành cho Admin tra cứu và cập nhật.
 * <p>
 * Lưu ý (system_rules.md §4):
 * <ul>
 *   <li>Service này chỉ cho phép Admin tra cứu và cập nhật thông tin hồ sơ cơ bản.</li>
 *   <li>Không cho phép xóa vật lý khách hàng để bảo toàn lịch sử đơn hàng.</li>
 * </ul>
 */
public interface IKhachHangService {

    /**
     * Tìm kiếm khách hàng theo từ khóa (SĐT hoặc email).
     * <p>
     * Tìm LIKE trên cả 2 trường — Admin gõ bất kỳ từ nào cũng match.
     * Trả về danh sách (không phân trang) vì số lượng kết quả thường nhỏ.
     *
     * @param tuKhoa Từ khóa tìm kiếm (SĐT hoặc email). Null/rỗng = trả về tất cả.
     * @return Danh sách {@link KhachHang} phù hợp.
     */
    List<KhachHang> timKiem(String tuKhoa);

    /**
     * Lấy thông tin chi tiết một khách hàng theo ID.
     *
     * @param id ID khách hàng.
     * @return {@link KhachHang} tương ứng.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy.
     */
    KhachHang layTheoId(Integer id);

    /**
     * Cập nhật thông tin hồ sơ khách hàng.
     * <p>
     * Các trường được phép cập nhật: {@code hoTen}, {@code email},
     * {@code soDienThoai}, {@code gioiTinh}, {@code ngaySinh}.
     *
     * @param id         ID khách hàng cần cập nhật.
     * @param khachHang  Dữ liệu mới.
     * @return {@link KhachHang} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy.
     * @throws IllegalArgumentException nếu email hoặc SĐT mới trùng với khách khác.
     */
    KhachHang capNhat(Integer id, KhachHang khachHang);

    /**
     * Tạo nhanh khách vãng lai (guest) — không cần tài khoản {@code NguoiDung}.
     * <p>
     * Nếu SĐT đã tồn tại trong hệ thống → trả về khách hàng cũ thay vì tạo mới
     * (tránh duplicate). Điều này giúp nhân viên không cần nhớ khách đã từng mua.
     *
     * @param khachHang Dữ liệu khách vãng lai (bắt buộc: {@code hoTen}, {@code soDienThoai}).
     * @return {@link KhachHang} mới tạo hoặc đã tồn tại.
     * @throws IllegalArgumentException nếu thiếu {@code hoTen} hoặc {@code soDienThoai}.
     */
    KhachHang taoKhachVangLai(KhachHang khachHang);

    /**
     * Lấy danh sách khách hàng kèm thống kê tổng hợp (số đơn, tổng chi tiêu,
     * trạng thái tài khoản, ngày tham gia).
     * <p>
     * Hỗ trợ tìm kiếm theo từ khóa và lọc theo trạng thái tài khoản.
     *
     * @param tuKhoa    Từ khóa (họ tên / sĐT / email). Null = tất cả.
     * @param trangThai "hoat_dong" | "khoa" | "vang_lai" | null (tất cả).
     * @return Danh sách {@link KhachHangRowDto}.
     */
    List<KhachHangRowDto> timKiemVoiThongKe(String tuKhoa, String trangThai);

    /**
     * Đổi trạng thái tài khoản của khách hàng (khóa ↔ hoạt động).
     * <p>
     * Chỉ áp dụng được với khách hàng đã có tài khoản (nguoi_dung_id != null).
     *
     * @param khachHangId ID khách hàng (không phải ID nguoi_dung).
     * @throws IllegalStateException nếu khách là khách vãng lai (chưa có tài khoản).
     */
    void doiTrangThaiTaiKhoan(Integer khachHangId);
}
