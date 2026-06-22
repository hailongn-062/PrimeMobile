package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng kho (Module 3: Kho hàng).
 * <p>
 * Hệ thống PrimeMobile chỉ có đúng 2 kho cố định:
 *  - "kho_tong"  : Kho vật lý tại cửa hàng, nhận hàng từ NCC, bán hàng offline.
 *  - "kho_online": Kho ảo phục vụ đơn hàng online (nguồn từ kho_tong chuyển sang).
 * <p>
 * Ràng buộc UNIQUE trên cột `loai` đảm bảo chỉ tồn tại đúng 1 bản ghi mỗi loại.
 * <p>
 * Quan hệ:
 *  - 1-N với {@link TonKho}        (mappedBy kho)
 *  - 1-N với {@link PhieuNhapKho}  (mappedBy kho)
 *  - 1-N với {@link PhieuChuyenKho} theo vai trò kho nguồn (mappedBy khoNguon)
 *  - 1-N với {@link PhieuChuyenKho} theo vai trò kho đích  (mappedBy khoDich)
 */
@Entity
@Table(
        name = "kho",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_kho_loai", columnNames = "loai")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"tonKhos", "phieuNhapKhos", "phieuChuyenKhoNguons", "phieuChuyenKhoDichs"})
@EqualsAndHashCode(exclude = {"tonKhos", "phieuNhapKhos", "phieuChuyenKhoNguons", "phieuChuyenKhoDichs"})
public class Kho {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Tên kho hiển thị (ví dụ: "Kho Tổng", "Kho Online"). */
    @Column(name = "ten_kho", nullable = false, length = 100)
    private String tenKho;

    /**
     * Loại kho – duy nhất trong hệ thống (DEFAULT constraint ở DB).
     * Giá trị hợp lệ: "kho_tong" | "kho_online"
     */
    @Column(name = "loai", nullable = false, length = 15, unique = true)
    private String loai;

    /** Địa chỉ vật lý của kho. NULL với kho_online. */
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    /** Trạng thái hoạt động của kho (DEFAULT true). */
    @Column(name = "kich_hoat", nullable = false)
    @Builder.Default
    private Boolean kichHoat = true;

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 Kho → nhiều TonKho (bản ghi tồn kho từng SKU trong kho này)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "kho", fetch = FetchType.LAZY)
    @Builder.Default
    private List<TonKho> tonKhos = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 Kho → nhiều PhieuNhapKho được nhập vào kho này
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "kho", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuNhapKho> phieuNhapKhos = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: Kho với tư cách là KHO NGUỒN trong phiếu chuyển kho
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "khoNguon", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuChuyenKho> phieuChuyenKhoNguons = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: Kho với tư cách là KHO ĐÍCH trong phiếu chuyển kho
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "khoDich", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuChuyenKho> phieuChuyenKhoDichs = new ArrayList<>();
}
