package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng bien_the_san_pham (Module 2: Sáº£n pháº©m & Biáº¿n
 * thá»ƒ).
 * <p>
 * Má»—i biáº¿n thá»ƒ = 1 SKU cá»¥ thá»ƒ (mÃ u sáº¯c + RAM + ROM).
 * VÃ­ dá»¥: iPhone 15 Pro Max â€“ Titan Äen â€“ 8GB RAM â€“ 256GB.
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link SanPham} (FK san_pham_id, ON DELETE CASCADE)
 * - 1:N vá»›i {@link MayDienThoai} (mappedBy bienTheSanPham)
 * - 1:N vá»›i {@link HinhAnhSanPham} (mappedBy bienTheSanPham, CASCADE DELETE)
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_bt_trang_thai):
 * "con_hang" | "het_hang" | "ngung_kinh_doanh"
 */
@Entity
@Table(name = "bien_the_san_pham", indexes = {
                @Index(name = "idx_bt_sp", columnList = "san_pham_id, trang_thai")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uq_bt_ma_sku", columnNames = "ma_sku")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = { "mayDienThoais", "hinhAnhSanPhams" })
@EqualsAndHashCode(exclude = { "mayDienThoais", "hinhAnhSanPhams" })
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class BienTheSanPham {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * Sáº£n pháº©m cha cá»§a biáº¿n thá»ƒ nÃ y.
         * NOT NULL, ON DELETE CASCADE (DB-level).
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "san_pham_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bt_sp"))
        private SanPham sanPham;

        /**
         * MÃ£ SKU â€“ duy nháº¥t trong toÃ n há»‡ thá»‘ng (vÃ­ dá»¥:
         * "IP15PM-TIT-8-256").
         */
        @Column(name = "ma_sku", nullable = false, length = 100, unique = true)
        private String maSku;

        /**
         * Màu sắc của biến thể (thay thế cho text mau_sac và ma_mau_hex).
         * NOT NULL.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "mau_sac_id", nullable = false, foreignKey = @ForeignKey(name = "fk_bt_ms"))
        private MauSac mauSac;

        /** Dung lÆ°á»£ng RAM tÃ­nh báº±ng GB. */
        @Column(name = "ram_gb", nullable = false)
        private Integer ramGb;

        /** Dung lÆ°á»£ng lÆ°u trá»¯ tÃ­nh báº±ng GB. */
        @Column(name = "luu_tru_gb", nullable = false)
        private Integer luuTruGb;

        /**
         * Loáº¡i bá»™ nhá»› trong (DEFAULT 'UFS').
         * VÃ­ dá»¥: "UFS", "NVMe", "eMMC".
         */
        @Column(name = "loai_luu_tru", nullable = false, length = 20)
        @Builder.Default
        private String loaiLuuTru = "UFS";

        /** GiÃ¡ bÃ¡n láº» niÃªm yáº¿t. */
        @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
        private BigDecimal giaBan;

        /** Trá» ng lÆ°á»£ng mÃ¡y tÃ­nh báº±ng gram. */
        @Column(name = "trong_luong_gram")
        private Integer trongLuongGram;

        /**
         * Dung lÆ°á»£ng pin tÃ­nh báº±ng mAh.
         * TÃªn cá»™t DB: pin_mAh (giá»¯ nguyÃªn mapping).
         */
        @Column(name = "pin_mAh")
        private Integer pinMah;

        /**
         * Tráº¡ng thÃ¡i tá»“n kho cá»§a biáº¿n thá»ƒ (DEFAULT 'con_hang').
         * GiÃ¡ trá»‹ há»£p lá»‡: "con_hang" | "het_hang" | "ngung_kinh_doanh"
         */
        @Column(name = "trang_thai", nullable = false, length = 20)
        @Builder.Default
        private String trangThai = "con_hang";

        /** Thá» i Ä‘iá»ƒm táº¡o báº£n ghi. */
        @Column(name = "ngay_tao", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime ngayTao = LocalDateTime.now();

        /** Thá» i Ä‘iá»ƒm cáº­p nháº­t báº£n ghi gáº§n nháº¥t. */
        @Column(name = "updated_at", nullable = false)
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();

        @PreUpdate
        protected void onUpdate() {
                updatedAt = LocalDateTime.now();
        }

        /**
         * Helper method để lấy tên màu sắc, tránh null pointer.
         */
        public String getMauSacTen() {
                return this.mauSac != null ? this.mauSac.getTenMau() : "";
        }

        // -------------------------------------------------------------------------
        // Quan há»‡ 1-N: 1 BienTheSanPham â†’ nhiá» u MayDienThoai (IMEI tracking)
        // -------------------------------------------------------------------------
        @OneToMany(mappedBy = "bienTheSanPham", fetch = FetchType.LAZY)
        @Builder.Default
        @com.fasterxml.jackson.annotation.JsonIgnore
        private List<MayDienThoai> mayDienThoais = new ArrayList<>();

        // -------------------------------------------------------------------------
        // Quan há»‡ 1-N: 1 BienTheSanPham â†’ nhiá»u HinhAnhSanPham (ON DELETE
        // CASCADE)
        // -------------------------------------------------------------------------
        @OneToMany(mappedBy = "bienTheSanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<HinhAnhSanPham> hinhAnhSanPhams = new ArrayList<>();
}
