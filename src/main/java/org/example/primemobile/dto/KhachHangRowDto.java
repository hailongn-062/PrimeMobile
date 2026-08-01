package org.example.primemobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO tổng hợp thông tin khách hàng dùng trong trang danh sách Admin.
 * <p>
 * Gom dữ liệu từ nhiều bảng:
 * <ul>
 *   <li>{@code khach_hang} — thông tin cơ bản</li>
 *   <li>{@code nguoi_dung} — trạng thái tài khoản, ngày tham gia</li>
 *   <li>{@code don_hang}   — số đơn đã mua, tổng chi tiêu</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhachHangRowDto {

    // ── Thông tin cơ bản khách hàng (khach_hang) ─────────────────────────
    private Integer id;
    private String  hoTen;
    private String  email;
    private String  soDienThoai;

    // ── Tài khoản (nguoi_dung) ────────────────────────────────────────────
    /** ID của nguoi_dung liên kết (null nếu là khách vãng lai). */
    private Integer nguoiDungId;

    /**
     * Ngày tham gia = ngay_tao của nguoi_dung.
     * Null nếu khách chưa đăng ký tài khoản.
     */
    private LocalDateTime ngayThamGia;

    /**
     * Trạng thái tài khoản: "hoat_dong" | "khoa".
     * Null nếu khách vãng lai (chưa có tài khoản).
     */
    private String trangThai;

    // ── Thống kê đơn hàng (don_hang) ─────────────────────────────────────
    /**
     * Tổng chi tiêu = SUM(tong_thanh_toan) của các đơn đã hoàn thành.
     * (trang_thai = 'da_hoan_thanh')
     */
    private BigDecimal tongChiTieu;
}
