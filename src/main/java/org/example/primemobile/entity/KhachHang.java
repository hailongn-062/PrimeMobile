package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping bảng khach_hang (Module 1: Người dùng & Phân quyền).
 * <p>
 * Hỗ trợ 2 loại khách hàng:
 *  - Khách vãng lai (guest):  nguoiDung = NULL, không cần tài khoản.
 *  - Khách có tài khoản:       nguoiDung != NULL, quan hệ 1-1 với bảng nguoi_dung.
 * <p>
 * Trường hang_thanh_vien phản ánh hạng hiện tại:
 *   "dong" (0đ) | "bac" (≥50tr) | "vang" (≥100tr) | "kim_cuong" (≥250tr)
 * Logic xét thăng hạng được hardcode ở Service layer theo system_rules.md §4.
 * <p>
 * Điểm tích lũy (diem_tich_luy) chỉ dùng để hiển thị và căn cứ thăng hạng,
 * TUYỆT ĐỐI KHÔNG quy đổi thành tiền hoặc trừ trực tiếp vào hóa đơn.
 */
@Entity
@Table(
        name = "khach_hang",
        indexes = {
                @Index(name = "idx_kh_sdt",   columnList = "so_dien_thoai"),
                @Index(name = "idx_kh_email",  columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_kh_nguoi_dung", columnNames = "nguoi_dung_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Liên kết 1-1 với tài khoản nguoi_dung.
     * NULL = khách vãng lai (chưa có tài khoản).
     * ON DELETE SET NULL — khi xóa nguoi_dung, khach_hang vẫn được giữ lại.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nguoi_dung_id",
            referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_kh_nd"),
            unique = true
    )
    private NguoiDung nguoiDung;

    /** Họ và tên người dùng (bắt buộc). */
    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    /**
     * Email liên lạc của khách hàng.
     * Có thể NULL với khách vãng lai.
     */
    @Column(name = "email", length = 100)
    private String email;

    /** Số điện thoại liên lạc (bắt buộc). */
    @Column(name = "so_dien_thoai", nullable = false, length = 20)
    private String soDienThoai;

    /**
     * Giới tính.
     * Giá trị hợp lệ: "Nam" | "Nu" | "Khac" | NULL.
     */
    @Column(name = "gioi_tinh", length = 5)
    private String gioiTinh;

    /** Ngày sinh (chỉ lưu phần ngày, không lưu giờ). */
    @Column(name = "ngay_sinh")
    private LocalDate ngaySinh;

    /**
     * Điểm tích lũy hiện tại (DEFAULT 0).
     * Mục đích: hiển thị mức độ thân thiết & xét thăng hạng.
     * KHÔNG được dùng để trừ tiền thanh toán.
     */
    @Column(name = "diem_tich_luy", nullable = false)
    @Builder.Default
    private Integer diemTichLuy = 0;

    /**
     * Hạng thành viên hiện tại (DEFAULT 'dong').
     * Giá trị hợp lệ: "dong" | "bac" | "vang" | "kim_cuong"
     * Logic thăng hạng hardcode trong Service Layer (system_rules.md §4.2).
     */
    @Column(name = "hang_thanh_vien", nullable = false, length = 15)
    @Builder.Default
    private String hangThanhVien = "dong";

    /**
     * Tổng tiền chi tiêu tích lũy (DEFAULT 0).
     * Dùng làm căn cứ xét thăng hạng theo system_rules.md §4.2.
     * Chỉ cộng khi đơn hàng chuyển sang trạng thái 'da_giao'.
     */
    @Column(name = "tong_chi_tieu", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tongChiTieu = BigDecimal.ZERO;

    /** Thời điểm tạo bản ghi. Mặc định = GETDATE() ở DB. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật bản ghi gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
