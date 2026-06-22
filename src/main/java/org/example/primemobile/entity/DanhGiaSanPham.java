package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng danh_gia_san_pham (Module 12: Đánh giá & Hỏi đáp).
 * <p>
 * Lưu đánh giá sản phẩm từ khách hàng sau khi mua hàng.
 * <p>
 * Ràng buộc nghiệp vụ (system_rules.md §5):
 *  Chỉ khách hàng có đơn hàng trạng thái 'da_giao' chứa sản phẩm đó
 *  mới được phép viết đánh giá.
 *  Kiểm tra điều kiện này tại Service Layer trước khi cho phép tạo bản ghi.
 * <p>
 * Ràng buộc UNIQUE (uq_dg): 1 khách chỉ được đánh giá 1 lần
 *  cho 1 sản phẩm trong 1 đơn hàng cụ thể.
 * <p>
 * Hình ảnh đính kèm ({@code hinhAnhJson}):
 *  Lưu dưới dạng JSON array đường dẫn, ví dụ: ["url1", "url2", "url3"].
 *  Parse/Serialize tại Service/DTO Layer.
 * <p>
 * Trạng thái (CHECK chk_dg_trang_thai):
 *  "cho_duyet" | "da_duyet" | "an"
 * <p>
 * Ghi chú: Tính năng đánh giá tạm hoãn (system_rules.md §7),
 * nhưng Entity vẫn phải tạo đủ để mapping DB.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link SanPham}    (FK san_pham_id)
 *  - N:1 với {@link KhachHang}  (FK khach_hang_id)
 *  - N:1 với {@link DonHang}    (FK don_hang_id)
 */
@Entity
@Table(
        name = "danh_gia_san_pham",
        indexes = {
                @Index(name = "idx_dg_sp", columnList = "san_pham_id, trang_thai")
        },
        uniqueConstraints = {
                // 1 khách chỉ được đánh giá 1 lần cho 1 sản phẩm trong 1 đơn hàng
                @UniqueConstraint(name = "uq_dg", columnNames = {"khach_hang_id", "don_hang_id", "san_pham_id"})
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DanhGiaSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sản phẩm được đánh giá.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dg_sp")
    )
    private SanPham sanPham;

    /**
     * Khách hàng viết đánh giá.
     * NOT NULL – chỉ khách có tài khoản mới được đánh giá.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dg_kh")
    )
    private KhachHang khachHang;

    /**
     * Đơn hàng liên kết với đánh giá (đảm bảo đã mua mới được đánh giá).
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dg_dh")
    )
    private DonHang donHang;

    /**
     * Số sao đánh giá.
     * CHECK: sao BETWEEN 1 AND 5.
     */
    @Column(name = "sao", nullable = false)
    private Integer sao;

    /** Tiêu đề ngắn của đánh giá (tùy chọn). */
    @Column(name = "tieu_de", length = 200)
    private String tieuDe;

    /** Nội dung đánh giá chi tiết. */
    @Column(name = "noi_dung", columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    /**
     * Mảng URL hình ảnh đính kèm lưu dưới dạng JSON string.
     * Ví dụ: '["https://...", "https://..."]'
     * Parse tại Service/DTO Layer khi cần hiển thị.
     * NULL nếu không đính kèm ảnh.
     */
    @Column(name = "hinh_anh_json", columnDefinition = "NVARCHAR(MAX)")
    private String hinhAnhJson;

    /**
     * Trạng thái kiểm duyệt (DEFAULT 'cho_duyet').
     * Giá trị hợp lệ: "cho_duyet" | "da_duyet" | "an"
     * Nhân viên có quyền duyệt/ẩn đánh giá.
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "cho_duyet";

    /** Thời điểm tạo đánh giá. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();
}
