package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng bien_the_san_pham (Module 2: Sản phẩm & Biến thể).
 * <p>
 * Mỗi biến thể = 1 SKU cụ thể (màu sắc + RAM + ROM).
 * Ví dụ: iPhone 15 Pro Max – Titan Đen – 8GB RAM – 256GB.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link SanPham}      (FK san_pham_id, ON DELETE CASCADE)
 *  - 1:N với {@link MayDienThoai} (mappedBy bienTheSanPham)
 *  - 1:N với {@link HinhAnhSanPham} (mappedBy bienTheSanPham, CASCADE DELETE)
 * <p>
 * Trạng thái hợp lệ (CHECK chk_bt_trang_thai):
 *  "con_hang" | "het_hang" | "ngung_kinh_doanh"
 */
@Entity
@Table(
        name = "bien_the_san_pham",
        indexes = {
                @Index(name = "idx_bt_sp",      columnList = "san_pham_id, trang_thai")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_bt_ma_sku",  columnNames = "ma_sku")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"mayDienThoais", "hinhAnhSanPhams"})
@EqualsAndHashCode(exclude = {"mayDienThoais", "hinhAnhSanPhams"})
public class BienTheSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sản phẩm cha của biến thể này.
     * NOT NULL, ON DELETE CASCADE (DB-level).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_bt_sp")
    )
    private SanPham sanPham;

    /** Mã SKU – duy nhất trong toàn hệ thống (ví dụ: "IP15PM-TIT-8-256"). */
    @Column(name = "ma_sku", nullable = false, length = 100, unique = true)
    private String maSku;



    /** Tên màu sắc (ví dụ: "Titan Đen", "Titan Trắng"). */
    @Column(name = "mau_sac", nullable = false, length = 50)
    private String mauSac;

    /**
     * Mã màu hex để hiển thị ô màu trên UI (ví dụ: "#1C1C1E").
     * Nullable nếu không cần hiển thị ô màu.
     */
    @Column(name = "ma_mau_hex", length = 7)
    private String maMauHex;

    /** Dung lượng RAM tính bằng GB. */
    @Column(name = "ram_gb", nullable = false)
    private Integer ramGb;

    /** Dung lượng lưu trữ tính bằng GB. */
    @Column(name = "luu_tru_gb", nullable = false)
    private Integer luuTruGb;

    /**
     * Loại bộ nhớ trong (DEFAULT 'UFS').
     * Ví dụ: "UFS", "NVMe", "eMMC".
     */
    @Column(name = "loai_luu_tru", nullable = false, length = 20)
    @Builder.Default
    private String loaiLuuTru = "UFS";

    /** Giá nhập từ nhà cung cấp. */
    @Column(name = "gia_nhap", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaNhap;

    /** Giá bán lẻ niêm yết. */
    @Column(name = "gia_ban", nullable = false, precision = 15, scale = 2)
    private BigDecimal giaBan;

    /**
     * Giá khuyến mãi tạm thời.
     * NULL = không đang khuyến mãi.
     */
    @Column(name = "gia_khuyen_mai", precision = 15, scale = 2)
    private BigDecimal giaKhuyenMai;

    /** Trọng lượng máy tính bằng gram. */
    @Column(name = "trong_luong_gram")
    private Integer trongLuongGram;

    /**
     * Dung lượng pin tính bằng mAh.
     * Tên cột DB: pin_mAh (giữ nguyên mapping).
     */
    @Column(name = "pin_mAh")
    private Integer pinMah;

    /**
     * Trạng thái tồn kho của biến thể (DEFAULT 'con_hang').
     * Giá trị hợp lệ: "con_hang" | "het_hang" | "ngung_kinh_doanh"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "con_hang";

    /** Thời điểm tạo bản ghi. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật bản ghi gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 BienTheSanPham → nhiều MayDienThoai (IMEI tracking)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "bienTheSanPham", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MayDienThoai> mayDienThoais = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 BienTheSanPham → nhiều HinhAnhSanPham (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "bienTheSanPham", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HinhAnhSanPham> hinhAnhSanPhams = new ArrayList<>();
}
