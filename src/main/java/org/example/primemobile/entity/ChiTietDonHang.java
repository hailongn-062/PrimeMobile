package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping báº£ng chi_tiet_don_hang (Module 7: ÄÆ¡n hÃ ng).
 * <p>
 * Má»—i dÃ²ng = 1 SKU trong Ä‘Æ¡n hÃ ng vá»›i giÃ¡ bÃ¡n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t (price snapshot).
 * GiÃ¡ Ä‘Æ°á»£c snapshot ngay khi Ä‘áº·t hÃ ng Ä‘á»ƒ trÃ¡nh sai lá»‡ch khi giÃ¡ sau Ä‘Ã³ thay Ä‘á»•i.
 * <p>
 * âš ï¸ COMPUTED COLUMN (AS PERSISTED):
 * Cá»™t {@code thanh_tien} = so_luong * don_gia_ban
 * Báº®T BUá»˜C dÃ¹ng {@code @Column(insertable = false, updatable = false)}.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link DonHang}        (FK don_hang_id, ON DELETE CASCADE)
 *  - N:1 vá»›i {@link BienTheSanPham} (FK bien_the_san_pham_id)
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
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietDonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ÄÆ¡n hÃ ng chá»©a dÃ²ng nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a Ä‘Æ¡n hÃ ng thÃ¬ chi tiáº¿t tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctdh_dh")
    )
    @com.fasterxml.jackson.annotation.JsonIgnore
    private DonHang donHang;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m (SKU) trong dÃ²ng Ä‘Æ¡n hÃ ng.
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
     * Sá»‘ lÆ°á»£ng sáº£n pháº©m trong dÃ²ng nÃ y.
     * CHECK: so_luong > 0.
     */
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    /**
     * ÄÆ¡n giÃ¡ bÃ¡n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng (price snapshot).
     * CHECK: don_gia_ban >= 0.
     * KhÃ´ng thay Ä‘á»•i dÃ¹ giÃ¡ sáº£n pháº©m sau nÃ y bá»‹ Ä‘iá»u chá»‰nh.
     */
    @Column(name = "don_gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGiaBan;

    /**
     * âš ï¸ Cá»˜T TÃNH TOÃN Tá»° Äá»˜NG (Computed Column - PERSISTED):
     * SQL Server tá»± tÃ­nh: so_luong * don_gia_ban
     * Báº®T BUá»˜C insertable = false, updatable = false
     * Ä‘á»ƒ Hibernate khÃ´ng cá»‘ ghi vÃ o cá»™t nÃ y gÃ¢y lá»—i "Cannot update a computed column".
     */
    @Column(name = "thanh_tien", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal thanhTien;
}
