package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng cau_hinh_thanh_toan (Module 7: Đơn hàng).
 * <p>
 * Cấu hình tài khoản ngân hàng nhận tiền chuyển khoản (dùng để sinh QR động qua VietQR API).
 * <p>
 * Quyền quản lý: Chỉ Admin mới được phép cấu hình bảng này (system_rules.md §1).
 * <p>
 * ⚠️ Ràng buộc đặc biệt (Filtered Unique Index trong SQL Server):
 * {@code CREATE UNIQUE INDEX uq_cauhinh_macdinh ON cau_hinh_thanh_toan (la_mac_dinh) WHERE la_mac_dinh = 1}
 * → Chỉ được phép có TỐI ĐA 1 bản ghi có la_mac_dinh = true tại mọi thời điểm.
 * JPA/Hibernate KHÔNG hỗ trợ Filtered Unique Index → Ràng buộc này phải được
 * kiểm tra và đảm bảo tại Service Layer trước khi gọi save().
 * <p>
 * Ghi chú (system_rules.md §7 – Tính năng hoãn lại):
 *  Cổng thanh toán SePay/VietQR tạm thời bị HOÃN.
 *  Entity này được tạo sẵn để không phải refactor DB sau.
 */
@Entity
@Table(name = "cau_hinh_thanh_toan")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CauHinhThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Tên ngân hàng hiển thị (ví dụ: "MB Bank", "Vietcombank").
     */
    @Column(name = "ten_ngan_hang", nullable = false, length = 100)
    private String tenNganHang;

    /**
     * Mã ngân hàng theo chuẩn VietQR (ví dụ: "MB", "VCB", "TCB").
     * Dùng để gọi API VietQR sinh mã QR chuyển khoản động.
     */
    @Column(name = "bank_id", nullable = false, length = 20)
    private String bankId;

    /** Số tài khoản ngân hàng nhận tiền. */
    @Column(name = "so_tai_khoan", nullable = false, length = 50)
    private String soTaiKhoan;

    /**
     * Tên chủ tài khoản.
     * Bắt buộc IN HOA, không dấu theo chuẩn ngân hàng (ví dụ: "NGUYEN VAN A").
     */
    @Column(name = "ten_chu_tk", nullable = false, length = 100)
    private String tenChuTk;

    /**
     * Đánh dấu đây là tài khoản mặc định nhận tiền (DEFAULT false).
     * ⚠️ Tối đa 1 bản ghi có giá trị true tại mọi thời điểm.
     * Filtered Unique Index ở DB không thể map qua JPA → kiểm tra tại Service Layer.
     */
    @Column(name = "la_mac_dinh", nullable = false)
    @Builder.Default
    private Boolean laMacDinh = false;

    /** Trạng thái kích hoạt tài khoản này (DEFAULT true). */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    /** Ghi chú nội bộ (ví dụ: "Tài khoản dùng cho demo đồ án"). */
    @Column(name = "ghi_chu", length = 255)
    private String ghiChu;
}
