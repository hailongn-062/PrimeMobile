package org.example.primemobile.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping báº£ng danh_muc (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * PrimeMobile chá»‰ bÃ¡n Ä‘iá»‡n thoáº¡i, nÃªn danh má»¥c Ä‘Æ°á»£c dÃ¹ng Ä‘á»ƒ phÃ¢n loáº¡i
 * dÃ²ng mÃ¡y (vÃ­ dá»¥: Android cao cáº¥p, iPhone, Flagship, Mid-range...).
 * <p>
 * Quan há»‡: 1 DanhMuc â†’ N SanPham.
 */
@Entity
@Table(
        name = "danh_muc",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dm_ten",  columnNames = "ten_danh_muc"),
                @UniqueConstraint(name = "uq_dm_slug", columnNames = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sanPhams")
@EqualsAndHashCode(exclude = "sanPhams")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** TÃªn danh má»¥c â€“ duy nháº¥t trong há»‡ thá»‘ng. */
    @Column(name = "ten_danh_muc", nullable = false, length = 100)
    private String tenDanhMuc;

    /**
     * Slug URL-friendly cho SEO (vÃ­ dá»¥: "dien-thoai-android").
     * Duy nháº¥t trong há»‡ thá»‘ng.
     */
    @Column(name = "slug", nullable = false, length = 100, unique = true)
    private String slug;

    /** MÃ´ táº£ danh má»¥c, cÃ³ thá»ƒ NULL. */
    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    /** Thá»© tá»± hiá»ƒn thá»‹ trÃªn giao diá»‡n. */
    @Column(name = "thu_tu")
    private Integer thuTu;

    /**
     * Tráº¡ng thÃ¡i kÃ­ch hoáº¡t (DEFAULT 1 = Ä‘ang hoáº¡t Ä‘á»™ng).
     * true = Ä‘ang hiá»ƒn thá»‹ | false = Ä‘Ã£ áº©n.
     */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 DanhMuc â†’ nhiá»u SanPham
    // @ToString.Exclude & @EqualsAndHashCode.Exclude trÃªn class Ä‘á»ƒ trÃ¡nh Ä‘á»‡ quy
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "danhMuc", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SanPham> sanPhams = new ArrayList<>();
}
