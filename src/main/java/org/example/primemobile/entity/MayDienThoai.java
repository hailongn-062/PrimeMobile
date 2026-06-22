package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng may_dien_thoai (Module 2: Sản phẩm & Biến thể).
 * <p>
 * Theo dõi từng chiếc điện thoại vật lý thông qua mã IMEI.
 * Đây là đơn vị quản lý nhỏ nhất trong chuỗi cung ứng:
 * NCC → Kho Tổng (IMEI gắn vào đây) → Bán ra (linked với don_hang).
 * <p>
 * Quan hệ:
 *  - N:1 với {@link BienTheSanPham} (FK bien_the_san_pham_id)
 *  - N:1 với {@link DonHang} (FK don_hang_id, ON DELETE SET NULL, nullable — refactored từ Module 7)
 * <p>
 * Ràng buộc nghiệp vụ quan trọng (system_rules.md §3.3):
 *  - Số lượng bản ghi IMEI có trạng thái 'trong_kho' của một SKU
 *    KHÔNG ĐƯỢC vượt quá số lượng tồn kho trong bảng ton_kho.
 *  - Khi bán hàng offline: cập nhật tinh_trang → 'da_ban', gán don_hang_id.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_may_tinh_trang):
 *  "trong_kho" | "da_ban" | "bao_hanh" | "loi_hong"
 */
@Entity
@Table(
        name = "may_dien_thoai",
        indexes = {
                @Index(name = "idx_imei1",          columnList = "imei1"),
                @Index(name = "idx_may_tinh_trang", columnList = "tinh_trang")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_may_imei1",  columnNames = "imei1"),
                @UniqueConstraint(name = "uq_may_imei2",  columnNames = "imei2"),
                @UniqueConstraint(name = "uq_may_serial", columnNames = "serial")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MayDienThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Biến thể sản phẩm mà chiếc máy này thuộc về.
     * NOT NULL – mỗi máy vật lý phải gắn với đúng 1 SKU.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "bien_the_san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_may_bt")
    )
    private BienTheSanPham bienTheSanPham;

    /**
     * IMEI khe SIM 1 – bắt buộc, duy nhất toàn hệ thống.
     * Chuẩn GSMA: 15 chữ số.
     */
    @Column(name = "imei1", nullable = false, length = 15, unique = true)
    private String imei1;

    /**
     * IMEI khe SIM 2 – tùy chọn (máy 1 SIM thì NULL).
     * Duy nhất nếu có.
     */
    @Column(name = "imei2", length = 15, unique = true)
    private String imei2;

    /**
     * Số serial của máy – tùy chọn, duy nhất nếu có.
     */
    @Column(name = "serial", length = 50, unique = true)
    private String serial;

    /**
     * Tình trạng hiện tại của máy vật lý (DEFAULT 'trong_kho').
     * Giá trị hợp lệ: "trong_kho" | "da_ban" | "bao_hanh" | "loi_hong"
     */
    @Column(name = "tinh_trang", nullable = false, length = 15)
    @Builder.Default
    private String tinhTrang = "trong_kho";

    /** Ngày nhập kho vật lý của chiếc máy này. */
    @Column(name = "ngay_nhap_kho", nullable = false)
    @Builder.Default
    private LocalDateTime ngayNhapKho = LocalDateTime.now();

    /**
     * Đơn hàng đã bán chiếc máy này.
     * NULL = máy đang trong kho, chưa bán ra.
     * ON DELETE SET NULL – xóa đơn hàng không làm mất bản ghi IMEI.
     * FK fk_may_dh tương ứng với ALTER TABLE trong SQL (Module 7).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "don_hang_id",
            foreignKey = @ForeignKey(name = "fk_may_dh")
    )
    private DonHang donHang;

    /** Ghi chú nội bộ (hư hỏng, lịch sử sửa chữa...). */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;
}
