package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng yeu_thich (Module 13: Wishlist).
 * <p>
 * Danh sÃ¡ch sáº£n pháº©m yÃªu thÃ­ch cá»§a khÃ¡ch hÃ ng.
 * Má»—i cáº·p (khachHang, sanPham) lÃ  DUY NHáº¤T â€” khÃ¡ch khÃ´ng thá»ƒ thÃªm
 * cÃ¹ng 1 sáº£n pháº©m vÃ o wishlist 2 láº§n.
 * <p>
 * Ghi chÃº (system_rules.md Â§7 â€“ Táº¡m hoÃ£n):
 *  TÃ­nh nÄƒng Wishlist vÃ  thuáº­t toÃ¡n Æ°u tiÃªn sáº£n pháº©m yÃªu thÃ­ch trong
 *  káº¿t quáº£ tÃ¬m kiáº¿m táº¡m thá»i Bá»Š HOÃƒN.
 *  Entity váº«n Ä‘Æ°á»£c táº¡o Ä‘á»§ Ä‘á»ƒ mapping DB.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link KhachHang} (FK khach_hang_id, ON DELETE CASCADE)
 *  - N:1 vá»›i {@link SanPham}   (FK san_pham_id)
 */
@Entity
@Table(
        name = "yeu_thich",
        uniqueConstraints = {
                // Má»—i khÃ¡ch hÃ ng chá»‰ cÃ³ thá»ƒ thÃªm 1 sáº£n pháº©m vÃ o wishlist 1 láº§n
                @UniqueConstraint(name = "uq_yt", columnNames = {"khach_hang_id", "san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class YeuThich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * KhÃ¡ch hÃ ng sá»Ÿ há»¯u má»¥c yÃªu thÃ­ch nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a khÃ¡ch hÃ ng thÃ¬ wishlist tá»± xÃ³a.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_kh")
    )
    private KhachHang khachHang;

    /**
     * Sáº£n pháº©m Ä‘Æ°á»£c yÃªu thÃ­ch.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_sp")
    )
    private SanPham sanPham;

    /** Thá»i Ä‘iá»ƒm thÃªm sáº£n pháº©m vÃ o danh sÃ¡ch yÃªu thÃ­ch. */
    @Column(name = "ngay_them", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayThem = LocalDateTime.now();
}
