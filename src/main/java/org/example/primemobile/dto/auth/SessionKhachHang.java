package org.example.primemobile.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * DTO đại diện cho phiên đăng nhập của Khách Hàng.
 * Lưu trữ các thông tin cần thiết trong HttpSession để phân biệt với Admin/NhanVien.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionKhachHang implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Key dùng để lưu đối tượng này vào HttpSession */
    public static final String SESSION_KEY = "CURRENT_CUSTOMER";

    private Integer khachHangId;
    private String email;
    private String hoTen;
    private String soDienThoai;
}
