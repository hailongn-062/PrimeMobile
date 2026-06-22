package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng chi_tiet_gio_hang (Module 6: Giỏ hàng).
 * <p>
 * Mỗi dòng = 1 SKU được thêm vào giỏ với số lượng cụ thể.
 * Ràng buộc UNIQUE (gio_hang_id, bien_the_san_pham_id) đảm bảo
 * cùng 1 SKU chỉ xuất hiện 1 lần trong 1 giỏ hàng.
 * Khi khách thêm cùng SKU lần 2, Service Layer phải UPDATE số lượng,
 * không INSERT bản ghi mới.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link GioHang}        (FK gio_hang_id, ON DELETE CASCADE)
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(
        name = "chi_tiet_gio_hang",
        uniqueConstraints = {
                // Mỗi SKU chỉ xuất hiện 1 lần trong 1 giỏ hàng
                @UniqueConstraint(name = "uq_ctgh", columnNames = {"gio_hang_id", "bien_the_san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Giỏ hàng chứa dòng này.
     * ON DELETE CASCADE – xóa giỏ hàng thì chi tiết tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "gio_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctgh_gh")
    )
    private GioHang gioHang;

    /**
     * Biến thể sản phẩm (SKU) được thêm vào giỏ.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctgh_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * Số lượng sản phẩm trong giỏ (DEFAULT 1).
     * CHECK constraint ở DB: so_luong > 0.
     */
    @Column(name = "so_luong", nullable = false)
    @Builder.Default
    private Integer soLuong = 1;

    /** Thời điểm thêm vào giỏ hàng. */
    @Column(name = "ngay_them", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayThem = LocalDateTime.now();
}
