package org.example.primemobile.dto.auth;

/**
 * DTO nhận thông tin đăng nhập từ form Admin Dashboard.
 * <p>
 * Áp dụng cho endpoint {@code POST /api/auth/admin/dang-nhap}.
 * Chỉ Admin và NhanVien được phép đăng nhập qua luồng này
 * (system_rules.md §1 — Cơ chế Đăng nhập).
 *
 * <ul>
 *   <li>{@code email}    – Email đăng nhập, bắt buộc.</li>
 *   <li>{@code matKhau}  – Mật khẩu plain-text từ form, sẽ được băm SHA-256
 *                          tại Service Layer để so sánh với giá trị trong DB.</li>
 * </ul>
 */
public class DangNhapAdminRequest {

    private String email;
    private String matKhau;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public DangNhapAdminRequest() {
    }

    public DangNhapAdminRequest(String email, String matKhau) {
        this.email    = email;
        this.matKhau  = matKhau;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMatKhau() {
        return matKhau;
    }

    public void setMatKhau(String matKhau) {
        this.matKhau = matKhau;
    }
}
