package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng hinh_anh_san_pham (Module 2: Sản phẩm & Biến thể).
 * <p>
 * Lưu trữ các ảnh của từng biến thể sản phẩm.
 * Mỗi biến thể có thể có nhiều ảnh, trong đó có duy nhất 1 ảnh chính.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id, ON DELETE CASCADE)
 */
@Entity
@Table(name = "hinh_anh_san_pham")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HinhAnhSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Biến thể sản phẩm mà ảnh này thuộc về.
     * ON DELETE CASCADE – xóa biến thể thì ảnh tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hasp_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * Đường dẫn ảnh (URL tuyệt đối hoặc đường dẫn tương đối từ static/).
     * Ví dụ: "/images/products/ip15pm-titan-black-1.jpg"
     */
    @Column(name = "duong_dan", nullable = false, length = 255)
    private String duongDan;

    /**
     * Đánh dấu đây có phải ảnh đại diện chính của biến thể không.
     * DEFAULT false. Mỗi biến thể chỉ nên có 1 ảnh chính.
     */
    @Column(name = "la_anh_chinh", nullable = false)
    @Builder.Default
    private Boolean laAnhChinh = false;

    /** Thứ tự hiển thị trong gallery (DEFAULT 0 = hiển thị đầu tiên). */
    @Column(name = "thu_tu", nullable = false)
    @Builder.Default
    private Integer thuTu = 0;
}
