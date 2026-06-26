package org.example.primemobile.entity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping bảng danh_muc (Module 2: Sản phẩm & Biến thể).
 * <p>
 * PrimeMobile chỉ bán điện thoại, nên danh mục được dùng để phân loại
 * dòng máy (ví dụ: Android cao cấp, iPhone, Flagship, Mid-range...).
 * <p>
 * Quan hệ: 1 DanhMuc → N SanPham.
 */
@Entity
@Table(
        name = "danh_muc",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dm_ten",  columnNames = "ten_danh_muc"),
                @UniqueConstraint(name = "uq_dm_slug", columnNames = "slug")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "sanPhams")
@EqualsAndHashCode(exclude = "sanPhams")
public class DanhMuc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Tên danh mục – duy nhất trong hệ thống. */
    @Column(name = "ten_danh_muc", nullable = false, length = 100)
    private String tenDanhMuc;

    /**
     * Slug URL-friendly cho SEO (ví dụ: "dien-thoai-android").
     * Duy nhất trong hệ thống.
     */
    @Column(name = "slug", nullable = false, length = 100, unique = true)
    private String slug;

    /** Mô tả danh mục, có thể NULL. */
    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    /** Thứ tự hiển thị trên giao diện (DEFAULT 0). */
    @Column(name = "thu_tu", nullable = false)
    @Builder.Default
    private Integer thuTu = 0;

    /**
     * Trạng thái kích hoạt (DEFAULT 1 = đang hoạt động).
     * true = đang hiển thị | false = đã ẩn.
     */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 DanhMuc → nhiều SanPham
    // @ToString.Exclude & @EqualsAndHashCode.Exclude trên class để tránh đệ quy
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "danhMuc", fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<SanPham> sanPhams = new ArrayList<>();
}
