package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping bảng chi_tiet_phieu_nhap (Module 3: Kho hàng).
 * <p>
 * Mỗi dòng chi tiết = 1 SKU được nhập với số lượng và đơn giá nhập cụ thể.
 * <p>
 * ⚠️ QUAN TRỌNG – Cột tính toán (Computed Column):
 * Cột {@code thanh_tien} trong SQL Server được định nghĩa là:
 * {@code AS (so_luong * don_gia_nhap) PERSISTED}
 * Do đó, trường {@code thanhTien} PHẢI được khai báo với:
 * {@code @Column(insertable = false, updatable = false)}
 * để Hibernate KHÔNG cố gắng INSERT/UPDATE vào cột này,
 * tránh lỗi "Cannot update a computed column" làm sập ứng dụng.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link PhieuNhapKho}   (FK phieu_nhap_id, ON DELETE CASCADE)
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(name = "chi_tiet_phieu_nhap")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietPhieuNhap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Phiếu nhập kho chứa dòng chi tiết này.
     * ON DELETE CASCADE ở DB – xóa phiếu nhập thì chi tiết tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "phieu_nhap_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctpn_phieu")
    )
    private PhieuNhapKho phieuNhapKho;

    /**
     * Biến thể sản phẩm (SKU) được nhập trong dòng này.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctpn_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /** Số lượng nhập trong dòng này. Phải > 0. */
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;

    /** Đơn giá nhập (giá mua từ NCC) cho 1 đơn vị SKU. */
    @Column(name = "don_gia_nhap", nullable = false, precision = 15, scale = 2)
    private BigDecimal donGiaNhap;

    /**
     * ⚠️ CỘT TÍNH TOÁN TỰ ĐỘNG (Computed Column - PERSISTED):
     * Được SQL Server tự tính theo công thức: so_luong * don_gia_nhap
     * <p>
     * BẮT BUỘC dùng insertable = false, updatable = false:
     *  - Hibernate sẽ ĐỌC giá trị này từ DB sau khi INSERT/UPDATE.
     *  - Hibernate sẽ KHÔNG CỐ GẮNG ghi vào cột này, tránh lỗi runtime.
     */
    @Column(name = "thanh_tien", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal thanhTien;
}
