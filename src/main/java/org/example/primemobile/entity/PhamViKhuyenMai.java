package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng pham_vi_khuyen_mai (Module 9: Khuyến mãi).
 * <p>
 * Xác định phạm vi áp dụng của một chương trình khuyến mãi.
 * Có thể áp dụng theo 3 cấp độ (một hoặc nhiều cùng lúc):
 *  - Theo sản phẩm cụ thể ({@link SanPham})
 *  - Theo danh mục ({@link DanhMuc})
 *  - Theo hãng sản xuất ({@link HangSanXuat})
 * <p>
 * Quan hệ:
 *  - N:1 với {@link ChuongTrinhKhuyenMai} (FK ctkm_id, ON DELETE CASCADE)
 *  - N:1 với {@link SanPham}              (FK san_pham_id, nullable)
 *  - N:1 với {@link DanhMuc}              (FK danh_muc_id, nullable)
 *  - N:1 với {@link HangSanXuat}          (FK hang_sx_id, nullable)
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

    /**
     * Danh mục được áp dụng (toàn bộ sản phẩm trong danh mục).
     * NULL = không giới hạn theo danh mục.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "danh_muc_id",
            foreignKey = @ForeignKey(name = "fk_pvkm_dm")
    )
    private DanhMuc danhMuc;

    /**
     * Hãng sản xuất được áp dụng (toàn bộ sản phẩm của hãng).
     * NULL = không giới hạn theo hãng.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "hang_sx_id",
            foreignKey = @ForeignKey(name = "fk_pvkm_hsx")
    )
    private HangSanXuat hangSanXuat;
}
