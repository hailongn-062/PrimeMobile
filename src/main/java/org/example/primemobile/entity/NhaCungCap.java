package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng nha_cung_cap (Module 4: NhÃ  cung cáº¥p).
 * <p>
 * Quáº£n lÃ½ danh sÃ¡ch nhÃ  cung cáº¥p Ä‘iá»‡n thoáº¡i cho cá»­a hÃ ng PrimeMobile.
 * <p>
 * Ghi chÃº (system_rules.md Â§3.2):
 *  Workflow Ä‘áº·t hÃ ng NCC (purchase order) Ä‘Ã£ bá»‹ loáº¡i bá».
 *  Entity nÃ y chá»‰ dÃ¹ng Ä‘á»ƒ tham chiáº¿u trong {@link PhieuNhapKho}
 *  vÃ  {@link YeuCauBaoHanh} (khi gá»­i mÃ¡y lá»—i cho NCC xá»­ lÃ½).
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_ncc_trang_thai):
 *  "dang_hop_tac" | "ngung_hop_tac"
 * <p>
 * Quan há»‡:
 *  - 1-N vá»›i {@link PhieuNhapKho} (mappedBy nhaCungCap)
 */
@Entity
@Table(
        name = "nha_cung_cap",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ncc_ma", columnNames = "ma_ncc")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "phieuNhapKhos")
@EqualsAndHashCode(exclude = "phieuNhapKhos")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NhaCungCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * MÃ£ nhÃ  cung cáº¥p ná»™i bá»™ â€“ duy nháº¥t (vÃ­ dá»¥: "NCC-001").
     * NOT NULL, UNIQUE.
     */
    @Column(name = "ma_ncc", nullable = false, length = 20, unique = true)
    private String maNcc;

    /** TÃªn Ä‘áº§y Ä‘á»§ cá»§a nhÃ  cung cáº¥p. */
    @Column(name = "ten_ncc", nullable = false, length = 200)
    private String tenNcc;

    /** Sá»‘ Ä‘iá»‡n thoáº¡i liÃªn há»‡ cá»§a nhÃ  cung cáº¥p. */
    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    /** Email liÃªn há»‡ cá»§a nhÃ  cung cáº¥p. */
    @Column(name = "email", length = 100)
    private String email;

    /** Äá»‹a chá»‰ trá»¥ sá»Ÿ / kho cá»§a nhÃ  cung cáº¥p. */
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    /** TÃªn ngÆ°á»i liÃªn há»‡ Ä‘áº§u má»‘i táº¡i nhÃ  cung cáº¥p. */
    @Column(name = "nguoi_lien_he", length = 100)
    private String nguoiLienHe;

    /**
     * Tráº¡ng thÃ¡i há»£p tÃ¡c (DEFAULT 'dang_hop_tac').
     * GiÃ¡ trá»‹ há»£p lá»‡: "dang_hop_tac" | "ngung_hop_tac"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "dang_hop_tac";

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 NhaCungCap â†’ nhiá»u PhieuNhapKho
    // -------------------------------------------------------------------------
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "nhaCungCap", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuNhapKho> phieuNhapKhos = new ArrayList<>();
}
