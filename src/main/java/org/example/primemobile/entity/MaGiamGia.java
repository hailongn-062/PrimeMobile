package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Entity mapping bảng ma_giam_gia (Module 9: Khuyến mãi).
 * <p>
 * Mã coupon/voucher mà khách hàng nhập để được giảm giá trên tổng hóa đơn.
 * Mỗi mã thuộc về đúng 1 chương trình khuyến mãi loại "ma_code".
 * <p>
 * Sau khi Entity này được tạo, thuộc tính {@code maGiamGiaId} trong
 * {@link DonHang} sẽ được refactor thành {@code @ManyToOne MaGiamGia}.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link ChuongTrinhKhuyenMai} (FK ctkm_id)
 */
@Entity
@Table(
        name = "ma_giam_gia",
        indexes = {
                @Index(name = "idx_mgg_code", columnList = "ma_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_mgg_code", columnNames = "ma_code")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MaGiamGia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Chương trình khuyến mãi cha của mã này.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "ctkm_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_mgg_ctkm")
    )
    private ChuongTrinhKhuyenMai chuongTrinhKhuyenMai;

    /**
     * Mã code khách nhập khi thanh toán – duy nhất toàn hệ thống.
     * Ví dụ: "SUMMER2024", "NEWUSER50K".
     */
    @Column(name = "ma_code", nullable = false, length = 50, unique = true)
    private String maCode;

    /**
     * Giá trị đơn hàng tối thiểu để áp dụng mã (DEFAULT 0 = không giới hạn).
     */
    @Column(name = "don_hang_toi_thieu", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal donHangToiThieu = BigDecimal.ZERO;

    /**
     * Mức giảm tối đa khi áp mã (áp dụng cho loại giảm theo %).
     * NULL = không giới hạn mức giảm.
     */
    @Column(name = "giam_toi_da", precision = 15, scale = 2)
    private BigDecimal giamToiDa;

    /** Tổng số lượt mã được phép sử dụng (DEFAULT 1). */
    @Column(name = "so_luong_toi_da", nullable = false)
    @Builder.Default
    private Integer soLuongToiDa = 1;

    /**
     * Số lượt mã đã được sử dụng thực tế (DEFAULT 0).
     * Service Layer kiểm tra: daSuDung < soLuongToiDa trước khi cho phép dùng.
     */
    @Column(name = "da_su_dung", nullable = false)
    @Builder.Default
    private Integer daSuDung = 0;
}
