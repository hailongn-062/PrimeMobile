package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng cuoc_hoi_thoai (Module 11: Chatbot AI).
 * <p>
 * Má»—i phiÃªn há»™i thoáº¡i giá»¯a khÃ¡ch hÃ ng vÃ  Chatbot AI (Gemini).
 * Há»— trá»£ cáº£ khÃ¡ch vÃ£ng lai (sessionId) vÃ  khÃ¡ch Ä‘Ã£ Ä‘Äƒng nháº­p (khachHang).
 * <p>
 * Ghi chÃº (system_rules.md Â§7 â€“ Táº¡m hoÃ£n):
 *  TÃ­ch há»£p Gemini API táº¡m thá»i Bá»Š HOÃƒN.
 *  Entity Ä‘Æ°á»£c táº¡o Ä‘áº§y Ä‘á»§ Ä‘á»ƒ mapping DB vÃ  phá»¥c vá»¥ refactor {@link DonHang}.
 * <p>
 *  - 1-N vá»›i {@link TinNhanChat}  (mappedBy cuocHoiThoai, CASCADE ALL)
 *  - ID tham chiáº¿u lá»ng tá»›i KhachHang (khach_hang_id) vÃ  DonHang (thÃ´ng qua cá»™t trong báº£ng don_hang)
 */
@Entity
@Table(
        name = "cuoc_hoi_thoai",
        indexes = {
                @Index(name = "idx_cht_kh",      columnList = "khach_hang_id, updated_at"),
                @Index(name = "idx_cht_session",  columnList = "session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"tinNhanChats"})
@EqualsAndHashCode(exclude = {"tinNhanChats"})
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class CuocHoiThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID KhÃ¡ch hÃ ng Ä‘Ã£ Ä‘Äƒng nháº­p thá»±c hiá»‡n há»™i thoáº¡i.
     * Tham chiáº¿u lá»ng, NULL náº¿u lÃ  khÃ¡ch vÃ£ng lai.
     */
    @Column(name = "khach_hang_id")
    private Integer khachHangId;

    /**
     * Session token Ä‘á»‹nh danh khÃ¡ch vÃ£ng lai.
     * NULL náº¿u lÃ  khÃ¡ch Ä‘Ã£ Ä‘Äƒng nháº­p.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * TiÃªu Ä‘á» ngáº¯n cá»§a há»™i thoáº¡i (tá»± Ä‘á»™ng sinh bá»Ÿi AI hoáº·c dÃ¹ng tin nháº¯n Ä‘áº§u tiÃªn).
     * NULL khi má»›i táº¡o.
     */
    @Column(name = "tieu_de", length = 255)
    private String tieuDe;

    /** Thá»i Ä‘iá»ƒm táº¡o cuá»™c há»™i thoáº¡i. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thá»i Ä‘iá»ƒm cáº­p nháº­t gáº§n nháº¥t (dÃ¹ng Ä‘á»ƒ sáº¯p xáº¿p há»™i thoáº¡i gáº§n Ä‘Ã¢y). */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 CuocHoiThoai â†’ nhiá»u TinNhanChat (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "cuocHoiThoai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TinNhanChat> tinNhanChats = new ArrayList<>();
}
