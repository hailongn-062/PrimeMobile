package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng phieu_nhap_kho (Module 3: Kho hàng).
 * <p>
 * Ghi nhận nghiệp vụ nhập hàng từ Nhà cung cấp vào Kho Tổng.
 * Khi phiếu nhập có trạng thái 'hoan_thanh', Service Layer phải tự động
 * cộng số lượng vào bảng ton_kho của kho_tong tương ứng.
 * <p>
 * Luồng nghiệp vụ (system_rules.md §3.2):
 *  NCC → phieu_nhap_kho (hoan_thanh) → ton_kho (kho_tong) tăng lên.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_pnk_trang_thai): "hoan_thanh" | "huy"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link Kho}               (FK kho_id — luôn là kho_tong)
 *  - N:1 với {@link NhaCungCap}        (FK nha_cung_cap_id, nullable — refactored từ Module 4)
 *  - N:1 với {@link NguoiDung}         (FK nguoi_tao_id)
 *  - 1-N với {@link ChiTietPhieuNhap} (mappedBy phieuNhapKho, CASCADE DELETE)
 */
@Entity
@Table(
        name = "phieu_nhap_kho",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pnk_ma", columnNames = "ma_phieu")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "chiTietPhieuNhaps")
@EqualsAndHashCode(exclude = "chiTietPhieuNhaps")
public class PhieuNhapKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã phiếu nhập – duy nhất (ví dụ: "PNK-2024-001"). */
    @Column(name = "ma_phieu", nullable = false, length = 50, unique = true)
    private String maPhieu;

    /**
     * Kho nhận hàng (luôn là Kho Tổng theo nghiệp vụ).
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnk_kho")
    )
    private Kho kho;

    /**
     * Nhà cung cấp cung ứng lô hàng này.
     * Nullable — phiếu nhập vẫn hợp lệ khi không xác định được NCC.
     * FK fk_pnk_ncc tương ứng với ALTER TABLE trong SQL (Module 4).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nha_cung_cap_id",
            foreignKey = @ForeignKey(name = "fk_pnk_ncc")
    )
    private NhaCungCap nhaCungCap;

    /**
     * Nhân viên / Admin tạo phiếu nhập.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "nguoi_tao_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pnk_nd")
    )
    private NguoiDung nguoiTao;

    /** Ngày giờ nhập hàng thực tế (DEFAULT GETDATE()). */
    @Column(name = "ngay_nhap", nullable = false)
    @Builder.Default
    private LocalDateTime ngayNhap = LocalDateTime.now();

    /** Tổng tiền của phiếu nhập (tự cộng từ các dòng chi tiết). */
    @Column(name = "tong_tien", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tongTien = BigDecimal.ZERO;

    /**
     * Trạng thái phiếu nhập (DEFAULT 'hoan_thanh').
     * Hệ thống chốt luôn khi tạo phiếu — không qua bước chờ duyệt (system_rules.md §3.2).
     * Giá trị hợp lệ: "hoan_thanh" | "huy"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "hoan_thanh";

    /** Ghi chú nội bộ về lô hàng nhập. */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 PhieuNhapKho → nhiều ChiTietPhieuNhap (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phieuNhapKho", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietPhieuNhap> chiTietPhieuNhaps = new ArrayList<>();
}
