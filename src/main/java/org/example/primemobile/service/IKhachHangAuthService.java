package org.example.primemobile.service;

import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.KhachHang;

/**
 * Contract cho phân hệ Xác thực Khách Hàng (đăng nhập / đăng ký).
 * <p>
 * Tách biệt hoàn toàn với {@link IAuthService} (dành cho Admin/NhanVien).
 * <p>
 * Quy tắc áp dụng (system_rules.md §1):
 * <ul>
 *   <li>TUYỆT ĐỐI KHÔNG dùng Spring Security / JWT.</li>
 *   <li>Phiên đăng nhập quản lý bằng {@code HttpSession} với key {@code "CURRENT_CUSTOMER"}.</li>
 *   <li>Mật khẩu so sánh plain-text (chế độ demo, nhất quán với toàn hệ thống).</li>
 * </ul>
 */
public interface IKhachHangAuthService {

    /**
     * Đăng nhập khách hàng bằng email + mật khẩu.
     * <p>
     * Luồng nghiệp vụ:
     * <ol>
     *   <li>Tìm {@code NguoiDung} theo email.</li>
     *   <li>Kiểm tra {@code vaiTro = 'KhachHang'}.</li>
     *   <li>Kiểm tra tài khoản không bị khóa ({@code trangThai = 'hoat_dong'}).</li>
     *   <li>So sánh mật khẩu plain-text.</li>
     *   <li>Tìm bản ghi {@code KhachHang} liên kết với {@code NguoiDung}.</li>
     *   <li>Trả về {@link SessionKhachHang} để Controller lưu vào HttpSession.</li>
     * </ol>
     *
     * @param email    Email đăng nhập.
     * @param matKhau  Mật khẩu plain-text từ form.
     * @return {@link SessionKhachHang} chứa thông tin khách hàng đã xác thực.
     * @throws jakarta.persistence.EntityNotFoundException nếu email không tồn tại.
     * @throws IllegalArgumentException nếu mật khẩu sai, tài khoản bị khóa,
     *                                  hoặc vai trò không phải KhachHang.
     */
    SessionKhachHang dangNhap(String email, String matKhau);

    /**
     * Đăng ký tài khoản khách hàng mới.
     * <p>
     * Luồng nghiệp vụ:
     * <ol>
     *   <li>Validate email chưa tồn tại trong {@code nguoi_dung}.</li>
     *   <li>Validate SĐT chưa tồn tại trong {@code khach_hang}.</li>
     *   <li>Tạo bản ghi {@code NguoiDung} với {@code vaiTro = 'KhachHang'}, lưu mật khẩu plain-text.</li>
     *   <li>Tạo bản ghi {@code KhachHang} liên kết với {@code NguoiDung} vừa tạo.</li>
     *   <li>Trả về {@link SessionKhachHang} để Controller tự động đăng nhập sau đăng ký.</li>
     * </ol>
     *
     * @param hoTen       Họ và tên khách hàng.
     * @param email       Email đăng nhập (duy nhất toàn hệ thống).
     * @param soDienThoai Số điện thoại liên lạc (duy nhất trong khach_hang).
     * @param matKhau     Mật khẩu plain-text (lưu thẳng, chế độ demo).
     * @return {@link SessionKhachHang} để Controller lưu vào HttpSession.
     * @throws IllegalArgumentException nếu email hoặc SĐT đã tồn tại.
     */
    SessionKhachHang dangKy(String hoTen, String email, String soDienThoai, String matKhau);
    /**
     * Đổi mật khẩu cho khách hàng (Quên mật khẩu).
     * <p>
     * Luồng nghiệp vụ:
     * <ol>
     *   <li>Tìm {@code NguoiDung} theo email.</li>
     *   <li>Kiểm tra {@code vaiTro = 'KhachHang'}.</li>
     *   <li>Kiểm tra {@code soDienThoai} khớp với bản ghi NguoiDung.</li>
     *   <li>Cập nhật mật khẩu mới (plain-text).</li>
     * </ol>
     *
     * @param email       Email đã đăng ký.
     * @param soDienThoai Số điện thoại đã đăng ký.
     * @param matKhauMoi  Mật khẩu mới (plain-text).
     * @throws IllegalArgumentException nếu email không tồn tại, sai SĐT, hoặc không phải KhachHang.
     */
    void doiMatKhau(String email, String soDienThoai, String matKhauMoi);
}
