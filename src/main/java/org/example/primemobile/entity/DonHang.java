package org.example.primemobile.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng don_hang (Module 7: ÄÆ¡n hÃ ng).
 * <p>
 * Trung tÃ¢m cá»§a toÃ n bá»™ luá»“ng bÃ¡n hÃ ng. Má»—i Ä‘Æ¡n hÃ ng cÃ³ 2 kÃªnh:
 * - "online" : KhÃ¡ch Ä‘áº·t qua website, xá»­ lÃ½ bá»Ÿi luá»“ng online.
 * - "tai_quay" : NhÃ¢n viÃªn táº¡o táº¡i quáº§y (bÃ¡n hÃ ng offline).
 * <p>
 * âš ï¸ COMPUTED COLUMNS (AS PERSISTED):
 * Cá»™t {@code tong_thanh_toan} = tong_tien_hang - tien_giam_gia + phi_ship
 * Báº®T BUá»˜C dÃ¹ng {@code @Column(insertable = false, updatable = false)}.
 * <p>
 * Snapshot Ä‘á»‹a chá»‰ giao hÃ ng:
 * Äá»‹a chá»‰ giao Ä‘Æ°á»£c snapshot ngay lÃºc Ä‘áº·t hÃ ng (hoTenNguoiNhan, sdtNguoiNhan,
 * diaChiGiaCuThe, phuongXaGiao, quanHuyenGiao, tinhThanhGiao).
 * Äiá»u nÃ y Ä‘áº£m báº£o Ä‘Æ¡n hÃ ng khÃ´ng bá»‹ áº£nh hÆ°á»Ÿng dÃ¹ khÃ¡ch sau Ä‘Ã³ thay Ä‘á»•i Ä‘á»‹a
 * chá»‰.
 * <p>
 * âœ… ToÃ n bá»™ FK Ä‘Ã£ Ä‘Æ°á»£c refactor hoÃ n táº¥t â€” khÃ´ng cÃ²n thuá»™c tÃ­nh Integer táº¡m
 * nÃ o.
 * <p>
 * Tráº¡ng thÃ¡i Ä‘Æ¡n hÃ ng (CHECK chk_dh_trang_thai):
 * "cho_xac_nhan" | "cho_hoan_tien" | "da_xac_nhan" | "dang_giao"
 * | "da_hoan_thanh" | "da_huy" | "don_hang_cho"
 * <p>
 * Tráº¡ng thÃ¡i thanh toÃ¡n (CHECK chk_dh_trang_thai_tt):
 * "chua_thanh_toan" | "dang_chuyen_huong" | "da_thanh_toan" | "that_bai" | "da_hoan_tien"
 * <p>
 * Luáº­t há»§y Ä‘Æ¡n (system_rules.md Â§2.2):
 * KhÃ¡ch hÃ ng chá»‰ Ä‘Æ°á»£c há»§y khi trang_thai = 'cho_xac_nhan'.
 * NÃºt Há»§y pháº£i bá»‹ Disable khi Ä‘Æ¡n Ä‘Ã£ chuyá»ƒn sang 'da_xac_nhan' hoáº·c
 * 'dang_giao'.
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link KhachHang} (FK khach_hang_id)
 * - N:1 vá»›i {@link NguoiDung} (FK nguoi_xu_ly_id, nullable)
 * - N:1 vá»›i {@link DiaChiKhachHang} (FK dia_chi_giao_id, ON DELETE SET NULL)
 * - 1-N vá»›i {@link ChiTietDonHang} (mappedBy donHang, CASCADE ALL)
 * - 1-N vá»›i {@link ThanhToan} (mappedBy donHang)
 */
@Entity
@Table(name = "don_hang", indexes = {
                @Index(name = "idx_dh_kh", columnList = "khach_hang_id, ngay_dat"),
                @Index(name = "idx_dh_tts", columnList = "trang_thai, ngay_dat"),
                @Index(name = "idx_dh_ngay", columnList = "ngay_dat"),
                @Index(name = "idx_dh_tttt", columnList = "trang_thai_thanh_toan, thoi_gian_het_han_tt")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uq_dh_ma", columnNames = "ma_don_hang")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

@ToString(exclude = { "chiTietDonHangs", "thanhToans" })
@EqualsAndHashCode(exclude = { "chiTietDonHangs", "thanhToans" })
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DonHang {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /** MÃ£ Ä‘Æ¡n hÃ ng â€“ duy nháº¥t, hiá»ƒn thá»‹ cho khÃ¡ch (vÃ­ dá»¥: "DH2024001"). */
        @Column(name = "ma_don_hang", nullable = false, length = 50, unique = true)
        private String maDonHang;

        /**
         * KhÃ¡ch hÃ ng Ä‘áº·t Ä‘Æ¡n.
         * NOT NULL â€“ má»i Ä‘Æ¡n hÃ ng Ä‘á»u pháº£i gáº¯n vá»›i 1 khÃ¡ch hÃ ng
         * (ká»ƒ cáº£ khÃ¡ch láº» vÃ£ng lai sáº½ Ä‘Æ°á»£c gÃ¡n vÃ o tÃ i khoáº£n khÃ¡ch láº» máº·c Ä‘á»‹nh).
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "khach_hang_id", nullable = false, foreignKey = @ForeignKey(name = "fk_dh_kh"))
        private KhachHang khachHang;

        /**
         * NhÃ¢n viÃªn / Admin xá»­ lÃ½ Ä‘Æ¡n nÃ y.
         * NULL khi Ä‘Æ¡n online chÆ°a cÃ³ nhÃ¢n viÃªn nháº­n xá»­ lÃ½.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "nguoi_xu_ly_id", foreignKey = @ForeignKey(name = "fk_dh_nd"))
        private NguoiDung nguoiXuLy;

        /**
         * KÃªnh bÃ¡n hÃ ng (DEFAULT 'online').
         * GiÃ¡ trá»‹ há»£p lá»‡: "online" | "tai_quay"
         */
        @Column(name = "kenh_ban", nullable = false, length = 10)
        @Builder.Default
        private String kenhBan = "online";

        /** NgÃ y giá» khÃ¡ch Ä‘áº·t hÃ ng (DEFAULT GETDATE()). */
        @Column(name = "ngay_dat", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime ngayDat = LocalDateTime.now();

        // -------------------------------------------------------------------------
        // SNAPSHOT Äá»ŠA CHá»ˆ GIAO HÃ€NG
        // LÆ°u cáº£ FK láº«n cÃ¡c trÆ°á»ng text Ä‘á»ƒ Ä‘áº£m báº£o tÃ­nh báº¥t biáº¿n cá»§a lá»‹ch sá»­ Ä‘Æ¡n hÃ ng
        // -------------------------------------------------------------------------

        /**
         * FK tá»›i Ä‘á»‹a chá»‰ giao hÃ ng gá»‘c cá»§a khÃ¡ch (ON DELETE SET NULL).
         * NULL sau khi khÃ¡ch xÃ³a Ä‘á»‹a chá»‰, nhÆ°ng cÃ¡c trÆ°á»ng text snapshot váº«n cÃ²n.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "dia_chi_giao_id", foreignKey = @ForeignKey(name = "fk_dh_dc"))
        private DiaChiKhachHang diaChiGiao;

        /** Snapshot: tÃªn ngÆ°á»i nháº­n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "ho_ten_nguoi_nhan", length = 100)
        private String hoTenNguoiNhan;

        /** Snapshot: SÄT ngÆ°á»i nháº­n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "sdt_nguoi_nhan", length = 20)
        private String sdtNguoiNhan;

        /** Snapshot: email ngÆ°á»i nháº­n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "email_nguoi_nhan", length = 100)
        private String emailNguoiNhan;

        /** Snapshot: sá»‘ nhÃ , tÃªn Ä‘Æ°á»ng táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "dia_chi_giao_cu_the", length = 255)
        private String diaChiGiaCuThe;

        /** Snapshot: tÃªn phÆ°á»ng/xÃ£ táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "phuong_xa_giao", length = 100)
        private String phuongXaGiao;

        /** Snapshot: tÃªn quáº­n/huyá»‡n táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "quan_huyen_giao", length = 100)
        private String quanHuyenGiao;

        /** Snapshot: tÃªn tá»‰nh/thÃ nh phá»‘ táº¡i thá»i Ä‘iá»ƒm Ä‘áº·t hÃ ng. */
        @Column(name = "tinh_thanh_giao", length = 100)
        private String tinhThanhGiao;

        // -------------------------------------------------------------------------
        // TÃ€I CHÃNH
        // -------------------------------------------------------------------------

        /** Tá»•ng tiá»n hÃ ng (chÆ°a giáº£m giÃ¡, chÆ°a cá»™ng phÃ­ ship). */
        @Column(name = "tong_tien_hang", nullable = false, precision = 15, scale = 2)
        private BigDecimal tongTienHang;

        /** Sá»‘ tiá»n Ä‘Æ°á»£c giáº£m (tá»« mÃ£ giáº£m giÃ¡ hoáº·c háº¡ng thÃ nh viÃªn). DEFAULT 0. */
        @Column(name = "tien_giam_gia", nullable = false, precision = 15, scale = 2)
        @Builder.Default
        private BigDecimal tienGiamGia = BigDecimal.ZERO;

        /**
         * PhÃ­ váº­n chuyá»ƒn láº¥y tá»« GHN API (DEFAULT 0).
         * Fallback vá» 0 náº¿u GHN API lá»—i (system_rules.md Â§6).
         */
        @Column(name = "phi_ship", nullable = false, precision = 15, scale = 2)
        @Builder.Default
        private BigDecimal phiShip = BigDecimal.ZERO;

        /**
         * âš ï¸ Cá»˜T TÃNH TOÃN Tá»° Äá»˜NG (Computed Column - PERSISTED):
         * SQL Server tá»± tÃ­nh: tong_tien_hang - tien_giam_gia + phi_ship
         * Báº®T BUá»˜C insertable = false, updatable = false
         * Ä‘á»ƒ Hibernate khÃ´ng cá»‘ ghi vÃ o cá»™t nÃ y gÃ¢y lá»—i "Cannot update a computed
         * column".
         */
        @Column(name = "tong_thanh_toan", insertable = false, updatable = false, precision = 15, scale = 2)
        private BigDecimal tongThanhToan;

        /**
         * ChÆ°Æ¡ng trÃ¬nh khuyáº¿n mÃ£i Ã¡p dá»¥ng cho Ä‘Æ¡n hÃ ng.
         */
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "chuong_trinh_khuyen_mai_id")
        private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

        // -------------------------------------------------------------------------
        // Váº¬N CHUYá»‚N
        // -------------------------------------------------------------------------

        /**
         * NgÃ y giao hÃ ng dá»± kiáº¿n (láº¥y tá»« GHN API /leadtime).
         * Chá»‰ lÆ°u ngÃ y, khÃ´ng lÆ°u giá».
         */
        @Column(name = "ngay_giao_du_kien")
        private LocalDate ngayGiaoDuKien;

        /** NgÃ y giá» giao hÃ ng thá»±c táº¿ (Ä‘iá»n khi cáº­p nháº­t tráº¡ng thÃ¡i 'da_hoan_thanh'). */
        @Column(name = "ngay_giao_thuc_te")
        private LocalDateTime ngayGiaoThucTe;

        // -------------------------------------------------------------------------
        // TRáº NG THÃI ÄÆ N HÃ€NG
        // -------------------------------------------------------------------------

        /**
         * Tráº¡ng thÃ¡i xá»­ lÃ½ Ä‘Æ¡n hÃ ng (DEFAULT 'cho_xac_nhan').
         * GiÃ¡ trá»‹ há»£p lá»‡: "cho_xac_nhan" | "cho_hoan_tien" | "da_xac_nhan"
         * | "dang_giao" | "da_hoan_thanh" | "da_huy" | "don_hang_cho"
         * âš ï¸ KhÃ¡ch chá»‰ Ä‘Æ°á»£c há»§y khi = "cho_xac_nhan" (system_rules.md Â§2.2).
         */
        @Column(name = "trang_thai", nullable = false, length = 20)
        @Builder.Default
        private String trangThai = "cho_xac_nhan";

        /**
         * Tráº¡ng thÃ¡i thanh toÃ¡n (DEFAULT 'chua_thanh_toan').
         * GiÃ¡ trá»‹ há»£p lá»‡:
         * "chua_thanh_toan"   â€” má»›i táº¡o Ä‘Æ¡n, chÆ°a báº¯t Ä‘áº§u thanh toÃ¡n
         * "dang_chuyen_huong" â€” Ä‘ang redirect sang trang VNPay
         * "da_thanh_toan"     â€” VNPay IPN xÃ¡c nháº­n OK
         * "that_bai"          â€” quÃ¡ háº¡n hoáº·c VNPay tráº£ lá»—i
         * "da_hoan_tien"      â€” Ä‘Ã£ hoÃ n tiá»n thÃ nh cÃ´ng cho khÃ¡ch
         */
        @Column(name = "trang_thai_thanh_toan", nullable = false, length = 20)
        @Builder.Default
        private String trangThaiThanhToan = "chua_thanh_toan";

        /**
         * Thá»i Ä‘iá»ƒm háº¿t háº¡n chá» thanh toÃ¡n VNPay (thÆ°á»ng sau 15 phÃºt).
         * NULL vá»›i thanh toÃ¡n offline (tiá»n máº·t / chuyá»ƒn khoáº£n).
         */
        @Column(name = "thoi_gian_het_han_tt")
        private LocalDateTime thoiGianHetHanTt;

        /** Ghi chÃº cá»§a khÃ¡ch hoáº·c nhÃ¢n viÃªn vá» Ä‘Æ¡n hÃ ng. */
        @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
        private String ghiChu;

        /** Thá»i Ä‘iá»ƒm cáº­p nháº­t Ä‘Æ¡n hÃ ng gáº§n nháº¥t. */
        @Column(name = "updated_at", nullable = false)
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();

        // -------------------------------------------------------------------------
        // Quan há»‡ 1-N: 1 DonHang â†’ nhiá»u ChiTietDonHang (ON DELETE CASCADE)
        // -------------------------------------------------------------------------
        @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
        @Builder.Default
        private List<ChiTietDonHang> chiTietDonHangs = new ArrayList<>();

        // -------------------------------------------------------------------------
        // Quan há»‡ 1-N: 1 DonHang â†’ nhiá»u ThanhToan (lá»‹ch sá»­ thanh toÃ¡n)
        // -------------------------------------------------------------------------
        @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY)
        @Builder.Default
        private List<ThanhToan> thanhToans = new ArrayList<>();
}
