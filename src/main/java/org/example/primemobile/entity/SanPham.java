package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Entity mapping bảng san_pham (Module 2: Sản phẩm & Biến thể).
 * <p>
 * PrimeMobile CHỈ bán điện thoại — không bán phụ kiện hay các loại khác.
 * Mỗi sản phẩm là một model điện thoại (ví dụ: iPhone 15 Pro Max).
 * Các màu sắc, dung lượng RAM/ROM cụ thể được quản lý ở {@link BienTheSanPham}.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link DanhMuc}  (FK danh_muc_id)
 *  - N:1 với {@link HangSanXuat} (FK hang_san_xuat_id)
 *  - 1:N với {@link BienTheSanPham} (mappedBy sanPham, CASCADE DELETE)
 *  - 1:N với {@link ThongSoKyThuat} (mappedBy sanPham, CASCADE DELETE)
 * <p>
 * Trạng thái hợp lệ (CHECK chk_sp_trang_thai):
 *  "dang_ban" | "ngung_ban" | "sap_ra_mat"
 */
@Entity
@Table(
        name = "san_pham",
        indexes = {
                @Index(name = "idx_sp_ten",  columnList = "ten_san_pham"),
                @Index(name = "idx_sp_dm",   columnList = "danh_muc_id, trang_thai"),
                @Index(name = "idx_sp_hang", columnList = "hang_san_xuat_id, trang_thai")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_sp_ma", columnNames = "ma_san_pham")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"bienTheSanPhams", "thongSoKyThuats"})
@EqualsAndHashCode(exclude = {"bienTheSanPhams", "thongSoKyThuats"})
public class SanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã sản phẩm nội bộ – duy nhất (ví dụ: "IP15PM-256"). */
    @Column(name = "ma_san_pham", nullable = false, length = 50, unique = true)
    private String maSanPham;

    /** Tên sản phẩm đầy đủ (ví dụ: "iPhone 15 Pro Max"). */
    @Column(name = "ten_san_pham", nullable = false, length = 255)
    private String tenSanPham;

    /**
     * Danh mục chứa sản phẩm.
     * NOT NULL – mọi sản phẩm đều phải thuộc một danh mục.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "danh_muc_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_dm")
    )
    private DanhMuc danhMuc;

    /**
     * Hãng sản xuất của sản phẩm.
     * NOT NULL – mọi sản phẩm đều phải có hãng.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "hang_san_xuat_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_sp_hsx")
    )
    private HangSanXuat hangSanXuat;

    /** Mô tả ngắn hiển thị trên card sản phẩm (tối đa 500 ký tự). */
    @Column(name = "mo_ta_ngan", length = 500)
    private String moTaNgan;

    /** Mô tả chi tiết đầy đủ (HTML content). */
    @Column(name = "mo_ta_chi_tiet", columnDefinition = "NVARCHAR(MAX)")
    private String moTaChiTiet;

    /** Năm ra mắt sản phẩm (ví dụ: 2024). SMALLINT ↔ Short. */
    @Column(name = "nam_ra_mat")
    private Short namRaMat;

    /** Số tháng bảo hành theo chính sách cửa hàng (DEFAULT 12). */
    @Column(name = "bao_hanh_thang", nullable = false)
    @Builder.Default
    private Integer baoHanhThang = 12;

    /**
     * Trạng thái kinh doanh của sản phẩm (DEFAULT 'dang_ban').
     * Giá trị hợp lệ: "dang_ban" | "ngung_ban" | "sap_ra_mat"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "dang_ban";

    /** Lượt xem tích lũy (DEFAULT 0). */
    @Column(name = "luot_xem", nullable = false)
    @Builder.Default
    private Integer luotXem = 0;

    /** Thời điểm tạo bản ghi. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật bản ghi gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 SanPham → nhiều BienTheSanPham (ON DELETE CASCADE ở DB)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<BienTheSanPham> bienTheSanPhams = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 SanPham → nhiều ThongSoKyThuat (ON DELETE CASCADE ở DB)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "sanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @JsonIgnore
    private List<ThongSoKyThuat> thongSoKyThuats = new ArrayList<>();
}
