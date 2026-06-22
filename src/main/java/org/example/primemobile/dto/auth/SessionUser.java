package org.example.primemobile.dto.auth;

import java.io.Serializable;

/**
 * Đối tượng lưu thông tin người dùng đã xác thực vào {@code HttpSession}.
 * <p>
 * Được gán vào session với key {@code "CURRENT_ADMIN"} sau khi đăng nhập thành công.
 * Implement {@link Serializable} vì HttpSession có thể serialize object khi
 * ứng dụng restart (yêu cầu bắt buộc của Servlet spec).
 * <p>
 * Chỉ chứa thông tin tối thiểu cần thiết cho mỗi request:
 * <ul>
 *   <li>{@code id}     – PK bảng nguoi_dung, dùng để tra cứu chi tiết khi cần.</li>
 *   <li>{@code email}  – Hiển thị trên header Dashboard.</li>
 *   <li>{@code hoTen}  – Hiển thị tên người dùng trên UI.</li>
 *   <li>{@code role}   – Giá trị vaiTro ("Admin" | "NhanVien"), dùng để phân quyền
 *                        tại {@code AuthInterceptor}.</li>
 * </ul>
 *
 * @see org.example.primemobile.interceptor.AuthInterceptor
 */
public class SessionUser implements Serializable {

    /** serialVersionUID bắt buộc khi implement Serializable. */
    private static final long serialVersionUID = 1L;

    private Integer id;
    private String  email;
    private String  hoTen;
    private String  role;   // "Admin" | "NhanVien"

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SessionUser() {
    }

    public SessionUser(Integer id, String email, String hoTen, String role) {
        this.id    = id;
        this.email = email;
        this.hoTen = hoTen;
        this.role  = role;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHoTen() {
        return hoTen;
    }

    public void setHoTen(String hoTen) {
        this.hoTen = hoTen;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    @Override
    public String toString() {
        return "SessionUser{id=" + id + ", email='" + email + "', role='" + role + "'}";
    }
}
