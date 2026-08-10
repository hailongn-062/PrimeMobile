package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng chuong_trinh_khuyen_mai (Module 9: Khuyáº¿n mÃ£i).
 * <p>
 * Quáº£n lÃ½ cÃ¡c chÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i cá»§a cá»­a hÃ ng.
 * <p>
 * PhÃ¢n loáº¡i (CHECK chk_ctkm_loai):
 * "theo_don_hang" â€” Giáº£m giÃ¡ khi Ä‘Æ¡n hÃ ng Ä‘áº¡t Ä‘iá»u kiá»‡n tá»‘i thiá»ƒu.
 * "theo_san_pham"  â€” Giáº£m giÃ¡ theo sáº£n pháº©m cá»¥ thá»ƒ (xem pham_vi_khuyen_mai).
 * <p>
 * Tráº¡ng thÃ¡i (CHECK chk_ctkm_trang_thai):
 * "chua_bat_dau" | "dang_dien_ra" | "da_ket_thuc" | "tam_dung"
 * <p>
 * Quan há»‡:
 * - 1-N vá»›i {@link PhamViKhuyenMai} (mappedBy chuongTrinhKhuyenMai, CASCADE ALL)
 */
@Entity
@Table(name = "chuong_trinh_khuyen_mai", indexes = {
        @Index(name = "idx_ctkm_tts", columnList = "trang_thai, ngay_bat_dau, ngay_ket_thuc")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "phamViKhuyenMais")
@EqualsAndHashCode(exclude = "phamViKhuyenMais")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ChuongTrinhKhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** TÃªn chÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i. */
    @Column(name = "ten_ctkm", nullable = false, length = 200)
    private String tenCtkm;

    /** MÃ´ táº£ chi tiáº¿t vá» chÆ°Æ¡ng trÃ¬nh. */
    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    /**
     * Loáº¡i khuyáº¿n mÃ£i (NOT NULL).
     * GiÃ¡ trá»‹ há»£p lá»‡: "theo_don_hang" | "theo_san_pham"
     */
    @Column(name = "loai", nullable = false, length = 25)
    private String loai;

    /**
     * GiÃ¡ trá»‹ Æ°u Ä‘Ã£i (sá»‘ tiá»n hoáº·c %).
     * GiÃ¡ trá»‹ Æ°u Ä‘Ã£i (sá»‘ tiá» n hoáº·c %).
     * NOT NULL.
     */
    @Column(name = "gia_tri_uu_dai", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaTriUuDai;

    /**
     * Đơn hàng tối thiểu để áp dụng khuyến mãi.
     * NULL = không có điều kiện tối thiểu.
     */
    @Column(name = "don_hang_toi_thieu", precision = 15, scale = 2)
    private BigDecimal donHangToiThieu;

    /**
     * Giới hạn mức giảm tối đa (VNĐ).
     * NULL = Không giới hạn.
     */
    @Column(name = "giam_toi_da", precision = 15, scale = 2)
    private BigDecimal giamToiDa;

    /** NgÃ y giá»  báº¯t Ä‘áº§u chÆ°Æ¡ng trÃ¬nh. */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    /** NgÃ y giá»  káº¿t thÃºc chÆ°Æ¡ng trÃ¬nh. */
    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    /**
     * Tráº¡ng thÃ¡i chÆ°Æ¡ng trÃ¬nh (DEFAULT 'chua_bat_dau').
     * GiÃ¡ trá»‹ há»£p lá»‡: "chua_bat_dau" | "dang_dien_ra" | "da_ket_thuc" | "tam_dung"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "chua_bat_dau";

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N
    // -------------------------------------------------------------------------

    /** Danh sÃ¡ch pháº¡m vi Ã¡p dá»¥ng (sáº£n pháº©m). */
    @OneToMany(mappedBy = "chuongTrinhKhuyenMai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhamViKhuyenMai> phamViKhuyenMais = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Helper Methods
    // -------------------------------------------------------------------------
    
    /**
     * TÃ­nh giÃ¡ sau giáº£m dá»±a trÃªn giÃ¡ gá»‘c vÃ  cÃ¡c quy táº¯c cá»§a CTKM (loáº¡i, giÃ¡ trá»‹, giáº£m tá»‘i Ä‘a).
     * @param giaGoc GiÃ¡ bÃ¡n cá»§a sáº£n pháº©m/biáº¿n thá»ƒ
     * @return GiÃ¡ sau khi Ä‘Ã£ trá»« khuyáº¿n mÃ£i
     */
    public BigDecimal tinhGiaSauKhuyenMai(BigDecimal giaGoc) {
        if (giaGoc == null || this.giaTriUuDai == null) {
            return giaGoc;
        }

        // Trong há»‡ thá»‘ng cá»§a PrimeMobile, giaTriUuDai luÃ´n lÃ  Pháº§n TrÄƒm (%).
        BigDecimal mucGiam = giaGoc.multiply(this.giaTriUuDai).divide(BigDecimal.valueOf(100), 0, java.math.RoundingMode.HALF_UP);

        // Náº¿u cÃ³ giá»›i háº¡n giáº£m tá»‘i Ä‘a thÃ¬ Ã¡p dá»¥ng
        if (this.giamToiDa != null && mucGiam.compareTo(this.giamToiDa) > 0) {
            mucGiam = this.giamToiDa;
        }

        BigDecimal giaSauGiam = giaGoc.subtract(mucGiam);
        return giaSauGiam.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : giaSauGiam;
    }
}
