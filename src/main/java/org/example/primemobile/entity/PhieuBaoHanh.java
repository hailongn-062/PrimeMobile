package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity mapping báº£ng phieu_bao_hanh (Module 8: Báº£o hÃ nh).
 * <p>
 * Phiáº¿u báº£o hÃ nh Ä‘Æ°á»£c cáº¥p tá»± Ä‘á»™ng khi Ä‘Æ¡n hÃ ng chuyá»ƒn sang tráº¡ng thÃ¡i 'da_hoan_thanh'.
 * Má»—i mÃ¡y váº­t lÃ½ ({@link MayDienThoai}) chá»‰ cÃ³ tá»‘i Ä‘a 1 phiáº¿u báº£o hÃ nh
 * (rÃ ng buá»™c UNIQUE trÃªn may_dien_thoai_id).
 * <p>
 * Luá»“ng báº£o hÃ nh (system_rules.md Â§5):
 *  Nháº­n tá»« khÃ¡ch â†’ kiá»ƒm tra IMEI â†’ tra cá»©u phiáº¿u báº£o hÃ nh há»£p lá»‡ cÃ²n háº¡n
 *  (trang_thai = 'con_hieu_luc') â†’ táº¡o {@link YeuCauBaoHanh}.
 * <p>
 * Ghi chÃº: TÃ­nh nÄƒng báº£o hÃ nh táº¡m hoÃ£n triá»ƒn khai (system_rules.md Â§7),
 * nhÆ°ng Entity váº«n Ä‘Æ°á»£c táº¡o Ä‘áº§y Ä‘á»§ Ä‘á»ƒ mapping Ä‘Ãºng DB.
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_pbh_trang_thai):
 *  "con_hieu_luc" | "het_han" | "da_su_dung" | "void"
 * <p>
 * Quan há»‡:
 *  - 1:1 vá»›i {@link MayDienThoai}   (FK may_dien_thoai_id, UNIQUE â€” 1 mÃ¡y 1 phiáº¿u)
 *  - N:1 vá»›i {@link KhachHang}      (FK khach_hang_id)
 *  - N:1 vá»›i {@link DonHang}        (FK don_hang_id)
 *  - 1-N vá»›i {@link YeuCauBaoHanh}  (mappedBy phieuBaoHanh)
 */
@Entity
@Table(
        name = "phieu_bao_hanh",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pbh_ma",  columnNames = "ma_phieu"),
                @UniqueConstraint(name = "uq_pbh_may", columnNames = "may_dien_thoai_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "yeuCauBaoHanhs")
@EqualsAndHashCode(exclude = "yeuCauBaoHanhs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhieuBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** MÃ£ phiáº¿u báº£o hÃ nh â€“ duy nháº¥t (vÃ­ dá»¥: "BH-2024-001"). */
    @Column(name = "ma_phieu", nullable = false, length = 50)
    private String maPhieu;

    /**
     * MÃ¡y Ä‘iá»‡n thoáº¡i váº­t lÃ½ Ä‘Æ°á»£c báº£o hÃ nh.
     * UNIQUE â€“ má»—i mÃ¡y chá»‰ cÃ³ Ä‘Ãºng 1 phiáº¿u báº£o hÃ nh.
     * NOT NULL.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "may_dien_thoai_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_pbh_may")
    )
    private MayDienThoai mayDienThoai;

    /**
     * KhÃ¡ch hÃ ng Ä‘Æ°á»£c cáº¥p phiáº¿u báº£o hÃ nh nÃ y.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pbh_kh")
    )
    private KhachHang khachHang;

    /**
     * ÄÆ¡n hÃ ng phÃ¡t sinh phiáº¿u báº£o hÃ nh nÃ y.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pbh_dh")
    )
    private DonHang donHang;

    /** Thá»i háº¡n báº£o hÃ nh tÃ­nh báº±ng sá»‘ thÃ¡ng (láº¥y tá»« san_pham.bao_hanh_thang). */
    @Column(name = "so_thang_bao_hanh", nullable = false)
    private Integer soThangBaoHanh;

    /** NgÃ y báº¯t Ä‘áº§u hiá»‡u lá»±c báº£o hÃ nh (thÆ°á»ng = ngÃ y giao hÃ ng thá»±c táº¿). */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    /** NgÃ y háº¿t háº¡n báº£o hÃ nh = ngayBatDau + soThangBaoHanh. */
    @Column(name = "ngay_het_han", nullable = false)
    private LocalDate ngayHetHan;

    /**
     * Tráº¡ng thÃ¡i phiáº¿u báº£o hÃ nh (DEFAULT 'con_hieu_luc').
     * GiÃ¡ trá»‹ há»£p lá»‡: "con_hieu_luc" | "het_han" | "da_su_dung" | "void"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "con_hieu_luc";

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 PhieuBaoHanh â†’ nhiá»u YeuCauBaoHanh
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phieuBaoHanh", fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<YeuCauBaoHanh> yeuCauBaoHanhs = new java.util.ArrayList<>();
}
