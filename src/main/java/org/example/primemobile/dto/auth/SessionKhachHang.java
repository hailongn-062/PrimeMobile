package org.example.primemobile.dto.auth;

import java.io.Serializable;

/**
 * Đối tượng lưu thông tin Khách Hàng đã đăng nhập vào {@code HttpSession}.
 * <p>
 * Được gán vào session với key {@code "CURRENT_CUSTOMER"} sau khi đăng nhập thành công.
 * Tách biệt hoàn toàn với {@link SessionUser} (dành cho Admin/NhanVien).
 * <p>
 * Chứa thông tin tối thiểu cần thiết cho mỗi request của khách hàng:
 * <ul>
 *   <li>{@code khachHangId} – PK bảng khach_hang, dùng để truy vấn đơn hàng, địa chỉ...</li>
 *   <li>{@code email}       – Hiển thị trên header trang khách hàng.</li>
 *   <li>{@code hoTen}       – Tên hiển thị.</li>
 *   <li>{@code soDienThoai} – Số điện thoại liên lạc.</li>
 * </ul>
 */
public class SessionKhachHang implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Key lưu trong HttpSession — nhất quán với KhachHangAuthController. */
    public static final String SESSION_KEY = "CURRENT_CUSTOMER";

    private Integer khachHangId;
    private String  email;
    private String  hoTen;
    private String  soDienThoai;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public SessionKhachHang() {
    }

    public SessionKhachHang(Integer khachHangId, String email, String hoTen, String soDienThoai) {
        this.khachHangId  = khachHangId;
        this.email        = email;
        this.hoTen        = hoTen;
        this.soDienThoai  = soDienThoai;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public Integer getKhachHangId() {
        return khachHangId;
    }

    public void setKhachHangId(Integer khachHangId) {
        this.khachHangId = khachHangId;
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

    public String getSoDienThoai() {
        return soDienThoai;
    }

    public void setSoDienThoai(String soDienThoai) {
        this.soDienThoai = soDienThoai;
    }

    @Override
    public String toString() {
        return "SessionKhachHang{khachHangId=" + khachHangId
                + ", email='" + email
                + "', hoTen='" + hoTen + "'}";
    }
}
