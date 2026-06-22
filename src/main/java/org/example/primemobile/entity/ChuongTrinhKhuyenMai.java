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
 * gioFlashBatDau / gioFlashKetThuc / soLuongToiDa có giá trị.
 * <p>
 * Phân loại (CHECK chk_ctkm_loai):
 *  "giam_gia_truc_tiep" | "phan_tram" | "ma_code" | "flash_sale" | "don_hang_toi_thieu"
 * <p>
 * Trạng thái (CHECK chk_ctkm_trang_thai):
 *  "chua_bat_dau" | "dang_dien_ra" | "da_ket_thuc" | "tam_dung"
 * <p>
 * Quan hệ:
 *  - 1-N với {@link PhamViKhuyenMai}   (mappedBy chuongTrinhKhuyenMai, CASCADE ALL)
 *  - 1-N với {@link MaGiamGia}         (mappedBy chuongTrinhKhuyenMai)
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
@ToString(exclude = {"phamViKhuyenMais", "maGiamGias", "chiTietFlashSales"})
@EqualsAndHashCode(exclude = {"phamViKhuyenMais", "maGiamGias", "chiTietFlashSales"})
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
     * Giá trị hợp lệ: "giam_gia_truc_tiep" | "phan_tram" | "ma_code"
     *                | "flash_sale" | "don_hang_toi_thieu"
     */
    @Column(name = "loai", nullable = false, length = 25)
    private String loai;

    /**
     * Giá trị ưu đãi:
     *  - laPhanTram = false: số tiền giảm cố định (đồng).
     *  - laPhanTram = true : phần trăm giảm (ví dụ: 10 = 10%).
     */
    @Column(name = "gia_tri_uu_dai", precision = 15, scale = 2)
    private BigDecimal giaTriUuDai;

    /**
     * Xác định giá trị ưu đãi là phần trăm hay số tiền tuyệt đối (DEFAULT false).
     * false = số tiền | true = phần trăm (%).
     */
    @Column(name = "la_phan_tram", nullable = false)
    @Builder.Default
    private Boolean laPhanTram = false;

    /**
     * Giảm tối đa (áp dụng khi laPhanTram = true).
     * Ví dụ: giảm 10% nhưng tối đa 500.000đ.
     * NULL = không giới hạn mức giảm.
     */
    @Column(name = "giam_toi_da", precision = 15, scale = 2)
    private BigDecimal giamToiDa;

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

    /**
     * Tổng số lượng sản phẩm tối đa được áp giá flash.
     * NULL = không giới hạn số lượng (áp dụng cho loại khác).
     */
    @Column(name = "so_luong_toi_da")
    private Integer soLuongToiDa;

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

    /** Danh sách mã giảm giá thuộc chương trình này. */
    @OneToMany(mappedBy = "chuongTrinhKhuyenMai", fetch = FetchType.LAZY)
    @Builder.Default
    private List<MaGiamGia> maGiamGias = new ArrayList<>();

    /** Chi tiết giá flash sale cho từng biến thể (ON DELETE CASCADE). */
    @OneToMany(mappedBy = "chuongTrinhKhuyenMai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietFlashSale> chiTietFlashSales = new ArrayList<>();
}
