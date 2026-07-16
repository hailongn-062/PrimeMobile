package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping báº£ng chi_tiet_phieu_nhap (Module 3: Kho hÃ ng).
 * <p>
 * Má»—i dÃ²ng chi tiáº¿t = 1 SKU Ä‘Æ°á»£c nháº­p vá»›i sá»‘ lÆ°á»£ng vÃ  Ä‘Æ¡n giÃ¡ nháº­p cá»¥ thá»ƒ.
 * <p>
 * âš ï¸ QUAN TRá»ŒNG â€“ Cá»™t tÃ­nh toÃ¡n (Computed Column):
 * Cá»™t {@code thanh_tien} trong SQL Server Ä‘Æ°á»£c Ä‘á»‹nh nghÄ©a lÃ :
 * {@code AS (so_luong * don_gia_nhap) PERSISTED}
 * Do Ä‘Ã³, trÆ°á»ng {@code thanhTien} PHáº¢I Ä‘Æ°á»£c khai bÃ¡o vá»›i:
 * {@code @Column(insertable = false, updatable = false)}
 * Ä‘á»ƒ Hibernate KHÃ”NG cá»‘ gáº¯ng INSERT/UPDATE vÃ o cá»™t nÃ y,
 * trÃ¡nh lá»—i "Cannot update a computed column" lÃ m sáº­p á»©ng dá»¥ng.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link PhieuNhapKho}   (FK phieu_nhap_id, ON DELETE CASCADE)
 *  - N:1 vá»›i {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(name = "chi_tiet_phieu_nhap")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietPhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Phiáº¿u nháº­p kho chá»©a dÃ²ng chi tiáº¿t nÃ y.
     * ON DELETE CASCADE á»Ÿ DB â€“ xÃ³a phiáº¿u nháº­p thÃ¬ chi tiáº¿t tá»± xÃ³a theo.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "phieu_nhap_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctpn_phieu")
    )
    private PhieuNhapKho phieuNhapKho;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m (SKU) Ä‘Æ°á»£c nháº­p trong dÃ²ng nÃ y.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctpn_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /** Sá»‘ lÆ°á»£ng nháº­p trong dÃ²ng nÃ y. Pháº£i > 0. */
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    /** ÄÆ¡n giÃ¡ nháº­p (giÃ¡ mua tá»« NCC) cho 1 Ä‘Æ¡n vá»‹ SKU. */
    @Column(name = "don_gia_nhap", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGiaNhap;

    /**
     * âš ï¸ Cá»˜T TÃNH TOÃN Tá»° Äá»˜NG (Computed Column - PERSISTED):
     * ÄÆ°á»£c SQL Server tá»± tÃ­nh theo cÃ´ng thá»©c: so_luong * don_gia_nhap
     * <p>
     * Báº®T BUá»˜C dÃ¹ng insertable = false, updatable = false:
     *  - Hibernate sáº½ Äá»ŒC giÃ¡ trá»‹ nÃ y tá»« DB sau khi INSERT/UPDATE.
     *  - Hibernate sáº½ KHÃ”NG Cá» Gáº®NG ghi vÃ o cá»™t nÃ y, trÃ¡nh lá»—i runtime.
     */
    @Column(name = "thanh_tien", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal thanhTien;
}
