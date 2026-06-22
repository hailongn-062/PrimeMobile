package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng phieu_chuyen_kho (Module 3: Kho hàng).
 * <p>
 * Ghi nhận nghiệp vụ chuyển hàng giữa 2 kho.
 * Luồng tiêu chuẩn (system_rules.md §3.2):
 *  Kho Tổng → Kho Online (để bổ sung hàng cho đơn online)
 *  Kho Online → Kho Tổng (hoàn hàng về)
 * <p>
 * Hệ thống chốt luôn (no-approval workflow):
 * Khi nhân viên xác nhận tạo phiếu, tồn kho 2 bên tự động tăng/giảm ngay.
 * <p>
 * ⚠️ Ràng buộc DB: CHECK (kho_nguon_id <> kho_dich_id) — không tự chuyển vào chính mình.
 * Validation này cần được kiểm tra thêm ở Service Layer trước khi lưu.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_pck_trang_thai): "hoan_thanh" | "huy"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link Kho}                (FK kho_nguon_id — kho xuất hàng)
 *  - N:1 với {@link Kho}                (FK kho_dich_id  — kho nhận hàng)
 *  - N:1 với {@link NguoiDung}          (FK nguoi_tao_id)
 *  - 1-N với {@link ChiTietChuyenKho}   (mappedBy phieuChuyenKho, CASCADE DELETE)
 */
@Entity
@Table(
        name = "phieu_chuyen_kho",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pck_ma", columnNames = "ma_phieu")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "chiTietChuyenKhos")
@EqualsAndHashCode(exclude = "chiTietChuyenKhos")
public class PhieuChuyenKho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã phiếu chuyển kho – duy nhất (ví dụ: "PCK-2024-001"). */
    @Column(name = "ma_phieu", nullable = false, length = 50, unique = true)
    private String maPhieu;

    /**
     * Kho xuất hàng (nguồn).
     * ⚠️ Phải khác khoDich – kiểm tra tại Service Layer trước khi save.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_nguon_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pck_nguon")
    )
    private Kho khoNguon;

    /**
     * Kho nhận hàng (đích).
     * ⚠️ Phải khác khoNguon – kiểm tra tại Service Layer trước khi save.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "kho_dich_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pck_dich")
    )
    private Kho khoDich;

    /**
     * Nhân viên / Admin tạo phiếu chuyển kho.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "nguoi_tao_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_pck_nd")
    )
    private NguoiDung nguoiTao;

    /** Ngày giờ chuyển kho (DEFAULT GETDATE()). */
    @Column(name = "ngay_chuyen", nullable = false)
    @Builder.Default
    private LocalDateTime ngayChuyen = LocalDateTime.now();

    /** Lý do chuyển kho (bổ sung hàng online, hoàn kho, kiểm kê...). */
    @Column(name = "ly_do", length = 255)
    private String lyDo;

    /**
     * Trạng thái phiếu chuyển kho (DEFAULT 'hoan_thanh').
     * Chốt luôn khi tạo – không cần bước duyệt (system_rules.md §3.2).
     * Giá trị hợp lệ: "hoan_thanh" | "huy"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "hoan_thanh";

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 PhieuChuyenKho → nhiều ChiTietChuyenKho (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "phieuChuyenKho", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietChuyenKho> chiTietChuyenKhos = new ArrayList<>();
}
