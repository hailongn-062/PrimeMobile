package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng ton_kho (Module 3: Kho hàng).
 * <p>
 * Lưu số lượng tồn kho của từng biến thể SKU tại từng kho.
 * Mỗi cặp (kho, bienTheSanPham) là DUY NHẤT trong bảng.
 * <p>
 * ⚠️ Ràng buộc quan trọng (system_rules.md §3.1 – Safety Stock Rule):
 *  Mức tồn kho tối thiểu bắt buộc = 5 sản phẩm/SKU/kho.
 *  Logic chặn xuất hàng phải được kiểm tra ở Service Layer trước khi UPDATE.
 * <p>
 * Ràng buộc DB: CHECK (so_luong >= 0) – không cho phép âm kho.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link Kho}             (FK kho_id)
 *  - N:1 với {@link BienTheSanPham}  (FK bien_the_san_pham_id)
 */
@Entity
@Table(
        name = "ton_kho",
        indexes = {
                @Index(name = "idx_tk_bt", columnList = "bien_the_san_pham_id")
        },
        uniqueConstraints = {
                // Đảm bảo mỗi cặp (kho, SKU) chỉ có đúng 1 bản ghi tồn kho
                @UniqueConstraint(name = "uq_ton_kho", columnNames = {"kho_id", "bien_the_san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TonKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Kho chứa hàng.
     * NOT NULL – mỗi bản ghi tồn kho phải thuộc về một kho cụ thể.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tk_kho")
    )
    private Kho kho;

    /**
     * Biến thể sản phẩm (SKU) đang được theo dõi tồn kho.
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
     * Số lượng tồn kho hiện tại (DEFAULT 0).
     * CHECK constraint ở DB đảm bảo >= 0.
     * Service Layer phải chặn không cho giảm xuống dưới 5 (Safety Stock).
     */
    @Column(name = "so_luong", nullable = false)
    @Builder.Default
    private Integer soLuong = 0;

    /** Thời điểm cập nhật tồn kho gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
