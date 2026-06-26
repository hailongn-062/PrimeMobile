package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping bảng hang_san_xuat (Module 2: Sản phẩm & Biến thể).
 * <p>
 * Quản lý thương hiệu điện thoại (Apple, Samsung, Xiaomi...).
 * <p>
 * Quan hệ: 1 HangSanXuat → N SanPham.
 */
@Entity
@Table(
        name = "hang_san_xuat",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_hsx_ten", columnNames = "ten_hang")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sanPhams")
@EqualsAndHashCode(exclude = "sanPhams")
public class HangSanXuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Tên thương hiệu – duy nhất (ví dụ: "Apple", "Samsung"). */
    @Column(name = "ten_hang", nullable = false, length = 100)
    private String tenHang;

    /** Đường dẫn logo thương hiệu (lưu URL hoặc đường dẫn tương đối). */
    @Column(name = "logo", length = 255)
    private String logo;

    /** Quốc gia sản xuất (ví dụ: "Mỹ", "Hàn Quốc"). */
    @Column(name = "quoc_gia", length = 50)
    private String quocGia;

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 HangSanXuat → nhiều SanPham
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "hangSanXuat", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SanPham> sanPhams = new ArrayList<>();
}
