package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping báº£ng hinh_anh_san_pham (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * LÆ°u trá»¯ cÃ¡c áº£nh cá»§a tá»«ng biáº¿n thá»ƒ sáº£n pháº©m.
 * Má»—i biáº¿n thá»ƒ cÃ³ thá»ƒ cÃ³ nhiá»u áº£nh, trong Ä‘Ã³ cÃ³ duy nháº¥t 1 áº£nh chÃ­nh.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link BienTheSanPham} (FK bien_the_san_pham_id, ON DELETE CASCADE)
 */
@Entity
@Table(name = "hinh_anh_san_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m mÃ  áº£nh nÃ y thuá»™c vá».
     * ON DELETE CASCADE â€“ xÃ³a biáº¿n thá»ƒ thÃ¬ áº£nh tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hasp_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * ÄÆ°á»ng dáº«n áº£nh (URL tuyá»‡t Ä‘á»‘i hoáº·c Ä‘Æ°á»ng dáº«n tÆ°Æ¡ng Ä‘á»‘i tá»« static/).
     * VÃ­ dá»¥: "/images/products/ip15pm-titan-black-1.jpg"
     */
    @Column(name = "duong_dan", nullable = false, length = 255)
    private String duongDan;

    /**
     * ÄÃ¡nh dáº¥u Ä‘Ã¢y cÃ³ pháº£i áº£nh Ä‘áº¡i diá»‡n chÃ­nh cá»§a biáº¿n thá»ƒ khÃ´ng.
     * DEFAULT false. Má»—i biáº¿n thá»ƒ chá»‰ nÃªn cÃ³ 1 áº£nh chÃ­nh.
     */
    @Column(name = "la_anh_chinh", nullable = false)
    @Builder.Default
    private Boolean laAnhChinh = false;

    /** Thá»© tá»± hiá»ƒn thá»‹ trong gallery (DEFAULT 0 = hiá»ƒn thá»‹ Ä‘áº§u tiÃªn). */
    @Column(name = "thu_tu", nullable = false)
    @Builder.Default
    private Integer thuTu = 0;
}
