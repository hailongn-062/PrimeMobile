package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng nha_cung_cap (Module 4: Nhà cung cấp).
 * <p>
 * Quản lý danh sách nhà cung cấp điện thoại cho cửa hàng PrimeMobile.
 * <p>
 * Ghi chú (system_rules.md §3.2):
 *  Workflow đặt hàng NCC (purchase order) đã bị loại bỏ.
 *  Entity này chỉ dùng để tham chiếu trong {@link PhieuNhapKho}
 *  và {@link YeuCauBaoHanh} (khi gửi máy lỗi cho NCC xử lý).
 * <p>
 * Trạng thái hợp lệ (CHECK chk_ncc_trang_thai):
 *  "dang_hop_tac" | "ngung_hop_tac"
 * <p>
 * Quan hệ:
 *  - 1-N với {@link PhieuNhapKho} (mappedBy nhaCungCap)
 */
@Entity
@Table(
        name = "nha_cung_cap",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ncc_ma", columnNames = "ma_ncc")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "phieuNhapKhos")
@EqualsAndHashCode(exclude = "phieuNhapKhos")
public class NhaCungCap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Mã nhà cung cấp nội bộ – duy nhất (ví dụ: "NCC-001").
     * NOT NULL, UNIQUE.
     */
    @Column(name = "ma_ncc", nullable = false, length = 20, unique = true)
    private String maNcc;

    /** Tên đầy đủ của nhà cung cấp. */
    @Column(name = "ten_ncc", nullable = false, length = 200)
    private String tenNcc;

    /** Số điện thoại liên hệ của nhà cung cấp. */
    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    /** Email liên hệ của nhà cung cấp. */
    @Column(name = "email", length = 100)
    private String email;

    /** Địa chỉ trụ sở / kho của nhà cung cấp. */
    @Column(name = "dia_chi", length = 255)
    private String diaChi;

    /** Tên người liên hệ đầu mối tại nhà cung cấp. */
    @Column(name = "nguoi_lien_he", length = 100)
    private String nguoiLienHe;

    /**
     * Trạng thái hợp tác (DEFAULT 'dang_hop_tac').
     * Giá trị hợp lệ: "dang_hop_tac" | "ngung_hop_tac"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "dang_hop_tac";

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 NhaCungCap → nhiều PhieuNhapKho
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "nhaCungCap", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PhieuNhapKho> phieuNhapKhos = new ArrayList<>();
}
