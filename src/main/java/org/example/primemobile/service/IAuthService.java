package org.example.primemobile.service;

import org.example.primemobile.dto.auth.DangNhapAdminRequest;
import org.example.primemobile.dto.auth.SessionUser;

/**
 * Contract cho phân hệ Xác thực dành riêng cho Admin và NhanVien.
 * <p>
 * Theo system_rules.md §1:
 * <ul>
 *   <li>TUYỆT ĐỐI KHÔNG dùng Spring Security hay JWT.</li>
 *   <li>Quản lý phiên bằng {@code HttpSession} + {@code HandlerInterceptor}.</li>
 *   <li>Đăng nhập bằng email kết hợp mật khẩu băm.</li>
 * </ul>
 */
public interface IAuthService {

    /**
     * Xác thực thông tin đăng nhập của Admin hoặc NhanVien.
     * <p>
     * Luồng nghiệp vụ:
     * <ol>
     *   <li>Tìm {@code NguoiDung} theo {@code email}.</li>
     *   <li>Kiểm tra tài khoản không bị khóa ({@code trang_thai = 'hoat_dong'}).</li>
     *   <li>Kiểm tra vai trò phải là {@code "Admin"} hoặc {@code "NhanVien"}.</li>
     *   <li>Xác minh mật khẩu qua {@code PasswordUtil.checkPassword()}.</li>
     *   <li>Nếu hợp lệ, trả về {@link SessionUser} để Controller lưu vào {@code HttpSession}.</li>
     * </ol>
     *
     * @param request DTO chứa email và mật khẩu plain-text từ form.
     * @return {@link SessionUser} chứa thông tin người dùng đã xác thực.
     * @throws jakarta.persistence.EntityNotFoundException nếu email không tồn tại.
     * @throws IllegalArgumentException                    nếu mật khẩu sai, tài khoản bị khóa,
     *                                                     hoặc vai trò không có quyền truy cập.
     */
    SessionUser dangNhapAdmin(DangNhapAdminRequest request);
}
