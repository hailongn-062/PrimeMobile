package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Entity mapping bảng phieu_bao_hanh (Module 8: Bảo hành).
 * <p>
 * Phiếu bảo hành được cấp tự động khi đơn hàng chuyển sang trạng thái 'da_giao'.
 * Mỗi máy vật lý ({@link MayDienThoai}) chỉ có tối đa 1 phiếu bảo hành
 * (ràng buộc UNIQUE trên may_dien_thoai_id).
 * <p>
 * Luồng bảo hành (system_rules.md §5):
 *  Nhận từ khách → kiểm tra IMEI → tra cứu phiếu bảo hành hợp lệ còn hạn
 *  (trang_thai = 'con_hieu_luc') → tạo {@link YeuCauBaoHanh}.
 * <p>
 * Ghi chú: Tính năng bảo hành tạm hoãn triển khai (system_rules.md §7),
 * nhưng Entity vẫn được tạo đầy đủ để mapping đúng DB.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_pbh_trang_thai):
 *  "con_hieu_luc" | "het_han" | "da_su_dung" | "void"
 * <p>
 * Quan hệ:
 *  - 1:1 với {@link MayDienThoai}   (FK may_dien_thoai_id, UNIQUE — 1 máy 1 phiếu)
 *  - N:1 với {@link KhachHang}      (FK khach_hang_id)
 *  - N:1 với {@link DonHang}        (FK don_hang_id)
 *  - 1-N với {@link YeuCauBaoHanh}  (mappedBy phieuBaoHanh)
 */
@Entity
@Table(
        name = "phieu_bao_hanh",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pbh_ma",  columnNames = "ma_phieu"),
                @UniqueConstraint(name = "uq_pbh_may", columnNames = "may_dien_thoai_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "yeuCauBaoHanhs")
@EqualsAndHashCode(exclude = "yeuCauBaoHanhs")
public class PhieuBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã phiếu bảo hành – duy nhất (ví dụ: "BH-2024-001"). */
    @Column(name = "ma_phieu", nullable = false, length = 50)
    private String maPhieu;

    /**
     * Máy điện thoại vật lý được bảo hành.
     * UNIQUE – mỗi máy chỉ có đúng 1 phiếu bảo hành.
     * NOT NULL.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "may_dien_thoai_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(name = "fk_pbh_may")
    )
    private MayDienThoai mayDienThoai;

    /**
     * Khách hàng được cấp phiếu bảo hành này.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pbh_kh")
    )
    private KhachHang khachHang;

    /**
     * Đơn hàng phát sinh phiếu bảo hành này.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pbh_dh")
    )
    private DonHang donHang;

    /** Thời hạn bảo hành tính bằng số tháng (lấy từ san_pham.bao_hanh_thang). */
    @Column(name = "so_thang_bao_hanh", nullable = false)
    private Integer soThangBaoHanh;

    /** Ngày bắt đầu hiệu lực bảo hành (thường = ngày giao hàng thực tế). */
    @Column(name = "ngay_bat_dau", nullable = false)
    private LocalDate ngayBatDau;

    /** Ngày hết hạn bảo hành = ngayBatDau + soThangBaoHanh. */
    @Column(name = "ngay_het_han", nullable = false)
    private LocalDate ngayHetHan;

    /**
     * Trạng thái phiếu bảo hành (DEFAULT 'con_hieu_luc').
     * Giá trị hợp lệ: "con_hieu_luc" | "het_han" | "da_su_dung" | "void"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "con_hieu_luc";

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 PhieuBaoHanh → nhiều YeuCauBaoHanh
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phieuBaoHanh", fetch = FetchType.LAZY)
    @Builder.Default
    private java.util.List<YeuCauBaoHanh> yeuCauBaoHanhs = new java.util.ArrayList<>();
}
