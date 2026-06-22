package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Entity mapping bảng thong_so_ky_thuat (Module 2: Sản phẩm & Biến thể).
 * <p>
 * Lưu trữ thông số kỹ thuật dạng key-value theo từng nhóm của một sản phẩm.
 * Ví dụ: Nhóm "Màn hình" → "Kích thước" → "6.7 inch".
 * <p>
 * Quan hệ:
 *  - N:1 với {@link SanPham} (FK san_pham_id, ON DELETE CASCADE)
 * <p>
 * Thông số kỹ thuật gắn với SanPham (model), không phải BienTheSanPham,
 * vì các thông số như CPU, màn hình, camera thường giống nhau giữa các biến thể.
 * Thông số riêng của từng biến thể (RAM, ROM, pin) đã có cột riêng trong bien_the_san_pham.
 */
@Entity
@Table(
        name = "thong_so_ky_thuat",
        indexes = {
                @Index(name = "idx_tskt_sp", columnList = "san_pham_id, nhom")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThongSoKyThuat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sản phẩm mà thông số này thuộc về.
     * ON DELETE CASCADE – xóa sản phẩm thì thông số tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tskt_sp")
    )
    private SanPham sanPham;

    /**
     * Nhóm thông số kỹ thuật (DEFAULT 'Thông tin chung').
     * Ví dụ: "Màn hình", "Camera", "Hiệu năng", "Kết nối", "Pin & Sạc".
     */
    @Column(name = "nhom", nullable = false, length = 100)
    @Builder.Default
    private String nhom = "Thông tin chung";

    /**
     * Tên thông số kỹ thuật.
     * Ví dụ: "Kích thước màn hình", "Độ phân giải", "CPU".
     */
    @Column(name = "ten_thong_so", nullable = false, length = 100)
    private String tenThongSo;

    /**
     * Giá trị của thông số.
     * Ví dụ: "6.7 inch", "2796 x 1290 pixels", "Apple A17 Pro".
     */
    @Column(name = "gia_tri", nullable = false, length = 255)
    private String giaTri;

    /** Thứ tự hiển thị trong nhóm (DEFAULT 0). */
    @Column(name = "thu_tu", nullable = false)
    @Builder.Default
    private Integer thuTu = 0;
}
