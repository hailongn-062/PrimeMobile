package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng chi_tiet_gio_hang (Module 6: Giá» hÃ ng).
 * <p>
 * Má»—i dÃ²ng = 1 SKU Ä‘Æ°á»£c thÃªm vÃ o giá» vá»›i sá»‘ lÆ°á»£ng cá»¥ thá»ƒ.
 * RÃ ng buá»™c UNIQUE (gio_hang_id, bien_the_san_pham_id) Ä‘áº£m báº£o
 * cÃ¹ng 1 SKU chá»‰ xuáº¥t hiá»‡n 1 láº§n trong 1 giá» hÃ ng.
 * Khi khÃ¡ch thÃªm cÃ¹ng SKU láº§n 2, Service Layer pháº£i UPDATE sá»‘ lÆ°á»£ng,
 * khÃ´ng INSERT báº£n ghi má»›i.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link GioHang}        (FK gio_hang_id, ON DELETE CASCADE)
 *  - N:1 vá»›i {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(
        name = "chi_tiet_gio_hang",
        uniqueConstraints = {
                // Má»—i SKU chá»‰ xuáº¥t hiá»‡n 1 láº§n trong 1 giá» hÃ ng
                @UniqueConstraint(name = "uq_ctgh", columnNames = {"gio_hang_id", "bien_the_san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChiTietGioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Giá» hÃ ng chá»©a dÃ²ng nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a giá» hÃ ng thÃ¬ chi tiáº¿t tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "gio_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctgh_gh")
    )
    private GioHang gioHang;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m (SKU) Ä‘Æ°á»£c thÃªm vÃ o giá».
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
     * Sá»‘ lÆ°á»£ng sáº£n pháº©m trong giá» (DEFAULT 1).
     * CHECK constraint á»Ÿ DB: so_luong > 0.
     */
    @Column(name = "so_luong", nullable = false)
    @Builder.Default
    private Integer soLuong = 1;

    /** Thá»i Ä‘iá»ƒm thÃªm vÃ o giá» hÃ ng. */
    @Column(name = "ngay_them", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayThem = LocalDateTime.now();
}
