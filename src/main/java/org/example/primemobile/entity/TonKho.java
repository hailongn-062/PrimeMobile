package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng ton_kho (Module 3: Kho hÃ ng).
 * <p>
 * LÆ°u sá»‘ lÆ°á»£ng tá»“n kho cá»§a tá»«ng biáº¿n thá»ƒ SKU táº¡i tá»«ng kho.
 * Má»—i cáº·p (kho, bienTheSanPham) lÃ  DUY NHáº¤T trong báº£ng.
 * <p>
 * âš ï¸ RÃ ng buá»™c quan trá»ng (system_rules.md Â§3.1 â€“ Safety Stock Rule):
 *  Má»©c tá»“n kho tá»‘i thiá»ƒu báº¯t buá»™c = 5 sáº£n pháº©m/SKU/kho.
 *  Logic cháº·n xuáº¥t hÃ ng pháº£i Ä‘Æ°á»£c kiá»ƒm tra á»Ÿ Service Layer trÆ°á»›c khi UPDATE.
 * <p>
 * RÃ ng buá»™c DB: CHECK (so_luong >= 0) â€“ khÃ´ng cho phÃ©p Ã¢m kho.
 * <p>
 * Quan há»‡:
 *  - N:1 vá»›i {@link Kho}             (FK kho_id)
 *  - N:1 vá»›i {@link BienTheSanPham}  (FK bien_the_san_pham_id)
 */
@Entity
@Table(
        name = "ton_kho",
        indexes = {
                @Index(name = "idx_tk_bt", columnList = "bien_the_san_pham_id")
        },
        uniqueConstraints = {
                // Äáº£m báº£o má»—i cáº·p (kho, SKU) chá»‰ cÃ³ Ä‘Ãºng 1 báº£n ghi tá»“n kho
                @UniqueConstraint(name = "uq_ton_kho", columnNames = {"kho_id", "bien_the_san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class TonKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Kho chá»©a hÃ ng.
     * NOT NULL â€“ má»—i báº£n ghi tá»“n kho pháº£i thuá»™c vá» má»™t kho cá»¥ thá»ƒ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tk_kho")
    )
    private Kho kho;

    /**
     * Biáº¿n thá»ƒ sáº£n pháº©m (SKU) Ä‘ang Ä‘Æ°á»£c theo dÃµi tá»“n kho.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tk_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * Sá»‘ lÆ°á»£ng tá»“n kho hiá»‡n táº¡i (DEFAULT 0).
     * CHECK constraint á»Ÿ DB Ä‘áº£m báº£o >= 0.
     * Service Layer pháº£i cháº·n khÃ´ng cho giáº£m xuá»‘ng dÆ°á»›i 5 (Safety Stock).
     */
    @Column(name = "so_luong", nullable = false)
    @Builder.Default
    private Integer soLuong = 0;

    /** Thá»i Ä‘iá»ƒm cáº­p nháº­t tá»“n kho gáº§n nháº¥t. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
