package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng pham_vi_khuyen_mai (Module 9: Khuyến mãi).
 * <p>
 * Xác định phạm vi áp dụng của một chương trình khuyến mãi.
 * Có thể áp dụng theo 3 cấp độ (một hoặc nhiều cùng lúc):
 *  - Theo sản phẩm cụ thể ({@link SanPham})
 * <p>
 * Quan hệ:
 *  - N:1 với {@link ChuongTrinhKhuyenMai} (FK ctkm_id, ON DELETE CASCADE)
 *  - N:1 với {@link SanPham}              (FK san_pham_id, nullable)
 */
@Entity
@Table(name = "pham_vi_khuyen_mai")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhamViKhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Chương trình khuyến mãi áp dụng phạm vi này.
     * ON DELETE CASCADE – xóa chương trình thì phạm vi tự xóa.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ctkm_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pvkm_ctkm")
    )
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    /**
     * Sản phẩm cụ thể được áp dụng.
     * NULL = không giới hạn theo sản phẩm.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "san_pham_id",
            foreignKey = @ForeignKey(name = "fk_pvkm_sp")
    )
    private SanPham sanPham;
}
