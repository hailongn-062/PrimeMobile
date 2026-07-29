package org.example.primemobile.service;

import org.example.primemobile.entity.NguoiDung;

import java.util.List;

/**
 * Contract cho phân hệ Quản lý Nhân Viên — dành riêng cho Admin.
 * <p>
 * Ghi chú (system_rules.md §1):
 * <ul>
 *   <li>Chỉ Admin mới có quyền truy cập các chức năng trong interface này.</li>
 *   <li>Không dùng Spring Security. Phân quyền quản lý bởi HandlerInterceptor.</li>
 *   <li>Tất cả nghiệp vụ làm việc trực tiếp trên bảng {@code nguoi_dung} với điều kiện
 *       {@code vai_tro = 'NhanVien'}.</li>
 *   <li>Khi tạo mới: vaiTro BẮT BUỘC được gán cứng thành {@code "NhanVien"} ở tầng Service.</li>
 * </ul>
 */
public interface INhanVienService {

    /**
     * Lấy danh sách toàn bộ nhân viên trong hệ thống.
     * <p>
     * Query trên bảng {@code nguoi_dung} với điều kiện {@code vai_tro = 'NhanVien'},
     * sắp xếp theo {@code ngay_tao} giảm dần (mới nhất lên đầu).
     *
     * @return Danh sách {@link NguoiDung} có vai trò nhân viên.
     */
    List<NguoiDung> layDanhSachNhanVien();

    List<NguoiDung> timKiemVaLocNhanVien(String tuKhoa, String trangThai);

    /**
     * Lấy thông tin chi tiết một nhân viên theo ID.
     *
     * @param id ID nhân viên (PK bảng {@code nguoi_dung}).
     * @return {@link NguoiDung} tương ứng.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy
     *         hoặc ID thuộc về tài khoản không phải NhanVien.
     */
    NguoiDung layTheoId(Integer id);

    /**
     * Thêm mới một tài khoản nhân viên.
     * <p>
     * Nghiệp vụ bắt buộc:
     * <ul>
     *   <li>Gán cứng {@code vaiTro = "NhanVien"} — bất kể giá trị client gửi lên.</li>
     *   <li>Gán cứng {@code trangThai = "hoat_dong"} cho tài khoản mới.</li>
     *   <li>Kiểm tra trùng {@code email} trước khi lưu.</li>
     *   <li>Kiểm tra trùng {@code soDienThoai} (nếu có) trước khi lưu.</li>
     *   <li>Mật khẩu được băm BCrypt trước khi persist.</li>
     * </ul>
     *
     * @param nhanVien Dữ liệu nhân viên mới (từ request body).
     * @return {@link NguoiDung} sau khi lưu thành công.
     * @throws IllegalArgumentException nếu email hoặc SĐT đã tồn tại.
     */
    NguoiDung themMoi(NguoiDung nhanVien);

    /**
     * Cập nhật thông tin hồ sơ nhân viên.
     * <p>
     * Các trường được phép cập nhật: {@code hoTen}, {@code email}, {@code soDienThoai}.
     * Các trường KHÔNG được phép cập nhật qua endpoint này: {@code vaiTro}, {@code matKhau},
     * {@code trangThai} (dùng endpoint riêng {@link #doiTrangThai}).
     *
     * @param id       ID nhân viên cần cập nhật.
     * @param nhanVien Dữ liệu mới.
     * @return {@link NguoiDung} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy.
     * @throws IllegalArgumentException nếu email hoặc SĐT mới trùng với tài khoản khác.
     */
    NguoiDung capNhat(Integer id, NguoiDung nhanVien);

    /**
     * Đổi trạng thái tài khoản nhân viên (Khóa / Mở khóa).
     * <p>
     * Logic toggle:
     * <ul>
     *   <li>Nếu hiện tại là {@code "hoat_dong"} → chuyển sang {@code "khoa"}.</li>
     *   <li>Nếu hiện tại là {@code "khoa"} → chuyển sang {@code "hoat_dong"}.</li>
     * </ul>
     *
     * @param id ID nhân viên cần đổi trạng thái.
     * @return {@link NguoiDung} sau khi cập nhật trạng thái.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy.
     */
    NguoiDung doiTrangThai(Integer id);
}
