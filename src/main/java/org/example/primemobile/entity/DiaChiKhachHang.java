package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping báº£ng dia_chi_khach_hang (Module 5: Äá»‹a chá»‰ khÃ¡ch hÃ ng).
 * <p>
 * LÆ°u danh sÃ¡ch Ä‘á»‹a chá»‰ giao hÃ ng cá»§a má»™t khÃ¡ch hÃ ng.
 * Má»—i Ä‘á»‹a chá»‰ lÆ°u 2 nhÃ³m thÃ´ng tin song song phá»¥c vá»¥ 2 má»¥c Ä‘Ã­ch khÃ¡c nhau:
 * <p>
 * <b>NhÃ³m 1 â€“ ID Ä‘á»‹a chá»‰ (dÃ¹ng Ä‘á»ƒ gá»i API GHN tÃ­nh phÃ­ ship):</b>
 * {@code tinh_thanh_id}, {@code quan_huyen_id}, {@code phuong_xa_code}
 * <p>
 * <b>NhÃ³m 2 â€“ TÃªn Ä‘á»‹a chá»‰ (dÃ¹ng Ä‘á»ƒ hiá»ƒn thá»‹ UI ngay láº­p tá»©c, khÃ´ng cáº§n gá»i API):</b>
 * {@code tinh_thanh_ten}, {@code quan_huyen_ten}, {@code phuong_xa_ten}
 * <p>
 * Ghi chÃº GHN (system_rules.md Â§6):
 *  - Chá»‰ dÃ¹ng API Ä‘á»c (tÃ­nh phÃ­, leadtime), KHÃ”NG gá»i API táº¡o Ä‘Æ¡n váº­n chuyá»ƒn.
 *  - Má»i lá»i gá»i HTTP sang GHN báº¯t buá»™c bá»c trong try-catch, fallback vá» phÃ­ = 0.
 * <p>
 * Loáº¡i Ä‘á»‹a chá»‰ há»£p lá»‡ (CHECK chk_dc_loai):
 *  "nha_rieng" | "co_quan" | "khac"
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link KhachHang} (FK khach_hang_id, ON DELETE CASCADE)
 */
@Entity
@Table(name = "dia_chi_khach_hang")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class DiaChiKhachHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * KhÃ¡ch hÃ ng sá»Ÿ há»¯u Ä‘á»‹a chá»‰ nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a khÃ¡ch hÃ ng thÃ¬ Ä‘á»‹a chá»‰ tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dc_kh")
    )
    private KhachHang khachHang;

    /**
     * Loáº¡i Ä‘á»‹a chá»‰ (DEFAULT 'nha_rieng').
     * GiÃ¡ trá»‹ há»£p lá»‡: "nha_rieng" | "co_quan" | "khac"
     */
    @Column(name = "loai_dia_chi", nullable = false, length = 15)
    @Builder.Default
    private String loaiDiaChi = "nha_rieng";

    /** Tên gợi nhớ của địa chỉ (ví dụ: Nhà, Công ty) */
    @Column(name = "ten_goi_nho", length = 255)
    private String tenGoiNho;

    /** TÃªn ngÆ°á»i nháº­n hÃ ng táº¡i Ä‘á»‹a chá»‰ nÃ y. */
    @Column(name = "ho_ten_nguoi_nhan", length = 100)
    private String hoTenNguoiNhan;

    /** Sá»‘ Ä‘iá»‡n thoáº¡i ngÆ°á»i nháº­n hÃ ng. */
    @Column(name = "so_dien_thoai_nguoi_nhan", length = 20)
    private String soDienThoaiNguoiNhan;

    /** Sá»‘ nhÃ , tÃªn Ä‘Æ°á»ng, tÃ²a nhÃ ... (pháº§n Ä‘á»‹a chá»‰ chi tiáº¿t). */
    @Column(name = "dia_chi_chi_tiet", nullable = false, length = 255)
    private String diaChiChiTiet;

    // -------------------------------------------------------------------------
    // NHÃ“M 1: ID Ä‘á»‹a chá»‰ â€“ gá»­i sang API GHN Ä‘á»ƒ tÃ­nh phÃ­ ship & leadtime
    // -------------------------------------------------------------------------

    /**
     * ID tá»‰nh/thÃ nh phá»‘ theo há»‡ thá»‘ng GHN.
     * DÃ¹ng cho API: /v2/shipping-order/fee vÃ  /v2/shipping-order/leadtime
     */
    @Column(name = "tinh_thanh_id", nullable = false)
    private Integer tinhThanhId;

    /**
     * ID quáº­n/huyá»‡n theo há»‡ thá»‘ng GHN.
     */
    @Column(name = "quan_huyen_id", nullable = false)
    private Integer quanHuyenId;

    /**
     * MÃ£ phÆ°á»ng/xÃ£ theo há»‡ thá»‘ng GHN.
     * Kiá»ƒu VARCHAR vÃ¬ GHN dÃ¹ng mÃ£ dáº¡ng chuá»—i (vÃ­ dá»¥: "550113").
     */
    @Column(name = "phuong_xa_code", nullable = false, length = 20)
    private String phuongXaCode;

    // -------------------------------------------------------------------------
    // NHÃ“M 2: TÃªn Ä‘á»‹a chá»‰ â€“ hiá»ƒn thá»‹ ngay trÃªn UI, khÃ´ng cáº§n gá»i thÃªm API
    // -------------------------------------------------------------------------

    /** TÃªn tá»‰nh/thÃ nh phá»‘ Ä‘á»ƒ hiá»ƒn thá»‹ (vÃ­ dá»¥: "TP. Há»“ ChÃ­ Minh"). */
    @Column(name = "tinh_thanh_ten", nullable = false, length = 100)
    private String tinhThanhTen;

    /** TÃªn quáº­n/huyá»‡n Ä‘á»ƒ hiá»ƒn thá»‹ (vÃ­ dá»¥: "Quáº­n 1"). */
    @Column(name = "quan_huyen_ten", nullable = false, length = 100)
    private String quanHuyenTen;

    /** TÃªn phÆ°á»ng/xÃ£ Ä‘á»ƒ hiá»ƒn thá»‹ (vÃ­ dá»¥: "PhÆ°á»ng Báº¿n NghÃ©"). */
    @Column(name = "phuong_xa_ten", nullable = false, length = 100)
    private String phuongXaTen;

    /**
     * ÄÃ¡nh dáº¥u Ä‘Ã¢y lÃ  Ä‘á»‹a chá»‰ máº·c Ä‘á»‹nh cá»§a khÃ¡ch hÃ ng (DEFAULT false).
     * Má»—i khÃ¡ch hÃ ng chá»‰ nÃªn cÃ³ 1 Ä‘á»‹a chá»‰ máº·c Ä‘á»‹nh.
     * Logic Ä‘áº£m báº£o duy nháº¥t 1 mac_dinh = true pháº£i Ä‘Æ°á»£c xá»­ lÃ½ táº¡i Service Layer.
     */
    @Column(name = "mac_dinh", nullable = false)
    @Builder.Default
    private Boolean macDinh = false;
}
