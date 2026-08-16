package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping báº£ng thong_so_ky_thuat (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * LÆ°u trá»¯ thÃ´ng sá»‘ ká»¹ thuáº­t dáº¡ng key-value theo tá»«ng nhÃ³m cá»§a má»™t sáº£n pháº©m.
 * VÃ­ dá»¥: NhÃ³m "MÃ n hÃ¬nh" â†’ "KÃ­ch thÆ°á»›c" â†’ "6.7 inch".
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link SanPham} (FK san_pham_id, ON DELETE CASCADE)
 * <p>
 * ThÃ´ng sá»‘ ká»¹ thuáº­t gáº¯n vá»›i SanPham (model), khÃ´ng pháº£i BienTheSanPham,
 * vÃ¬ cÃ¡c thÃ´ng sá»‘ nhÆ° CPU, mÃ n hÃ¬nh, camera thÆ°á»ng giá»‘ng nhau giá»¯a cÃ¡c biáº¿n thá»ƒ.
 * ThÃ´ng sá»‘ riÃªng cá»§a tá»«ng biáº¿n thá»ƒ (RAM, ROM, pin) Ä‘Ã£ cÃ³ cá»™t riÃªng trong bien_the_san_pham.
 */
@Entity
@Table(
        name = "thong_so_ky_thuat",
        indexes = {
                @Index(name = "idx_tskt_sp", columnList = "san_pham_id, nhom")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ThongSoKyThuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sáº£n pháº©m mÃ  thÃ´ng sá»‘ nÃ y thuá»™c vá».
     * ON DELETE CASCADE â€“ xÃ³a sáº£n pháº©m thÃ¬ thÃ´ng sá»‘ tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tskt_sp")
    )
    private SanPham sanPham;

    /**
     * NhÃ³m thÃ´ng sá»‘ ká»¹ thuáº­t (DEFAULT 'ThÃ´ng tin chung').
     * VÃ­ dá»¥: "MÃ n hÃ¬nh", "Camera", "Hiá»‡u nÄƒng", "Káº¿t ná»‘i", "Pin & Sáº¡c".
     */
    @Column(name = "nhom", nullable = false, length = 100)
    @Builder.Default
    private String nhom = "ThÃ´ng tin chung";

    /**
     * TÃªn thÃ´ng sá»‘ ká»¹ thuáº­t.
     * VÃ­ dá»¥: "KÃ­ch thÆ°á»›c mÃ n hÃ¬nh", "Äá»™ phÃ¢n giáº£i", "CPU".
     */
    @Column(name = "ten_thong_so", nullable = false, length = 100)
    private String tenThongSo;

    /**
     * Giá trị của thông số.
     * Có thể chứa xuống dòng, tối đa 1000 ký tự.
     */
    @Column(name = "gia_tri", nullable = false, length = 1000)
    private String giaTri;

    /** Thá»© tá»± hiá»ƒn thá»‹ trong nhÃ³m (DEFAULT 0). */
    @Column(name = "thu_tu", nullable = false)
    @Builder.Default
    private Integer thuTu = 0;
}
