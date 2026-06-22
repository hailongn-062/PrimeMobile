package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng gio_hang (Module 6: Giỏ hàng).
 * <p>
 * Hỗ trợ 2 loại giỏ hàng:
 *  - Khách vãng lai: khachHang = NULL, sessionId có giá trị (lưu trong cookie/session).
 *  - Khách đã đăng nhập: khachHang != NULL, sessionId có thể NULL.
 * <p>
 * ⚠️ Ràng buộc DB: CHECK (khach_hang_id IS NOT NULL OR session_id IS NOT NULL)
 * → Không được để cả 2 cùng NULL. Logic này phải được kiểm tra tại Service Layer.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link KhachHang}          (FK khach_hang_id, ON DELETE CASCADE, nullable)
 *  - 1-N với {@link ChiTietGioHang}     (mappedBy gioHang, CASCADE ALL)
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
public class GioHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Khách hàng sở hữu giỏ hàng này.
     * NULL nếu là khách vãng lai (giỏ hàng theo session).
     * ON DELETE CASCADE – xóa khách hàng thì giỏ hàng tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "khach_hang_id",
            foreignKey = @ForeignKey(name = "fk_gh_kh")
    )
    private KhachHang khachHang;

    /**
     * ID phiên làm việc của khách vãng lai (cookie/session token).
     * NULL nếu là khách đã đăng nhập.
     * ⚠️ Ít nhất 1 trong 2 (khachHang, sessionId) phải khác NULL.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /** Thời điểm tạo giỏ hàng. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật giỏ hàng gần nhất (thêm/xóa sản phẩm). */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 GioHang → nhiều ChiTietGioHang (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "gioHang", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietGioHang> chiTietGioHangs = new ArrayList<>();
}
