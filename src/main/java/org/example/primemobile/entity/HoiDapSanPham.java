package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng hoi_dap_san_pham (Module 12: ÄÃ¡nh giÃ¡ & Há»i Ä‘Ã¡p).
 * <p>
 * KhÃ¡ch hÃ ng Ä‘áº·t cÃ¢u há»i vá» sáº£n pháº©m trÃªn trang chi tiáº¿t sáº£n pháº©m.
 * NhÃ¢n viÃªn / Admin tráº£ lá»i trá»±c tiáº¿p trong cÃ¹ng báº£n ghi.
 * <p>
 * Ghi chÃº (system_rules.md Â§7 â€“ Táº¡m hoÃ£n):
 *  PhÃ¢n há»‡ há»i Ä‘Ã¡p táº¡m hoÃ£n triá»ƒn khai.
 *  Entity táº¡o Ä‘á»§ Ä‘á»ƒ mapping DB.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link SanPham}    (FK san_pham_id)
 *  - N:1 vá»›i {@link KhachHang}  (FK khach_hang_id â€” ngÆ°á»i Ä‘áº·t cÃ¢u há»i)
 *  - N:1 vá»›i {@link NguoiDung}  (FK nguoi_tra_loi_id, ON DELETE SET NULL, nullable)
 */
@Entity
@Table(
        name = "hoi_dap_san_pham",
        indexes = {
                @Index(name = "idx_hdsp_sp", columnList = "san_pham_id, hien_thi")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class HoiDapSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sáº£n pháº©m Ä‘Æ°á»£c há»i.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdsp_sp")
    )
    private SanPham sanPham;

    /**
     * KhÃ¡ch hÃ ng Ä‘áº·t cÃ¢u há»i.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdsp_kh")
    )
    private KhachHang khachHang;

    /** Ná»™i dung cÃ¢u há»i cá»§a khÃ¡ch. */
    @Column(name = "cau_hoi", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String cauHoi;

    /**
     * Ná»™i dung cÃ¢u tráº£ lá»i cá»§a nhÃ¢n viÃªn/Admin.
     * NULL náº¿u chÆ°a cÃ³ ai tráº£ lá»i.
     */
    @Column(name = "tra_loi", columnDefinition = "NVARCHAR(MAX)")
    private String traLoi;

    /**
     * NhÃ¢n viÃªn / Admin tráº£ lá»i cÃ¢u há»i.
     * ON DELETE SET NULL â€“ xÃ³a nhÃ¢n viÃªn váº«n giá»¯ ná»™i dung tráº£ lá»i.
     * NULL náº¿u chÆ°a Ä‘Æ°á»£c tráº£ lá»i.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nguoi_tra_loi_id",
            foreignKey = @ForeignKey(name = "fk_hdsp_nd")
    )
    private NguoiDung nguoiTraLoi;

    /** Thá»i Ä‘iá»ƒm khÃ¡ch Ä‘áº·t cÃ¢u há»i. */
    @Column(name = "ngay_hoi", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayHoi = LocalDateTime.now();

    /**
     * Thá»i Ä‘iá»ƒm nhÃ¢n viÃªn tráº£ lá»i.
     * NULL náº¿u chÆ°a Ä‘Æ°á»£c tráº£ lá»i.
     */
    @Column(name = "ngay_tra_loi")
    private LocalDateTime ngayTraLoi;

    /**
     * Tráº¡ng thÃ¡i hiá»ƒn thá»‹ cÃ´ng khai (DEFAULT true).
     * false = áº©n khá»i trang sáº£n pháº©m (do vi pháº¡m ná»™i quy...).
     */
    @Column(name = "hien_thi", nullable = false)
    @Builder.Default
    private Boolean hienThi = true;
}
