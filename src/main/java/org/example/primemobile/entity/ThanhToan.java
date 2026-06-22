package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity mapping bảng thanh_toan (Module 7: Đơn hàng).
 * <p>
 * Lịch sử các giao dịch thanh toán của một đơn hàng.
 * 1 đơn hàng có thể có nhiều bản ghi ThanhToan (ví dụ: thanh toán thất bại → thử lại).
 * <p>
 * Ghi chú (system_rules.md §7 – Tính năng hoãn lại):
 *  Phương thức COD đang là DUY NHẤT được sử dụng.
 *  Các trường QR động (qrCodeUrl, noiDungChuyenKhoan...) và SePay webhook
 *  (sepayTransactionId, rawWebhook...) được map đầy đủ để không phải refactor
 *  khi bỏ tạm hoãn, nhưng sẽ để NULL trong giai đoạn hiện tại.
 * <p>
 * ⚠️ Filtered Unique Index trong SQL Server (không thể map qua JPA):
 *  {@code CREATE UNIQUE INDEX uq_tt_sepay_id ON thanh_toan (sepay_transaction_id) WHERE sepay_transaction_id IS NOT NULL}
 *  → Service Layer phải kiểm tra trùng sepayTransactionId trước khi xử lý webhook SePay.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_tt_trang_thai):
 *  "cho" | "thanh_cong" | "that_bai" | "hoan_tien"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link DonHang}              (FK don_hang_id)
 *  - N:1 với {@link PhuongThucThanhToan}  (FK phuong_thuc_thanh_toan_id)
 */
@Entity
@Table(
        name = "thanh_toan",
        indexes = {
                @Index(name = "idx_tt_dh", columnList = "don_hang_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ThanhToan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Đơn hàng được thanh toán.
     * NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "don_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tt_dh")
    )
    private DonHang donHang;

    /**
     * Phương thức thanh toán được sử dụng.
     * Hiện tại mặc định là COD (system_rules.md §7).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "phuong_thuc_thanh_toan_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tt_pttt")
    )
    private PhuongThucThanhToan phuongThucThanhToan;

    /** Số tiền cần thanh toán (CHECK >= 0). */
    @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
    private BigDecimal soTien;

    /**
     * Số tiền khách THỰC TẾ đã chuyển (nhận từ SePay webhook).
     * NULL với COD, có giá trị sau khi SePay xác nhận chuyển khoản.
     */
    @Column(name = "so_tien_thuc_te", precision = 15, scale = 2)
    private BigDecimal soTienThucTe;

    /** Mã giao dịch nội bộ hoặc từ cổng thanh toán. */
    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    /**
     * Trạng thái giao dịch (DEFAULT 'cho').
     * Giá trị hợp lệ: "cho" | "thanh_cong" | "that_bai" | "hoan_tien"
     */
    @Column(name = "trang_thai", nullable = false, length = 15)
    @Builder.Default
    private String trangThai = "cho";

    /** Thời điểm tạo bản ghi thanh toán. */
    @Column(name = "thoi_gian_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime thoiGianTao = LocalDateTime.now();

    /** Thời điểm thanh toán thành công (NULL nếu chưa thành công). */
    @Column(name = "thoi_gian_thanh_cong")
    private LocalDateTime thoiGianThanhCong;

    // -------------------------------------------------------------------------
    // THÔNG TIN QR ĐỘNG (VietQR) — Tạm hoãn theo system_rules.md §7
    // -------------------------------------------------------------------------

    /** URL ảnh mã QR sinh bởi VietQR API. NULL với COD. */
    @Column(name = "qr_code_url", length = 500)
    private String qrCodeUrl;

    /**
     * Nội dung in sẵn trong QR chuyển khoản (ví dụ: "THANHTOAN DH2024001").
     * Dùng để đối chiếu tự động qua SePay webhook.
     */
    @Column(name = "noi_dung_chuyen_khoan", length = 100)
    private String noiDungChuyenKhoan;

    /**
     * Thời điểm mã QR hết hạn (thường sau 15 phút).
     * NULL với COD.
     */
    @Column(name = "thoi_gian_het_han")
    private LocalDateTime thoiGianHetHan;

    // -------------------------------------------------------------------------
    // DỮ LIỆU SEPAY WEBHOOK — Tạm hoãn theo system_rules.md §7
    // -------------------------------------------------------------------------

    /**
     * ID giao dịch trên hệ thống SePay.
     * ⚠️ Filtered Unique Index ở DB: UNIQUE khi NOT NULL.
     * Service Layer phải kiểm tra trùng trước khi xử lý webhook.
     */
    @Column(name = "sepay_transaction_id")
    private Long sepayTransactionId;

    /** Tên ngân hàng khách dùng để quét QR. */
    @Column(name = "ten_ngan_hang_gui", length = 50)
    private String tenNganHangGui;

    /** Nội dung chuyển khoản thực tế khách nhập (có thể khác noiDungChuyenKhoan). */
    @Column(name = "noi_dung_goc", length = 255)
    private String noiDungGoc;

    /** referenceCode từ SePay để tra cứu giao dịch. */
    @Column(name = "ma_tham_chieu", length = 100)
    private String maThamChieu;

    /** Thời điểm ngân hàng xử lý giao dịch. */
    @Column(name = "thoi_gian_ngan_hang")
    private LocalDateTime thoiGianNganHang;

    /**
     * JSON gốc SePay gửi về qua webhook (dùng để debug khi cần).
     * NULL với COD.
     */
    @Column(name = "raw_webhook", columnDefinition = "NVARCHAR(MAX)")
    private String rawWebhook;
}
