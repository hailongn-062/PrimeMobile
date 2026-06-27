package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng chi_tiet_chuyen_kho (Module 3: Kho hàng).
 * <p>
 * Mỗi dòng chi tiết = 1 SKU được chuyển với số lượng cụ thể trong 1 phiếu chuyển.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link PhieuChuyenKho} (FK phieu_chuyen_id, ON DELETE CASCADE)
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id)
 */
@Entity
@Table(name = "chi_tiet_chuyen_kho")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietChuyenKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Phiếu chuyển kho chứa dòng chi tiết này.
     * ON DELETE CASCADE – xóa phiếu chuyển thì chi tiết tự xóa theo.
     */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "phieu_chuyen_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctck_phieu")
    )
    private PhieuChuyenKho phieuChuyenKho;

    /**
     * Biến thể sản phẩm (SKU) được chuyển trong dòng này.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ctck_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * Số lượng chuyển trong dòng này.
     * Phải > 0 (kiểm tra tại Service Layer).
     * ⚠️ Service Layer cần kiểm tra Safety Stock Rule (§3.1) trước khi chấp nhận:
     *    Sau khi trừ, tồn kho kho nguồn không được < 5.
     */
    @Column(name = "so_luong", nullable = false)
    private Integer soLuong;
}
