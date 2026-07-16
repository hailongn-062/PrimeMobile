package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng may_dien_thoai (Module 2: Sáº£n pháº©m & Biáº¿n thá»ƒ).
 * <p>
 * Theo dÃµi tá»«ng chiáº¿c Ä‘iá»‡n thoáº¡i váº­t lÃ½ thÃ´ng qua mÃ£ IMEI.
 * ÄÃ¢y lÃ  Ä‘Æ¡n vá»‹ quáº£n lÃ½ nhá» nháº¥t trong chuá»—i cung á»©ng:
 * NCC â†’ Kho Tá»•ng (IMEI gáº¯n vÃ o Ä‘Ã¢y) â†’ BÃ¡n ra (linked vá»›i don_hang).
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link BienTheSanPham} (FK bien_the_san_pham_id)
 * - N:1 vá»›i {@link DonHang} (FK don_hang_id, nullable â€” khÃ´ng cÃ³ ON DELETE CASCADE,
 *   báº¯t buá»™c há»§y má»m Ä‘Æ¡n hÃ ng táº¡i Service Layer trÆ°á»›c khi "nháº£" IMEI vá» kho)
 * <p>
 * RÃ ng buá»™c nghiá»‡p vá»¥ quan trá»ng:
 * - Khi bÃ¡n hÃ ng: cáº­p nháº­t tinh_trang â†’ 'da_ban', gÃ¡n don_hang_id.
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_may_tinh_trang):
 * "trong_kho" | "da_ban" | "bao_hanh" | "loi_hong"
 * <p>
 * LÆ°u Ã½ vá» UNIQUE INDEX:
 * - imei1: UNIQUE constraint thÃ´ng thÆ°á»ng (NOT NULL, luÃ´n unique).
 * - imei2: Filtered Unique Index á»Ÿ DB â€” cho phÃ©p nhiá»u NULL, unique khi cÃ³ giÃ¡ trá»‹.
 *   KhÃ´ng map qua @UniqueConstraint trong JPA; kiá»ƒm tra trÃ¹ng táº¡i Service Layer.
 */
@Entity
@Table(name = "may_dien_thoai", indexes = {
        @Index(name = "idx_imei1", columnList = "imei1"),
        @Index(name = "idx_may_tinh_trang", columnList = "tinh_trang")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_may_imei1", columnNames = "imei1")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class MayDienThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m mÃ  chiáº¿c mÃ¡y nÃ y thuá»™c vá».
     * NOT NULL â€“ má»—i mÃ¡y váº­t lÃ½ pháº£i gáº¯n vá»›i Ä‘Ãºng 1 SKU.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bien_the_san_pham_id", nullable = false, foreignKey = @ForeignKey(name = "fk_may_bt"))
    private BienTheSanPham bienTheSanPham;

    /**
     * IMEI khe SIM 1 â€“ báº¯t buá»™c, duy nháº¥t toÃ n há»‡ thá»‘ng.
     * Chuáº©n GSMA: 15 chá»¯ sá»‘.
     */
    @Column(name = "imei1", nullable = false, length = 15, unique = true)
    private String imei1;

    /**
     * IMEI khe SIM 2 â€“ tÃ¹y chá»n (mÃ¡y 1 SIM thÃ¬ NULL).
     * Duy nháº¥t khi cÃ³ giÃ¡ trá»‹ (Ä‘áº£m báº£o bá»Ÿi Filtered Unique Index á»Ÿ DB).
     * KhÃ´ng dÃ¹ng @UniqueConstraint JPA vÃ¬ DB dÃ¹ng Filtered Index cho phÃ©p nhiá»u NULL.
     * Service Layer pháº£i kiá»ƒm tra trÃ¹ng imei2 trÆ°á»›c khi INSERT.
     */
    @Column(name = "imei2", length = 15)
    private String imei2;

    /**
     * TÃ¬nh tráº¡ng hiá»‡n táº¡i cá»§a mÃ¡y váº­t lÃ½ (DEFAULT 'trong_kho').
     * GiÃ¡ trá»‹ há»£p lá»‡: "trong_kho" | "da_ban" | "bao_hanh" | "loi_hong"
     */
    @Column(name = "tinh_trang", nullable = false, length = 15)
    @Builder.Default
    private String tinhTrang = "trong_kho";

    /** NgÃ y nháº­p kho váº­t lÃ½ cá»§a chiáº¿c mÃ¡y nÃ y. */
    @Column(name = "ngay_nhap_kho", nullable = false)
    @Builder.Default
    private LocalDateTime ngayNhapKho = LocalDateTime.now();

    /**
     * ÄÆ¡n hÃ ng Ä‘Ã£ bÃ¡n chiáº¿c mÃ¡y nÃ y.
     * NULL = mÃ¡y Ä‘ang trong kho, chÆ°a bÃ¡n ra.
     * ON DELETE SET NULL â€“ xÃ³a Ä‘Æ¡n hÃ ng khÃ´ng lÃ m máº¥t báº£n ghi IMEI.
     * Đơn hàng đã bán chiếc máy này.
     * NULL = máy đang trong kho, chưa bán ra.
     * ON DELETE SET NULL – xóa đơn hàng không làm mất bản ghi IMEI.
     * FK fk_may_dh tương ứng với ALTER TABLE trong SQL (Module 7).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "don_hang_id", foreignKey = @ForeignKey(name = "fk_may_dh"))
    private DonHang donHang;

    /** Ghi chú nội bộ (hư hỏng, lịch sử sửa chữa...). */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    /** Người đang giữ IMEI này trong giỏ hàng (Cart Reservation) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_giu_id", foreignKey = @ForeignKey(name = "fk_may_dien_thoai_nguoi_giu"))
    private NguoiDung nguoiGiu;

    /** Thời gian bắt đầu giữ IMEI này */
    @Column(name = "thoi_gian_giu")
    private LocalDateTime thoiGianGiu;
}
