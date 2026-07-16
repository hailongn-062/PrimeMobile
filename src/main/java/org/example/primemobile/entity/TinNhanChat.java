package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng tin_nhan_chat (Module 11: Chatbot AI).
 * <p>
 * Má»—i báº£n ghi = 1 tin nháº¯n trong cuá»™c há»™i thoáº¡i.
 * <p>
 * Vai trÃ² ngÆ°á»i gá»­i (CHECK chk_tnc_vai â€” chuáº©n Gemini API):
 *  "user"   : Tin nháº¯n tá»« khÃ¡ch hÃ ng.
 *  "model"  : Pháº£n há»“i tá»« Gemini AI (Ä‘Ã£ Ä‘á»•i tá»« 'assistant' theo chuáº©n Gemini).
 *  "system" : Tin nháº¯n há»‡ thá»‘ng ná»™i bá»™ (ngá»¯ cáº£nh, thÃ´ng bÃ¡o...).
 * <p>
 * Intent phÃ¢n loáº¡i (CHECK chk_tnc_intent, nullable):
 *  "tu_van_sp" | "tra_cuu_dh" | "bao_hanh" | "khuyen_mai" | "chinh_sach" | "khac"
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link CuocHoiThoai} (FK cuoc_hoi_thoai_id, ON DELETE CASCADE)
 *  - donHangIdRef (Integer)        â€” FK ON DELETE SET NULL tá»›i don_hang (tham chiáº¿u ngá»¯ cáº£nh)
 *  - sanPhamIdRef (Integer)        â€” FK ON DELETE SET NULL tá»›i san_pham (tham chiáº¿u ngá»¯ cáº£nh)
 */
@Entity
@Table(
        name = "tin_nhan_chat",
        indexes = {
                @Index(name = "idx_tnc_cht", columnList = "cuoc_hoi_thoai_id, thoi_gian")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TinNhanChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Cuá»™c há»™i thoáº¡i chá»©a tin nháº¯n nÃ y.
     * ON DELETE CASCADE â€“ xÃ³a há»™i thoáº¡i thÃ¬ tin nháº¯n tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cuoc_hoi_thoai_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tnc_cht")
    )
    private CuocHoiThoai cuocHoiThoai;

    /**
     * Vai trÃ² ngÆ°á»i gá»­i (chuáº©n Gemini API).
     * GiÃ¡ trá»‹ há»£p lá»‡: "user" | "model" | "system"
     */
    @Column(name = "vai", nullable = false, length = 10)
    private String vai;

    /** Ná»™i dung tin nháº¯n (cÃ³ thá»ƒ lÃ  text, hoáº·c mÃ´ táº£ áº£nh náº¿u cÃ³ hinhAnhUrl). */
    @Column(name = "noi_dung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    /**
     * URL áº£nh Ä‘Ã­nh kÃ¨m (há»— trá»£ Gemini Vision API).
     * NULL náº¿u tin nháº¯n chá»‰ cÃ³ text.
     */
    @Column(name = "hinh_anh_url", length = 255)
    private String hinhAnhUrl;

    /**
     * Sá»‘ token tiÃªu thá»¥ cá»§a tin nháº¯n nÃ y (dÃ¹ng Ä‘á»ƒ quáº£n lÃ½ quota API Free).
     * NULL náº¿u lÃ  tin nháº¯n cá»§a "user".
     */
    @Column(name = "so_token")
    private Integer soToken;

    /**
     * PhÃ¢n loáº¡i Ã½ Ä‘á»‹nh cá»§a tin nháº¯n (nullable).
     * GiÃ¡ trá»‹ há»£p lá»‡: "tu_van_sp" | "tra_cuu_dh" | "bao_hanh"
     *               | "khuyen_mai" | "chinh_sach" | "khac" | NULL
     */
    @Column(name = "intent", length = 30)
    private String intent;

    /**
     * ID Ä‘Æ¡n hÃ ng Ä‘Æ°á»£c nháº¯c Ä‘áº¿n trong tin nháº¯n (ngá»¯ cáº£nh há»™i thoáº¡i).
     * FK fk_tnc_dh vá»›i ON DELETE SET NULL â€” giá»¯ nguyÃªn khi Ä‘Æ¡n bá»‹ xÃ³a.
     * Táº¡m giá»¯ Integer â€” FK tham chiáº¿u ngá»¯ cáº£nh khÃ´ng cáº§n load full DonHang.
     */
    @Column(name = "don_hang_id_ref")
    private Integer donHangIdRef;

    /**
     * ID sáº£n pháº©m Ä‘Æ°á»£c nháº¯c Ä‘áº¿n trong tin nháº¯n (ngá»¯ cáº£nh há»™i thoáº¡i).
     * FK fk_tnc_sp vá»›i ON DELETE SET NULL.
     * Táº¡m giá»¯ Integer â€” FK tham chiáº¿u ngá»¯ cáº£nh khÃ´ng cáº§n load full SanPham.
     */
    @Column(name = "san_pham_id_ref")
    private Integer sanPhamIdRef;

    /** Thá»i Ä‘iá»ƒm gá»­i tin nháº¯n. */
    @Column(name = "thoi_gian", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime thoiGian = LocalDateTime.now();
}
