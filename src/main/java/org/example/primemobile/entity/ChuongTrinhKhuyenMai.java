package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng chuong_trinh_khuyen_mai (Module 9: Khuyến mãi).
 * <p>
 * Gộp chương trình khuyến mãi thông thường và Flash Sale vào cùng 1 bảng.
 * Flash Sale được phân biệt bằng loai = 'flash_sale' và các trường
 * gioFlashBatDau / gioFlashKetThuc có giá trị.
 * <p>
 * Phân loại (CHECK chk_ctkm_loai):
 *  "giam_gia_truc_tiep" | "phan_tram" | "flash_sale" | "don_hang_toi_thieu"
 * <p>
 * Trạng thái (CHECK chk_ctkm_trang_thai):
 *  "chua_bat_dau" | "dang_dien_ra" | "da_ket_thuc" | "tam_dung"
 * <p>
 * Quan hệ:
 *  - 1-N với {@link PhamViKhuyenMai}   (mappedBy chuongTrinhKhuyenMai, CASCADE ALL)
 *  - 1-N với {@link ChiTietFlashSale}  (mappedBy chuongTrinhKhuyenMai, CASCADE ALL)
 */
@Entity
@Table(
        name = "chuong_trinh_khuyen_mai",
        indexes = {
                @Index(name = "idx_ctkm_tts", columnList = "trang_thai, ngay_bat_dau, ngay_ket_thuc")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"phamViKhuyenMais", "chiTietFlashSales"})
@EqualsAndHashCode(exclude = {"phamViKhuyenMais", "chiTietFlashSales"})
public class ChuongTrinhKhuyenMai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Tên chương trình khuyến mãi. */
    @Column(name = "ten_ctkm", nullable = false, length = 200)
    private String tenCtkm;

    /** Mô tả chi tiết về chương trình. */
    @Column(name = "mo_ta", columnDefinition = "NVARCHAR(MAX)")
    private String moTa;

    /**
     * Loại khuyến mãi (NOT NULL).
     * Giá trị hợp lệ: "giam_gia_truc_tiep" | "phan_tram" 
     *                | "flash_sale" | "don_hang_toi_thieu"
     */
    @Column(name = "loai", nullable = false, length = 25)
    private String loai;

    /** Giá trị ưu đãi. */
    @Column(name = "gia_tri_uu_dai", precision = 15, scale = 2)
    private BigDecimal giaTriUuDai;

    /**
     * Đơn hàng tối thiểu (áp dụng khi loai = 'don_hang_toi_thieu').
     */
    @Column(name = "don_hang_toi_thieu", precision = 15, scale = 2)
    private BigDecimal donHangToiThieu;


    /** Ngày giờ bắt đầu chương trình. */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDateTime ngayBatDau;

    /** Ngày giờ kết thúc chương trình. */
    @Column(name = "ngay_ket_thuc", nullable = false)
    private LocalDateTime ngayKetThuc;

    // -------------------------------------------------------------------------
    // FLASH SALE: các trường chỉ có giá trị khi loai = 'flash_sale'
    // -------------------------------------------------------------------------

    /** Giờ bắt đầu flash sale trong ngày. NULL nếu không phải flash sale. */
    @Column(name = "gio_flash_bat_dau")
    private LocalDateTime gioFlashBatDau;

    /** Giờ kết thúc flash sale trong ngày. NULL nếu không phải flash sale. */
    @Column(name = "gio_flash_ket_thuc")
    private LocalDateTime gioFlashKetThuc;


    /** Đếm số lần mã đã được sử dụng (DEFAULT 0). */
    @Column(name = "so_lan_da_dung", nullable = false)
    @Builder.Default
    private Integer soLanDaDung = 0;

    /**
     * Trạng thái chương trình (DEFAULT 'chua_bat_dau').
     * Giá trị hợp lệ: "chua_bat_dau" | "dang_dien_ra" | "da_ket_thuc" | "tam_dung"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "chua_bat_dau";

    // -------------------------------------------------------------------------
    // Quan hệ 1-N
    // -------------------------------------------------------------------------

    /** Danh sách phạm vi áp dụng (sản phẩm, danh mục, hãng). */
    @OneToMany(mappedBy = "chuongTrinhKhuyenMai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PhamViKhuyenMai> phamViKhuyenMais = new ArrayList<>();

    /** Chi tiết giá flash sale cho từng biến thể (ON DELETE CASCADE). */
    @OneToMany(mappedBy = "chuongTrinhKhuyenMai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietFlashSale> chiTietFlashSales = new ArrayList<>();
}
