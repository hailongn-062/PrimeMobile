package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng yeu_cau_bao_hanh (Module 8: Báº£o hÃ nh).
 * <p>
 * Gá»™p thÃ´ng tin yÃªu cáº§u báº£o hÃ nh vÃ  thÃ´ng tin gá»­i trung tÃ¢m báº£o hÃ nh (TTBH)
 * vÃ o cÃ¹ng 1 báº£ng.
 * Luá»“ng xá»­ lÃ½:
 * Nháº­n mÃ¡y tá»« khÃ¡ch â†’ [Sá»­a táº¡i cá»­a hÃ ng / Gá»­i TTBH] â†’ Nháº­n láº¡i â†’ Tráº£ khÃ¡ch.
 * <p>
 * HÃ¬nh thá»©c xá»­ lÃ½ (CHECK chk_ycbh_hinh_thuc):
 * "sua_chua" | "doi_moi" | "hoan_tien"
 * <p>
 * Tráº¡ng thÃ¡i yÃªu cáº§u (CHECK chk_ycbh_trang_thai):
 * "tiep_nhan" | "dang_kiem_tra" | "da_gui_ttbh" | "ttbh_dang_xu_ly"
 * | "da_nhan_lai_ttbh" | "cho_tra_khach" | "da_tra_khach" | "tu_choi"
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link PhieuBaoHanh}     (FK phieu_bao_hanh_id)
 * - N:1 vá»›i {@link NguoiDung}        (FK nguoi_tiep_nhan_id, ON DELETE SET NULL, nullable)
 * - N:1 vá»›i {@link TrungTamBaoHanh}  (FK trung_tam_bao_hanh_id, nullable â€” khi gá»­i TTBH)
 */
@Entity
@Table(name = "yeu_cau_bao_hanh", indexes = {
        @Index(name = "idx_ycbh_pbh", columnList = "phieu_bao_hanh_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_ycbh_ma", columnNames = "ma_yeu_cau")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class YeuCauBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** MÃ£ yÃªu cáº§u báº£o hÃ nh â€“ duy nháº¥t (vÃ­ dá»¥: "YCBH-2024-001"). */
    @Column(name = "ma_yeu_cau", nullable = false, length = 50)
    private String maYeuCau;

    /**
     * Phiáº¿u báº£o hÃ nh liÃªn káº¿t vá»›i yÃªu cáº§u nÃ y.
     * NOT NULL â€“ yÃªu cáº§u báº£o hÃ nh pháº£i cÃ³ phiáº¿u báº£o hÃ nh há»£p lá»‡.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_bao_hanh_id", nullable = false, foreignKey = @ForeignKey(name = "fk_ycbh_pbh"))
    private PhieuBaoHanh phieuBaoHanh;

    /**
     * NhÃ¢n viÃªn tiáº¿p nháº­n yÃªu cáº§u báº£o hÃ nh.
     * NULL náº¿u chÆ°a cÃ³ nhÃ¢n viÃªn nháº­n.
     * ON DELETE SET NULL â€“ xÃ³a nhÃ¢n viÃªn khÃ´ng áº£nh hÆ°á»Ÿng lá»‹ch sá»­.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tiep_nhan_id", foreignKey = @ForeignKey(name = "fk_ycbh_nd"))
    private NguoiDung nguoiTiepNhan;

    /** NgÃ y giá» tiáº¿p nháº­n mÃ¡y tá»« khÃ¡ch (DEFAULT GETDATE()). */
    @Column(name = "ngay_tiep_nhan", nullable = false)
    @Builder.Default
    private LocalDateTime ngayTiepNhan = LocalDateTime.now();

    /** MÃ´ táº£ lá»—i / hiá»‡n tÆ°á»£ng khÃ¡ch mÃ´ táº£ khi mang mÃ¡y Ä‘áº¿n. */
    @Column(name = "mo_ta_loi", columnDefinition = "NVARCHAR(MAX)")
    private String moTaLoi;

    /**
     * HÃ¬nh thá»©c xá»­ lÃ½ báº£o hÃ nh.
     * GiÃ¡ trá»‹ há»£p lá»‡: "sua_chua" | "doi_moi" | "hoan_tien"
     */
    @Column(name = "hinh_thuc", nullable = false, length = 15)
    private String hinhThuc;

    /**
     * Tráº¡ng thÃ¡i xá»­ lÃ½ (DEFAULT 'tiep_nhan').
     * GiÃ¡ trá»‹ há»£p lá»‡: "tiep_nhan" | "dang_kiem_tra" | "da_gui_ttbh"
     * | "ttbh_dang_xu_ly" | "da_nhan_lai_ttbh" | "cho_tra_khach"
     * | "da_tra_khach" | "tu_choi"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "tiep_nhan";

    // -------------------------------------------------------------------------
    // THÃ”NG TIN Gá»¬I TRUNG TÃ‚M Báº¢O HÃ€NH (TTBH)
    // -------------------------------------------------------------------------

    /**
     * Trung tÃ¢m báº£o hÃ nh nháº­n mÃ¡y Ä‘á»ƒ sá»­a.
     * NULL náº¿u cá»­a hÃ ng tá»± sá»­a, khÃ´ng gá»­i TTBH.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trung_tam_bao_hanh_id", foreignKey = @ForeignKey(name = "fk_ycbh_ttbh"))
    private TrungTamBaoHanh trungTamBaoHanh;

    /** NgÃ y gá»­i mÃ¡y sang TTBH. NULL náº¿u chÆ°a gá»­i. */
    @Column(name = "ngay_gui_ttbh")
    private LocalDateTime ngayGuiTtbh;

    /** NgÃ y dá»± kiáº¿n nháº­n láº¡i tá»« TTBH. */
    @Column(name = "ngay_du_kien_nhan")
    private LocalDate ngayDuKienNhan;

    /** NgÃ y thá»±c táº¿ nháº­n láº¡i mÃ¡y tá»« TTBH. NULL náº¿u chÆ°a nháº­n. */
    @Column(name = "ngay_nhan_lai_ttbh")
    private LocalDateTime ngayNhanLaiTtbh;

    /** Káº¿t quáº£ xá»­ lÃ½ tá»« TTBH (bÃ¡o cÃ¡o ká»¹ thuáº­t, mÃ´ táº£ thay tháº¿...). */
    @Column(name = "ket_qua_ttbh", columnDefinition = "NVARCHAR(MAX)")
    private String ketQuaTtbh;

    // -------------------------------------------------------------------------
    // TRáº¢ KHÃCH
    // -------------------------------------------------------------------------

    /** NgÃ y giá» tráº£ mÃ¡y láº¡i cho khÃ¡ch. NULL náº¿u chÆ°a tráº£. */
    @Column(name = "ngay_tra_khach")
    private LocalDateTime ngayTraKhach;

    /** Ghi chÃº ná»™i bá»™ vá» quÃ¡ trÃ¬nh xá»­ lÃ½. */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;
}
