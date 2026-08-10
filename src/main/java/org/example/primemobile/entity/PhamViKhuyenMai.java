package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping báº£ng pham_vi_khuyen_mai (Module 9: Khuyáº¿n mÃ£i).
 * <p>
 * XÃ¡c Ä‘á»‹nh pháº¡m vi Ã¡p dá»¥ng cá»§a má»™t chÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i.
 * CÃ³ thá»ƒ Ã¡p dá»¥ng theo 3 cáº¥p Ä‘á»™ (má»™t hoáº·c nhiá»u cÃ¹ng lÃºc):
 *  - Theo sáº£n pháº©m cá»¥ thá»ƒ ({@link SanPham})
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link ChuongTrinhKhuyenMai} (FK ctkm_id, ON DELETE CASCADE)
 *  - N:1 vá»›i {@link BienTheSanPham}      (FK bien_the_id, nullable)
 */
@Entity
@Table(name = "pham_vi_khuyen_mai")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhamViKhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ChÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i Ã¡p dá»¥ng pháº¡m vi nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a chÆ°Æ¡ng trÃ¬nh thÃ¬ pháº¡m vi tá»± xÃ³a.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ctkm_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pvkm_ctkm")
    )
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    /**
     * Biến thể sản phẩm cụ thể được áp dụng.
     * NOT NULL – mỗi bản ghi phạm vi phải gắn với 1 biến thể cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pvkm_bt")
    )
    private BienTheSanPham bienThe;
}
