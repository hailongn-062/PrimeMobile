package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng dia_chi_khach_hang (Module 5: Địa chỉ khách hàng).
 * <p>
 * Lưu danh sách địa chỉ giao hàng của một khách hàng.
 * Mỗi địa chỉ lưu 2 nhóm thông tin song song phục vụ 2 mục đích khác nhau:
 * <p>
 * <b>Nhóm 1 – ID địa chỉ (dùng để gọi API GHN tính phí ship):</b>
 * {@code tinh_thanh_id}, {@code quan_huyen_id}, {@code phuong_xa_code}
 * <p>
 * <b>Nhóm 2 – Tên địa chỉ (dùng để hiển thị UI ngay lập tức, không cần gọi API):</b>
 * {@code tinh_thanh_ten}, {@code quan_huyen_ten}, {@code phuong_xa_ten}
 * <p>
 * Ghi chú GHN (system_rules.md §6):
 *  - Chỉ dùng API đọc (tính phí, leadtime), KHÔNG gọi API tạo đơn vận chuyển.
 *  - Mọi lời gọi HTTP sang GHN bắt buộc bọc trong try-catch, fallback về phí = 0.
 * <p>
 * Loại địa chỉ hợp lệ (CHECK chk_dc_loai):
 *  "nha_rieng" | "co_quan" | "khac"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link KhachHang} (FK khach_hang_id, ON DELETE CASCADE)
 */
@Entity
@Table(name = "dia_chi_khach_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiaChiKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Khách hàng sở hữu địa chỉ này.
     * ON DELETE CASCADE – xóa khách hàng thì địa chỉ tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dc_kh")
    )
    private KhachHang khachHang;

    /**
     * Loại địa chỉ (DEFAULT 'nha_rieng').
     * Giá trị hợp lệ: "nha_rieng" | "co_quan" | "khac"
     */
    @Column(name = "loai_dia_chi", nullable = false, length = 15)
    @Builder.Default
    private String loaiDiaChi = "nha_rieng";

    /** Tên người nhận hàng tại địa chỉ này. */
    @Column(name = "ho_ten_nguoi_nhan", length = 100)
    private String hoTenNguoiNhan;

    /** Số điện thoại người nhận hàng. */
    @Column(name = "so_dien_thoai_nguoi_nhan", length = 20)
    private String soDienThoaiNguoiNhan;

    /** Số nhà, tên đường, tòa nhà... (phần địa chỉ chi tiết). */
    @Column(name = "dia_chi_chi_tiet", nullable = false, length = 255)
    private String diaChiChiTiet;

    // -------------------------------------------------------------------------
    // NHÓM 1: ID địa chỉ – gửi sang API GHN để tính phí ship & leadtime
    // -------------------------------------------------------------------------

    /**
     * ID tỉnh/thành phố theo hệ thống GHN.
     * Dùng cho API: /v2/shipping-order/fee và /v2/shipping-order/leadtime
     */
    @Column(name = "tinh_thanh_id", nullable = false)
    private Integer tinhThanhId;

    /**
     * ID quận/huyện theo hệ thống GHN.
     */
    @Column(name = "quan_huyen_id", nullable = false)
    private Integer quanHuyenId;

    /**
     * Mã phường/xã theo hệ thống GHN.
     * Kiểu VARCHAR vì GHN dùng mã dạng chuỗi (ví dụ: "550113").
     */
    @Column(name = "phuong_xa_code", nullable = false, length = 20)
    private String phuongXaCode;

    // -------------------------------------------------------------------------
    // NHÓM 2: Tên địa chỉ – hiển thị ngay trên UI, không cần gọi thêm API
    // -------------------------------------------------------------------------

    /** Tên tỉnh/thành phố để hiển thị (ví dụ: "TP. Hồ Chí Minh"). */
    @Column(name = "tinh_thanh_ten", nullable = false, length = 100)
    private String tinhThanhTen;

    /** Tên quận/huyện để hiển thị (ví dụ: "Quận 1"). */
    @Column(name = "quan_huyen_ten", nullable = false, length = 100)
    private String quanHuyenTen;

    /** Tên phường/xã để hiển thị (ví dụ: "Phường Bến Nghé"). */
    @Column(name = "phuong_xa_ten", nullable = false, length = 100)
    private String phuongXaTen;

    /**
     * Đánh dấu đây là địa chỉ mặc định của khách hàng (DEFAULT false).
     * Mỗi khách hàng chỉ nên có 1 địa chỉ mặc định.
     * Logic đảm bảo duy nhất 1 mac_dinh = true phải được xử lý tại Service Layer.
     */
    @Column(name = "mac_dinh", nullable = false)
    @Builder.Default
    private Boolean macDinh = false;
}
