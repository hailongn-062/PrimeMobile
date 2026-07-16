package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng gio_hang (Module 6: Giá» hÃ ng).
 * <p>
 * Há»— trá»£ 2 loáº¡i giá» hÃ ng:
 *  - KhÃ¡ch vÃ£ng lai: khachHang = NULL, sessionId cÃ³ giÃ¡ trá»‹ (lÆ°u trong cookie/session).
 *  - KhÃ¡ch Ä‘Ã£ Ä‘Äƒng nháº­p: khachHang != NULL, sessionId cÃ³ thá»ƒ NULL.
 * <p>
 * âš ï¸ RÃ ng buá»™c DB: CHECK (khach_hang_id IS NOT NULL OR session_id IS NOT NULL)
 * â†’ KhÃ´ng Ä‘Æ°á»£c Ä‘á»ƒ cáº£ 2 cÃ¹ng NULL. Logic nÃ y pháº£i Ä‘Æ°á»£c kiá»ƒm tra táº¡i Service Layer.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link KhachHang}          (FK khach_hang_id, ON DELETE CASCADE, nullable)
 *  - 1-N vá»›i {@link ChiTietGioHang}     (mappedBy gioHang, CASCADE ALL)
 */
@Entity
@Table(
        name = "gio_hang",
        indexes = {
                @Index(name = "idx_gh_kh",      columnList = "khach_hang_id"),
                @Index(name = "idx_gh_session",  columnList = "session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "chiTietGioHangs")
@EqualsAndHashCode(exclude = "chiTietGioHangs")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * KhÃ¡ch hÃ ng sá»Ÿ há»¯u giá» hÃ ng nÃ y.
     * NULL náº¿u lÃ  khÃ¡ch vÃ£ng lai (giá» hÃ ng theo session).
     * ON DELETE CASCADE â€“ xÃ³a khÃ¡ch hÃ ng thÃ¬ giá» hÃ ng tá»± xÃ³a theo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "khach_hang_id",
            foreignKey = @ForeignKey(name = "fk_gh_kh")
    )
    private KhachHang khachHang;

    /**
     * ID phiÃªn lÃ m viá»‡c cá»§a khÃ¡ch vÃ£ng lai (cookie/session token).
     * NULL náº¿u lÃ  khÃ¡ch Ä‘Ã£ Ä‘Äƒng nháº­p.
     * âš ï¸ Ãt nháº¥t 1 trong 2 (khachHang, sessionId) pháº£i khÃ¡c NULL.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /** Thá»i Ä‘iá»ƒm táº¡o giá» hÃ ng. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thá»i Ä‘iá»ƒm cáº­p nháº­t giá» hÃ ng gáº§n nháº¥t (thÃªm/xÃ³a sáº£n pháº©m). */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan há»‡ 1-N: 1 GioHang â†’ nhiá»u ChiTietGioHang (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "gioHang", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietGioHang> chiTietGioHangs = new ArrayList<>();
}
