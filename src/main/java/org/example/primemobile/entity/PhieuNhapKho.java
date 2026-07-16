package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng phieu_nhap_kho (Module 3: Kho hÃ ng).
 * <p>
 * Ghi nháº­n nghiá»‡p vá»¥ nháº­p hÃ ng tá»« NhÃ  cung cáº¥p vÃ o Kho Tá»•ng.
 * Khi phiáº¿u nháº­p cÃ³ tráº¡ng thÃ¡i 'hoan_thanh', Service Layer pháº£i tá»± Ä‘á»™ng
 * cá»™ng sá»‘ lÆ°á»£ng vÃ o báº£ng ton_kho cá»§a kho_tong tÆ°Æ¡ng á»©ng.
 * <p>
 * Luá»“ng nghiá»‡p vá»¥ (system_rules.md Â§3.2):
 *  NCC â†’ phieu_nhap_kho (hoan_thanh) â†’ ton_kho (kho_tong) tÄƒng lÃªn.
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_pnk_trang_thai): "hoan_thanh" | "huy"
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link Kho}               (FK kho_id â€” luÃ´n lÃ  kho_tong)
 *  - N:1 vá»›i {@link NhaCungCap}        (FK nha_cung_cap_id, nullable â€” refactored tá»« Module 4)
 *  - N:1 vá»›i {@link NguoiDung}         (FK nguoi_tao_id)
 *  - 1-N vá»›i {@link ChiTietPhieuNhap} (mappedBy phieuNhapKho, CASCADE DELETE)
 */
@Entity
@Table(
        name = "phieu_nhap_kho",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pnk_ma", columnNames = "ma_phieu")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "chiTietPhieuNhaps")
@EqualsAndHashCode(exclude = "chiTietPhieuNhaps")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhieuNhapKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** MÃ£ phiáº¿u nháº­p â€“ duy nháº¥t (vÃ­ dá»¥: "PNK-2024-001"). */
    @Column(name = "ma_phieu", nullable = false, length = 50, unique = true)
    private String maPhieu;

    /**
     * Kho nháº­n hÃ ng (luÃ´n lÃ  Kho Tá»•ng theo nghiá»‡p vá»¥).
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnk_kho")
    )
    private Kho kho;

    /**
     * NhÃ  cung cáº¥p cung á»©ng lÃ´ hÃ ng nÃ y.
     * Nullable â€” phiáº¿u nháº­p váº«n há»£p lá»‡ khi khÃ´ng xÃ¡c Ä‘á»‹nh Ä‘Æ°á»£c NCC.
     * FK fk_pnk_ncc tÆ°Æ¡ng á»©ng vá»›i ALTER TABLE trong SQL (Module 4).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nha_cung_cap_id",
            foreignKey = @ForeignKey(name = "fk_pnk_ncc")
    )
    private NhaCungCap nhaCungCap;

    /**
     * NhÃ¢n viÃªn / Admin táº¡o phiáº¿u nháº­p.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "nguoi_tao_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnk_nd")
    )
    private NguoiDung nguoiTao;

    /** NgÃ y giá» nháº­p hÃ ng thá»±c táº¿ (DEFAULT GETDATE()). */
    @Column(name = "ngay_nhap", nullable = false)
    @Builder.Default
    private LocalDateTime ngayNhap = LocalDateTime.now();

    /** Tá»•ng tiá»n cá»§a phiáº¿u nháº­p (tá»± cá»™ng tá»« cÃ¡c dÃ²ng chi tiáº¿t). */
    @Column(name = "tong_tien", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tongTien = BigDecimal.ZERO;

    /**
     * Tráº¡ng thÃ¡i phiáº¿u nháº­p (DEFAULT 'hoan_thanh').
     * Há»‡ thá»‘ng chá»‘t luÃ´n khi táº¡o phiáº¿u â€” khÃ´ng qua bÆ°á»›c chá» duyá»‡t (system_rules.md Â§3.2).
     * GiÃ¡ trá»‹ há»£p lá»‡: "hoan_thanh" | "huy"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "hoan_thanh";

    /** Ghi chÃº ná»™i bá»™ vá» lÃ´ hÃ ng nháº­p. */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 PhieuNhapKho â†’ nhiá»u ChiTietPhieuNhap (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phieuNhapKho", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietPhieuNhap> chiTietPhieuNhaps = new ArrayList<>();
}
