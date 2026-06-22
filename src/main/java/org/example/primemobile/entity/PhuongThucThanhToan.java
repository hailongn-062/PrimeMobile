package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng phuong_thuc_thanh_toan (Module 7: Đơn hàng).
 * <p>
 * Danh mục các phương thức thanh toán được hỗ trợ.
 * <p>
 * Ghi chú (system_rules.md §7 – Tính năng hoãn lại):
 *  Hiện tại hệ thống chỉ sử dụng duy nhất phương thức COD (Nhận hàng trả tiền mặt).
 *  Tích hợp cổng thanh toán SePay/VietQR tạm thời bị HOÃN LẠI.
 * <p>
 * Quan hệ:
 *  - 1-N với {@link ThanhToan} (mappedBy phuongThucThanhToan)
 */
@Entity
@Table(
        name = "phuong_thuc_thanh_toan",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pttt_ten", columnNames = "ten_pttt")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "thanhToans")
@EqualsAndHashCode(exclude = "thanhToans")
public class PhuongThucThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Tên phương thức thanh toán – duy nhất.
     * Ví dụ: "Tiền mặt / COD", "Chuyển khoản ngân hàng".
     */
    @Column(name = "ten_pttt", nullable = false, length = 50, unique = true)
    private String tenPttt;

    /** Mô tả chi tiết về phương thức thanh toán. */
    @Column(name = "mo_ta", length = 255)
    private String moTa;

    /**
     * Trạng thái kích hoạt (DEFAULT true).
     * false = tạm ngừng hỗ trợ phương thức này.
     */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 PhuongThucThanhToan → nhiều ThanhToan
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phuongThucThanhToan", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ThanhToan> thanhToans = new ArrayList<>();
}
