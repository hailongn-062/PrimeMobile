package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping báº£ng hang_san_xuat (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * Quáº£n lÃ½ thÆ°Æ¡ng hiá»‡u Ä‘iá»‡n thoáº¡i (Apple, Samsung, Xiaomi...).
 * <p>
 * Quan há»‡: 1 HangSanXuat â†’ N SanPham.
 */
@Entity
@Table(
        name = "hang_san_xuat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_hsx_ten", columnNames = "ten_hang")
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
public class HangSanXuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** TÃªn thÆ°Æ¡ng hiá»‡u â€“ duy nháº¥t (vÃ­ dá»¥: "Apple", "Samsung"). */
    @Column(name = "ten_hang", nullable = false, length = 100)
    private String tenHang;

    /** ÄÆ°á»ng dáº«n logo thÆ°Æ¡ng hiá»‡u (lÆ°u URL hoáº·c Ä‘Æ°á»ng dáº«n tÆ°Æ¡ng Ä‘á»‘i). */
    @Column(name = "logo", length = 255)
    private String logo;

    /** Quá»‘c gia sáº£n xuáº¥t (vÃ­ dá»¥: "Má»¹", "HÃ n Quá»‘c"). */
    @Column(name = "quoc_gia", length = 50)
    private String quocGia;

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 HangSanXuat â†’ nhiá»u SanPham
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "hangSanXuat", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SanPham> sanPhams = new ArrayList<>();
}
