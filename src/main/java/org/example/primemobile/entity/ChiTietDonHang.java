package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping bảng chi_tiet_don_hang (Module 7: Đơn hàng).
 * <p>
 * Mỗi dòng = 1 SKU trong đơn hàng với giá bán tại thời điểm đặt (price snapshot).
 * Giá được snapshot ngay khi đặt hàng để tránh sai lệch khi giá sau đó thay đổi.
 * <p>
 * ⚠️ COMPUTED COLUMN (AS PERSISTED):
 * Cột {@code thanh_tien} = so_luong * don_gia_ban
 * BẮT BUỘC dùng {@code @Column(insertable = false, updatable = false)}.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link DonHang}        (FK don_hang_id, ON DELETE CASCADE)
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(
        name = "chi_tiet_don_hang",
        indexes = {
                @Index(name = "idx_ctdh_dh", columnList = "don_hang_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Đơn hàng chứa dòng này.
     * ON DELETE CASCADE – xóa đơn hàng thì chi tiết tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctdh_dh")
    )
    private DonHang donHang;

    /**
     * Biến thể sản phẩm (SKU) trong dòng đơn hàng.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctdh_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * Số lượng sản phẩm trong dòng này.
     * CHECK: so_luong > 0.
     */
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    /**
     * Đơn giá bán tại thời điểm đặt hàng (price snapshot).
     * CHECK: don_gia_ban >= 0.
     * Không thay đổi dù giá sản phẩm sau này bị điều chỉnh.
     */
    @Column(name = "don_gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGiaBan;

    /**
     * ⚠️ CỘT TÍNH TOÁN TỰ ĐỘNG (Computed Column - PERSISTED):
     * SQL Server tự tính: so_luong * don_gia_ban
     * BẮT BUỘC insertable = false, updatable = false
     * để Hibernate không cố ghi vào cột này gây lỗi "Cannot update a computed column".
     */
    @Column(name = "thanh_tien", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal thanhTien;
}
