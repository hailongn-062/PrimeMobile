package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng hoi_dap_san_pham (Module 12: Đánh giá & Hỏi đáp).
 * <p>
 * Khách hàng đặt câu hỏi về sản phẩm trên trang chi tiết sản phẩm.
 * Nhân viên / Admin trả lời trực tiếp trong cùng bản ghi.
 * <p>
 * Ghi chú (system_rules.md §7 – Tạm hoãn):
 *  Phân hệ hỏi đáp tạm hoãn triển khai.
 *  Entity tạo đủ để mapping DB.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link SanPham}    (FK san_pham_id)
 *  - N:1 với {@link KhachHang}  (FK khach_hang_id — người đặt câu hỏi)
 *  - N:1 với {@link NguoiDung}  (FK nguoi_tra_loi_id, ON DELETE SET NULL, nullable)
 */
@Entity
@Table(
        name = "hoi_dap_san_pham",
        indexes = {
                @Index(name = "idx_hdsp_sp", columnList = "san_pham_id, hien_thi")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HoiDapSanPham {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Sản phẩm được hỏi.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "san_pham_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdsp_sp")
    )
    private SanPham sanPham;

    /**
     * Khách hàng đặt câu hỏi.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_hdsp_kh")
    )
    private KhachHang khachHang;

    /** Nội dung câu hỏi của khách. */
    @Column(name = "cau_hoi", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String cauHoi;

    /**
     * Nội dung câu trả lời của nhân viên/Admin.
     * NULL nếu chưa có ai trả lời.
     */
    @Column(name = "tra_loi", columnDefinition = "NVARCHAR(MAX)")
    private String traLoi;

    /**
     * Nhân viên / Admin trả lời câu hỏi.
     * ON DELETE SET NULL – xóa nhân viên vẫn giữ nội dung trả lời.
     * NULL nếu chưa được trả lời.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nguoi_tra_loi_id",
            foreignKey = @ForeignKey(name = "fk_hdsp_nd")
    )
    private NguoiDung nguoiTraLoi;

    /** Thời điểm khách đặt câu hỏi. */
    @Column(name = "ngay_hoi", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayHoi = LocalDateTime.now();

    /**
     * Thời điểm nhân viên trả lời.
     * NULL nếu chưa được trả lời.
     */
    @Column(name = "ngay_tra_loi")
    private LocalDateTime ngayTraLoi;

    /**
     * Trạng thái hiển thị công khai (DEFAULT true).
     * false = ẩn khỏi trang sản phẩm (do vi phạm nội quy...).
     */
    @Column(name = "hien_thi", nullable = false)
    @Builder.Default
    private Boolean hienThi = true;
}
