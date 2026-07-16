package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping báº£ng san_pham (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * PrimeMobile CHá»ˆ bÃ¡n Ä‘iá»‡n thoáº¡i â€” khÃ´ng bÃ¡n phá»¥ kiá»‡n hay cÃ¡c loáº¡i khÃ¡c.
 * Má»—i sáº£n pháº©m lÃ  má»™t model Ä‘iá»‡n thoáº¡i (vÃ­ dá»¥: iPhone 15 Pro Max).
 * CÃ¡c mÃ u sáº¯c, dung lÆ°á»£ng RAM/ROM cá»¥ thá»ƒ Ä‘Æ°á»£c quáº£n lÃ½ á»Ÿ {@link BienTheSanPham}.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link DanhMuc}  (FK danh_muc_id)
 *  - N:1 vá»›i {@link HangSanXuat} (FK hang_san_xuat_id)
 *  - 1:N vá»›i {@link BienTheSanPham} (mappedBy sanPham, CASCADE DELETE)
 *  - 1:N vá»›i {@link ThongSoKyThuat} (mappedBy sanPham, CASCADE DELETE)
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_sp_trang_thai):
 *  "dang_ban" | "ngung_ban" | "sap_ra_mat"
 */
@Entity
@Table(
        name = "san_pham",
        indexes = {
                @Index(name = "idx_sp_ten",  columnList = "ten_san_pham"),
                @Index(name = "idx_sp_dm",   columnList = "danh_muc_id, trang_thai"),
                @Index(name = "idx_sp_hang", columnList = "hang_san_xuat_id, trang_thai")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_sp_ma", columnNames = "ma_san_pham")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"bienTheSanPhams", "thongSoKyThuats"})
@EqualsAndHashCode(exclude = {"bienTheSanPhams", "thongSoKyThuats"})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** MÃ£ sáº£n pháº©m ná»™i bá»™ â€“ duy nháº¥t (vÃ­ dá»¥: "IP15PM-256"). */
    @Column(name = "ma_san_pham", nullable = false, length = 50, unique = true)
    private String maSanPham;

    /** TÃªn sáº£n pháº©m Ä‘áº§y Ä‘á»§ (vÃ­ dá»¥: "iPhone 15 Pro Max"). */
    @Column(name = "ten_san_pham", nullable = false, length = 255)
    private String tenSanPham;

    /**
     * Danh má»¥c chá»©a sáº£n pháº©m.
     * NOT NULL â€“ má»i sáº£n pháº©m Ä‘á»u pháº£i thuá»™c má»™t danh má»¥c.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "danh_muc_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_dm")
    )
    private DanhMuc danhMuc;

    /**
     * HÃ£ng sáº£n xuáº¥t cá»§a sáº£n pháº©m.
     * NOT NULL â€“ má»i sáº£n pháº©m Ä‘á»u pháº£i cÃ³ hÃ£ng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "hang_san_xuat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_hsx")
    )
    private HangSanXuat hangSanXuat;

    /** MÃ´ táº£ ngáº¯n hiá»ƒn thá»‹ trÃªn card sáº£n pháº©m (tá»‘i Ä‘a 500 kÃ½ tá»±). */
    @Column(name = "mo_ta_ngan", length = 500)
    private String moTaNgan;

    /** MÃ´ táº£ chi tiáº¿t Ä‘áº§y Ä‘á»§ (HTML content). */
    @Column(name = "mo_ta_chi_tiet", columnDefinition = "NVARCHAR(MAX)")
    private String moTaChiTiet;

    /** NÄƒm ra máº¯t sáº£n pháº©m (vÃ­ dá»¥: 2024). SMALLINT â†” Short. */
    @Column(name = "nam_ra_mat")
    private Short namRaMat;

    /** Sá»‘ thÃ¡ng báº£o hÃ nh theo chÃ­nh sÃ¡ch cá»­a hÃ ng (DEFAULT 12). */
    @Column(name = "bao_hanh_thang", nullable = false)
    @Builder.Default
    private Integer baoHanhThang = 12;

    /**
     * Tráº¡ng thÃ¡i kinh doanh cá»§a sáº£n pháº©m (DEFAULT 'dang_ban').
     * GiÃ¡ trá»‹ há»£p lá»‡: "dang_ban" | "ngung_ban" | "sap_ra_mat"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "dang_ban";

    /** LÆ°á»£t xem tÃ­ch lÅ©y (DEFAULT 0). */
    @Column(name = "luot_xem", nullable = false)
    @Builder.Default
    private Integer luotXem = 0;

    /** Thá»i Ä‘iá»ƒm táº¡o báº£n ghi. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thá»i Ä‘iá»ƒm cáº­p nháº­t báº£n ghi gáº§n nháº¥t. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 SanPham â†’ nhiá»u BienTheSanPham (ON DELETE CASCADE á»Ÿ DB)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<BienTheSanPham> bienTheSanPhams = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 SanPham â†’ nhiá»u ThongSoKyThuat (ON DELETE CASCADE á»Ÿ DB)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<ThongSoKyThuat> thongSoKyThuats = new ArrayList<>();
}
