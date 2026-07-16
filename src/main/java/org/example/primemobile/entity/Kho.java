package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng kho (Module 3: Kho hÃ ng).
 * <p>
 * Há»‡ thá»‘ng PrimeMobile cÃ³ nhiá»u kho váº­t lÃ½ Ä‘á»ƒ quáº£n lÃ½ tá»“n kho.
 * <p>
 * Quan há»‡:
 * - 1-N vá»›i {@link TonKho} (mappedBy kho)
 * - 1-N vá»›i {@link PhieuNhapKho} (mappedBy kho)
 */
@Entity
@Table(name = "kho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "tonKhos", "phieuNhapKhos" })
@EqualsAndHashCode(exclude = { "tonKhos", "phieuNhapKhos" })
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Kho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** TÃªn kho hiá»ƒn thá»‹ (vÃ­ dá»¥: "Kho Tá»•ng", "Kho Online"). */
    @Column(name = "ten_kho", nullable = false, length = 100)
    private String tenKho;

    /** Äá»‹a chá»‰ váº­t lÃ½ cá»§a kho. NULL vá»›i kho_online. */
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    /** Tráº¡ng thÃ¡i hoáº¡t Ä‘á»™ng cá»§a kho (DEFAULT true). */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 Kho â†’ nhiá»u TonKho (báº£n ghi tá»“n kho tá»«ng SKU trong kho nÃ y)
    // -------------------------------------------------------------------------
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "kho", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TonKho> tonKhos = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 Kho â†’ nhiá»u PhieuNhapKho Ä‘Æ°á»£c nháº­p vÃ o kho nÃ y
    // -------------------------------------------------------------------------
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "kho", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuNhapKho> phieuNhapKhos = new ArrayList<>();
}
