package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng danh_gia_san_pham (Module 12: ÄÃ¡nh giÃ¡ & Há»i Ä‘Ã¡p).
 * <p>
 * LÆ°u Ä‘Ã¡nh giÃ¡ sáº£n pháº©m tá»« khÃ¡ch hÃ ng sau khi mua hÃ ng.
 * <p>
 * RÃ ng buá»™c nghiá»‡p vá»¥ (system_rules.md Â§5):
 * Chá»‰ khÃ¡ch hÃ ng cÃ³ Ä‘Æ¡n hÃ ng tráº¡ng thÃ¡i 'da_hoan_thanh' chá»©a sáº£n pháº©m Ä‘Ã³
 * má»›i Ä‘Æ°á»£c phÃ©p viáº¿t Ä‘Ã¡nh giÃ¡.
 * Kiá»ƒm tra Ä‘iá»u kiá»‡n nÃ y táº¡i Service Layer trÆ°á»›c khi cho phÃ©p táº¡o báº£n ghi.
 * <p>
 * RÃ ng buá»™c UNIQUE (uq_dg): 1 khÃ¡ch chá»‰ Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡ 1 láº§n
 * cho 1 sáº£n pháº©m trong 1 Ä‘Æ¡n hÃ ng cá»¥ thá»ƒ.
 * <p>
 * HÃ¬nh áº£nh Ä‘Ã­nh kÃ¨m ({@code hinhAnhJson}):
 * LÆ°u dÆ°á»›i dáº¡ng JSON array Ä‘Æ°á»ng dáº«n, vÃ­ dá»¥: ["url1", "url2", "url3"].
 * Parse/Serialize táº¡i Service/DTO Layer.
 * <p>
 * Tráº¡ng thÃ¡i (CHECK chk_dg_trang_thai):
 * "cho_duyet" | "da_duyet" | "an"
 * <p>
 * Ghi chÃº: TÃ­nh nÄƒng Ä‘Ã¡nh giÃ¡ táº¡m hoÃ£n (system_rules.md Â§7),
 * nhÆ°ng Entity váº«n pháº£i táº¡o Ä‘á»§ Ä‘á»ƒ mapping DB.
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link SanPham} (FK san_pham_id)
 * - N:1 vá»›i {@link KhachHang} (FK khach_hang_id)
 * - N:1 vá»›i {@link DonHang} (FK don_hang_id)
 */
@Entity
@Table(name = "danh_gia_san_pham", indexes = {
                @Index(name = "idx_dg_sp", columnList = "san_pham_id, trang_thai")
}, uniqueConstraints = {
                // 1 khÃ¡ch chá»‰ Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡ 1 láº§n cho 1 sáº£n pháº©m trong 1 Ä‘Æ¡n hÃ ng
                @UniqueConstraint(name = "uq_dg", columnNames = { "khach_hang_id", "don_hang_id", "san_pham_id" })
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DanhGiaSanPham {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * Sáº£n pháº©m Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡.
         * NOT NULL.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "san_pham_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dg_sp"))
        private SanPham sanPham;

        /**
         * KhÃ¡ch hÃ ng viáº¿t Ä‘Ã¡nh giÃ¡.
         * NOT NULL â€“ chá»‰ khÃ¡ch cÃ³ tÃ i khoáº£n má»›i Ä‘Æ°á»£c Ä‘Ã¡nh giÃ¡.
         * Khách hàng viết đánh giá.
         * NOT NULL – chỉ khách có tài khoản mới được đánh giá.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "khach_hang_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dg_kh"))
        private KhachHang khachHang;

        /**
         * Đơn hàng liên kết với đánh giá (đảm bảo đã mua mới được đánh giá).
         * NOT NULL.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "don_hang_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dg_dh"))
        @com.fasterxml.jackson.annotation.JsonIgnore
        private DonHang donHang;

        /**
         * Số sao đánh giá.
         * CHECK: sao BETWEEN 1 AND 5.
         */
        @Column(name = "sao", nullable = false)
        private Integer sao;

        /** TiÃªu Ä‘á» ngáº¯n cá»§a Ä‘Ã¡nh giÃ¡ (tÃ¹y chá»n). */
        @Column(name = "tieu_de", length = 200)
        private String tieuDe;

        /** Ná»™i dung Ä‘Ã¡nh giÃ¡ chi tiáº¿t. */
        @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
        private String noiDung;

        /**
         * Máº£ng URL hÃ¬nh áº£nh Ä‘Ã­nh kÃ¨m lÆ°u dÆ°á»›i dáº¡ng JSON string.
         * VÃ­ dá»¥: '["https://...", "https://..."]'
         * Parse táº¡i Service/DTO Layer khi cáº§n hiá»ƒn thá»‹.
         * NULL náº¿u khÃ´ng Ä‘Ã­nh kÃ¨m áº£nh.
         */
        @Column(name = "hinh_anh_json", columnDefinition = "NVARCHAR(MAX)")
        private String hinhAnhJson;

        /**
         * Tráº¡ng thÃ¡i kiá»ƒm duyá»‡t (DEFAULT 'cho_duyet').
         * GiÃ¡ trá»‹ há»£p lá»‡: "cho_duyet" | "da_duyet" | "an"
         * NhÃ¢n viÃªn cÃ³ quyá» n duyá»‡t/áº©n Ä‘Ã¡nh giÃ¡.
         */
        @Column(name = "trang_thai", nullable = false, length = 15)
        @Builder.Default
        private String trangThai = "cho_duyet";

        /** Thá» i Ä‘iá»ƒm táº¡o Ä‘Ã¡nh giÃ¡. */
        @Column(name = "ngay_tao", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime ngayTao = LocalDateTime.now();

        /** Chuỗi chứa tên các biến thể đã mua (không lưu vào DB, dùng để trả về API). */
        @Transient
        private String tenBienTheMua;
}
