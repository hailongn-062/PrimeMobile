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
 * ⚠️ Filtered Unique Index trong SQL Server (không thể map qua JPA):
 *  {@code CREATE UNIQUE INDEX uq_tt_vnp_txn_ref ON thanh_toan (vnp_txn_ref) WHERE vnp_txn_ref IS NOT NULL}
 *  → Service Layer phải kiểm tra trùng vnpTxnRef trước khi xử lý IPN VNPay.
 * <p>
 * Trạng thái hợp lệ (CHECK chk_tt_trang_thai):
 *  "cho" | "thanh_cong" | "that_bai"
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
     * Ví dụ: COD, Chuyển khoản, VNPay.
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
     * Số tiền khách THỰC TẾ đã thanh toán (nhận từ VNPay IPN).
     * NULL với COD, có giá trị sau khi VNPay xác nhận giao dịch.
     */
    @Column(name = "so_tien_thuc_te", precision = 15, scale = 2)
    private BigDecimal soTienThucTe;

    /** Mã giao dịch nội bộ hoặc từ cổng thanh toán. */
    @Column(name = "ma_giao_dich", length = 100)
    private String maGiaoDich;

    /**
     * Trạng thái giao dịch (DEFAULT 'cho').
     * Giá trị hợp lệ: "cho" | "thanh_cong" | "that_bai"
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
    // THÔNG TIN VNPAY
    // -------------------------------------------------------------------------

    /** Mã tham chiếu giao dịch do hệ thống tạo, gửi sang VNPay. UNIQUE khi NOT NULL. */
    @Column(name = "vnp_txn_ref", length = 100)
    private String vnpTxnRef;

    /** Mã giao dịch do VNPay cấp sau khi thanh toán thành công. */
    @Column(name = "vnp_transaction_no", length = 100)
    private String vnpTransactionNo;

    /** Mã phản hồi từ VNPay (00 = thành công). */
    @Column(name = "vnp_response_code", length = 10)
    private String vnpResponseCode;

    /** Mã ngân hàng khách dùng để thanh toán qua VNPay. */
    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    /** Mã giao dịch tại ngân hàng. */
    @Column(name = "vnp_bank_tran_no", length = 100)
    private String vnpBankTranNo;

    /** Loại thẻ/tài khoản khách dùng (ví dụ: ATM, QRCODE). */
    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;

    /** Thời điểm thanh toán theo VNPay (định dạng yyyyMMddHHmmss). */
    @Column(name = "vnp_pay_date", length = 20)
    private String vnpPayDate;

    /** Chữ ký bảo mật VNPay gửi về trong IPN để xác thực tính toàn vẹn. */
    @Column(name = "vnp_secure_hash", length = 256)
    private String vnpSecureHash;

    /**
     * JSON gốc VNPay gửi về qua IPN (dùng để debug khi cần).
     * NULL với COD / Chuyển khoản.
     */
    @Column(name = "raw_ipn", columnDefinition = "NVARCHAR(MAX)")
    private String rawIpn;
}
