package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping bảng lich_su_diem (Module 10: Điểm tích lũy & Hạng thành viên).
 * <p>
 * Ghi lại toàn bộ lịch sử biến động điểm tích lũy của khách hàng.
 * Điểm hiện tại được lưu tại {@code khach_hang.diem_tich_luy};
 * bảng này chỉ dùng để tra cứu lịch sử và audit.
 * <p>
 * Công thức tích điểm (hardcode tại Service Layer — system_rules.md §4.1):
 *  diem_cong = FLOOR(tong_thanh_toan / 100.000) × he_so_hang
 *  Hệ số: dong=1.0 | bac=1.2 | vang=1.5 | kim_cuong=2.0
 * <p>
 * ⚠️ Mục đích điểm (system_rules.md §4.1):
 *  TUYỆT ĐỐI KHÔNG dùng điểm để quy đổi thành tiền hoặc giảm trừ hóa đơn.
 *  Điểm chỉ dùng để hiển thị thân thiết và xét thăng hạng.
 * <p>
 * Ghi chú: Logic tích điểm tạm hoãn triển khai (system_rules.md §7),
 * nhưng Entity vẫn được tạo để mapping đúng DB.
 * <p>
 * Loại biến động (CHECK chk_lsd_loai):
 *  "cong" | "tru" | "het_han" | "dieu_chinh"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link KhachHang} (FK khach_hang_id)
 *  - N:1 với {@link DonHang}   (FK don_hang_id, ON DELETE SET NULL, nullable)
 */
@Entity
@Table(
        name = "lich_su_diem",
        indexes = {
                @Index(name = "idx_lsd_kh", columnList = "khach_hang_id, thoi_gian")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LichSuDiem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Khách hàng có biến động điểm.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_lsd_kh")
    )
    private KhachHang khachHang;

    /**
     * Loại biến động điểm.
     * Giá trị hợp lệ: "cong" | "tru" | "het_han" | "dieu_chinh"
     */
    @Column(name = "loai", nullable = false, length = 15)
    private String loai;

    /**
     * Số điểm thay đổi trong lần này (luôn dương, loại quyết định cộng hay trừ).
     */
    @Column(name = "so_diem", nullable = false)
    private Integer soDiem;

    /** Số điểm trước khi biến động (để audit và hiển thị lịch sử). */
    @Column(name = "so_diem_truoc", nullable = false)
    private Integer soDiemTruoc;

    /** Số điểm sau khi biến động = soDiemTruoc ± soDiem. */
    @Column(name = "so_diem_sau", nullable = false)
    private Integer soDiemSau;

    /**
     * Đơn hàng phát sinh biến động điểm (khi loai = 'cong').
     * ON DELETE SET NULL – xóa đơn hàng không mất lịch sử điểm.
     * NULL với các loại 'tru', 'het_han', 'dieu_chinh'.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "don_hang_id",
            foreignKey = @ForeignKey(name = "fk_lsd_dh")
    )
    private DonHang donHang;

    /** Lý do biến động (ví dụ: "Tích điểm đơn hàng DH2024001", "Điều chỉnh thủ công"). */
    @Column(name = "ly_do", length = 255)
    private String lyDo;

    /**
     * Ngày hết hạn của lô điểm này (nếu hệ thống có quy định điểm hết hạn).
     * NULL = điểm không có hạn sử dụng.
     */
    @Column(name = "ngay_het_han")
    private LocalDate ngayHetHan;

    /** Thời điểm xảy ra biến động điểm (DEFAULT GETDATE()). */
    @Column(name = "thoi_gian", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime thoiGian = LocalDateTime.now();
}
