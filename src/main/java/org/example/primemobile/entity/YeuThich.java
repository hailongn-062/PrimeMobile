package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng yeu_thich (Module 13: Wishlist).
 * <p>
 * Danh sách sản phẩm yêu thích của khách hàng.
 * Mỗi cặp (khachHang, sanPham) là DUY NHẤT — khách không thể thêm
 * cùng 1 sản phẩm vào wishlist 2 lần.
 * <p>
 * Ghi chú (system_rules.md §7 – Tạm hoãn):
 *  Tính năng Wishlist và thuật toán ưu tiên sản phẩm yêu thích trong
 *  kết quả tìm kiếm tạm thời BỊ HOÃN.
 *  Entity vẫn được tạo đủ để mapping DB.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link KhachHang} (FK khach_hang_id, ON DELETE CASCADE)
 *  - N:1 với {@link SanPham}   (FK san_pham_id)
 */
@Entity
@Table(
        name = "yeu_thich",
        uniqueConstraints = {
                // Mỗi khách hàng chỉ có thể thêm 1 sản phẩm vào wishlist 1 lần
                @UniqueConstraint(name = "uq_yt", columnNames = {"khach_hang_id", "san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuThich {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Khách hàng sở hữu mục yêu thích này.
     * ON DELETE CASCADE – xóa khách hàng thì wishlist tự xóa.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_kh")
    )
    private KhachHang khachHang;

    /**
     * Sản phẩm được yêu thích.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_yt_sp")
    )
    private SanPham sanPham;

    /** Thời điểm thêm sản phẩm vào danh sách yêu thích. */
    @Column(name = "ngay_them", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayThem = LocalDateTime.now();
}
