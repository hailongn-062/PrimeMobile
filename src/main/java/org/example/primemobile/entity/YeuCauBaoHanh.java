package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping bảng yeu_cau_bao_hanh (Module 8: Bảo hành).
 * <p>
 * Gộp thông tin yêu cầu bảo hành và thông tin gửi NCC vào cùng 1 bảng.
 * Luồng xử lý hoàn toàn 0 đồng (system_rules.md §5):
 *  Nhận máy từ khách → [Sửa tại cửa hàng / Gửi NCC] → Nhận lại → Trả khách.
 * <p>
 * Hình thức xử lý (CHECK chk_ycbh_hinh_thuc):
 *  "sua_chua" | "doi_moi" | "hoan_tien"
 * <p>
 * Trạng thái yêu cầu (CHECK chk_ycbh_trang_thai):
 *  "tiep_nhan" | "dang_kiem_tra" | "da_gui_ncc" | "ncc_dang_xu_ly"
 *  | "da_nhan_lai_ncc" | "cho_tra_khach" | "da_tra_khach" | "tu_choi"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link PhieuBaoHanh}  (FK phieu_bao_hanh_id)
 *  - N:1 với {@link NguoiDung}     (FK nguoi_tiep_nhan_id, ON DELETE SET NULL, nullable)
 *  - N:1 với {@link NhaCungCap}    (FK nha_cung_cap_id, nullable — khi gửi NCC)
 */
@Entity
@Table(
        name = "yeu_cau_bao_hanh",
        indexes = {
                @Index(name = "idx_ycbh_pbh", columnList = "phieu_bao_hanh_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ycbh_ma", columnNames = "ma_yeu_cau")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YeuCauBaoHanh {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã yêu cầu bảo hành – duy nhất (ví dụ: "YCBH-2024-001"). */
    @Column(name = "ma_yeu_cau", nullable = false, length = 50)
    private String maYeuCau;

    /**
     * Phiếu bảo hành liên kết với yêu cầu này.
     * NOT NULL – yêu cầu bảo hành phải có phiếu bảo hành hợp lệ.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "phieu_bao_hanh_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_ycbh_pbh")
    )
    private PhieuBaoHanh phieuBaoHanh;

    /**
     * Nhân viên tiếp nhận yêu cầu bảo hành.
     * NULL nếu chưa có nhân viên nhận.
     * ON DELETE SET NULL – xóa nhân viên không ảnh hưởng lịch sử.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nguoi_tiep_nhan_id",
            foreignKey = @ForeignKey(name = "fk_ycbh_nd")
    )
    private NguoiDung nguoiTiepNhan;

    /** Ngày giờ tiếp nhận máy từ khách (DEFAULT GETDATE()). */
    @Column(name = "ngay_tiep_nhan", nullable = false)
    @Builder.Default
    private LocalDateTime ngayTiepNhan = LocalDateTime.now();

    /** Mô tả lỗi / hiện tượng khách mô tả khi mang máy đến. */
    @Column(name = "mo_ta_loi", columnDefinition = "NVARCHAR(MAX)")
    private String moTaLoi;

    /**
     * Hình thức xử lý bảo hành.
     * Giá trị hợp lệ: "sua_chua" | "doi_moi" | "hoan_tien"
     */
    @Column(name = "hinh_thuc", nullable = false, length = 15)
    private String hinhThuc;

    /**
     * Trạng thái xử lý (DEFAULT 'tiep_nhan').
     * Giá trị hợp lệ: "tiep_nhan" | "dang_kiem_tra" | "da_gui_ncc"
     *  | "ncc_dang_xu_ly" | "da_nhan_lai_ncc" | "cho_tra_khach"
     *  | "da_tra_khach" | "tu_choi"
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "tiep_nhan";

    // -------------------------------------------------------------------------
    // THÔNG TIN GỬI NCC (Gộp từ phieu_gui_ncc_bao_hanh — system_rules.md §8)
    // -------------------------------------------------------------------------

    /**
     * Nhà cung cấp / hãng nhận máy để sửa.
     * NULL nếu cửa hàng tự sửa, không gửi NCC.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nha_cung_cap_id",
            foreignKey = @ForeignKey(name = "fk_ycbh_ncc")
    )
    private NhaCungCap nhaCungCap;

    /** Ngày gửi máy sang NCC / hãng. NULL nếu chưa gửi. */
    @Column(name = "ngay_gui_ncc")
    private LocalDateTime ngayGuiNcc;

    /** Ngày dự kiến nhận lại từ NCC. */
    @Column(name = "ngay_du_kien_nhan")
    private LocalDate ngayDuKienNhan;

    /** Ngày thực tế nhận lại máy từ NCC. NULL nếu chưa nhận. */
    @Column(name = "ngay_nhan_lai_ncc")
    private LocalDateTime ngayNhanLaiNcc;

    /** Kết quả xử lý từ NCC (báo cáo kỹ thuật, mô tả thay thế...). */
    @Column(name = "ket_qua_ncc", columnDefinition = "NVARCHAR(MAX)")
    private String ketQuaNcc;

    // -------------------------------------------------------------------------
    // TRẢ KHÁCH
    // -------------------------------------------------------------------------

    /** Ngày giờ trả máy lại cho khách. NULL nếu chưa trả. */
    @Column(name = "ngay_tra_khach")
    private LocalDateTime ngayTraKhach;

    /** Ghi chú nội bộ về quá trình xử lý. */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;
}
