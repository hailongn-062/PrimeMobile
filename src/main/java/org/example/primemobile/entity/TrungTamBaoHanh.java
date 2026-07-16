package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng trung_tam_bao_hanh (Module 8: Báº£o hÃ nh).
 * <p>
 * Quáº£n lÃ½ danh sÃ¡ch trung tÃ¢m báº£o hÃ nh / hÃ£ng mÃ  cá»­a hÃ ng gá»­i mÃ¡y lá»—i Ä‘áº¿n.
 * Thay tháº¿ vai trÃ² cá»§a NhaCungCap trong luá»“ng báº£o hÃ nh.
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_ttbh_trang_thai):
 *  "hoat_dong" | "ngung_hoat_dong"
 * <p>
 * Quan há»‡:
 *  - 1-N vá»›i {@link YeuCauBaoHanh} (mappedBy trungTamBaoHanh)
 */
@Entity
@Table(name = "trung_tam_bao_hanh")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "yeuCauBaoHanhs")
@EqualsAndHashCode(exclude = "yeuCauBaoHanhs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TrungTamBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** TÃªn trung tÃ¢m báº£o hÃ nh (vÃ­ dá»¥: "AASC", "Samsung Service Center"). */
    @Column(name = "ten_trung_tam", nullable = false, length = 200)
    private String tenTrungTam;

    /** Sá»‘ Ä‘iá»‡n thoáº¡i liÃªn há»‡ cá»§a trung tÃ¢m. */
    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    /** Äá»‹a chá»‰ trung tÃ¢m báº£o hÃ nh. */
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    /** TÃªn ngÆ°á»i liÃªn há»‡ Ä‘áº§u má»‘i táº¡i trung tÃ¢m. */
    @Column(name = "nguoi_lien_he", length = 100)
    private String nguoiLienHe;

    /**
     * Tráº¡ng thÃ¡i hoáº¡t Ä‘á»™ng (DEFAULT 'hoat_dong').
     * GiÃ¡ trá»‹ há»£p lá»‡: "hoat_dong" | "ngung_hoat_dong"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "hoat_dong";

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 TrungTamBaoHanh â†’ nhiá»u YeuCauBaoHanh
    // -------------------------------------------------------------------------
    @com.fasterxml.jackson.annotation.JsonIgnore
    @OneToMany(mappedBy = "trungTamBaoHanh", fetch = FetchType.LAZY)
    @Builder.Default
    private List<YeuCauBaoHanh> yeuCauBaoHanhs = new ArrayList<>();
}
