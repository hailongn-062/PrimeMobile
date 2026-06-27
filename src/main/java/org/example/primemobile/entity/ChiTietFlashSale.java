package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping bảng chi_tiet_flash_sale (Module 9: Khuyến mãi).
 * <p>
 * Chi tiết giá flash sale cho từng biến thể sản phẩm trong một chương trình flash sale.
 * Mỗi biến thể có giá flash riêng và giới hạn số lượng được bán với giá đó.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link ChuongTrinhKhuyenMai} (FK ctkm_id, ON DELETE CASCADE)
 *  - N:1 với {@link BienTheSanPham}        (FK bien_the_san_pham_id)
 */
@Entity
@Table(name = "chi_tiet_flash_sale")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietFlashSale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Chương trình flash sale chứa dòng này.
     * ON DELETE CASCADE – xóa chương trình thì chi tiết tự xóa.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ctkm_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctfs_ctkm")
    )
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    /**
     * Biến thể sản phẩm được áp giá flash.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctfs_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /** Phần trăm giảm giá (VD: 10 cho 10%). */
    @Column(name = "gia_flash", nullable = false, precision = 15, scale = 2)
    private BigDecimal phanTramGiam;

    /**
     * Số lượng tối đa được bán với giá flash.
     * Khi daBan >= soLuongGioiHan, khuyến mãi flash tự động kết thúc cho biến thể này.
     */
    @Column(name = "so_luong_gioi_han", nullable = false)
    private Integer soLuongGioiHan;

    /**
     * Số lượng đã bán với giá flash (DEFAULT 0).
     * Service Layer kiểm tra: daBan < soLuongGioiHan trước khi cho phép áp giá flash.
     */
    @Column(name = "da_ban", nullable = false)
    @Builder.Default
    private Integer daBan = 0;
}
