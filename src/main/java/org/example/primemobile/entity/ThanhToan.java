package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng thanh_toan (Module 7: ÄÆ¡n hÃ ng).
 * <p>
 * Lá»‹ch sá»­ cÃ¡c giao dá»‹ch thanh toÃ¡n cá»§a má»™t Ä‘Æ¡n hÃ ng.
 * 1 Ä‘Æ¡n hÃ ng cÃ³ thá»ƒ cÃ³ nhiá»u báº£n ghi ThanhToan (vÃ­ dá»¥: thanh toÃ¡n tháº¥t báº¡i â†’
 * thá»­ láº¡i).
 * <p>
 * âš ï¸ Filtered Unique Index trong SQL Server (khÃ´ng thá»ƒ map qua JPA):
 * {@code CREATE UNIQUE INDEX uq_tt_vnp_txn_ref ON thanh_toan (vnp_txn_ref) WHERE vnp_txn_ref IS NOT NULL}
 * â†’ Service Layer pháº£i kiá»ƒm tra trÃ¹ng vnpTxnRef trÆ°á»›c khi xá»­ lÃ½ IPN VNPay.
 * <p>
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK chk_tt_trang_thai):
 * "cho" | "thanh_cong" | "that_bai"
 * <p>
 * Quan há»‡:
 * - N:1 vá»›i {@link DonHang} (FK don_hang_id)
 * - N:1 vá»›i {@link PhuongThucThanhToan} (FK phuong_thuc_thanh_toan_id)
 */
@Entity
@Table(name = "thanh_toan", indexes = {
                @Index(name = "idx_tt_dh", columnList = "don_hang_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ThanhToan {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * ÄÆ¡n hÃ ng Ä‘Æ°á»£c thanh toÃ¡n.
         * NOT NULL.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "don_hang_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tt_dh"))
        @com.fasterxml.jackson.annotation.JsonIgnore
        private DonHang donHang;

        /**
         * PhÆ°Æ¡ng thá»©c thanh toÃ¡n Ä‘Æ°á»£c sá»­ dá»¥ng.
         * VÃ­ dá»¥: COD, Chuyá»ƒn khoáº£n, VNPay.
         */
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "phuong_thuc_thanh_toan_id", nullable = false, foreignKey = @ForeignKey(name = "fk_tt_pttt"))
        private PhuongThucThanhToan phuongThucThanhToan;

        /** Sá»‘ tiá»n cáº§n thanh toÃ¡n (CHECK >= 0). */
        @Column(name = "so_tien", nullable = false, precision = 15, scale = 2)
        private BigDecimal soTien;

        /**
         * Sá»‘ tiá»n khÃ¡ch THá»°C Táº¾ Ä‘Ã£ thanh toÃ¡n (nháº­n tá»« VNPay IPN).
         * NULL vá»›i COD, cÃ³ giÃ¡ trá»‹ sau khi VNPay xÃ¡c nháº­n giao dá»‹ch.
         */
        @Column(name = "so_tien_thuc_te", precision = 15, scale = 2)
        private BigDecimal soTienThucTe;

        /** MÃ£ giao dá»‹ch ná»™i bá»™ hoáº·c tá»« cá»•ng thanh toÃ¡n. */
        @Column(name = "ma_giao_dich", length = 100)
        private String maGiaoDich;

        /**
         * Tráº¡ng thÃ¡i giao dá»‹ch (DEFAULT 'cho').
         * GiÃ¡ trá»‹ há»£p lá»‡: "cho" | "thanh_cong" | "that_bai" | "da_hoan_tien"
         */
        @Column(name = "trang_thai", nullable = false, length = 15)
        @Builder.Default
        private String trangThai = "cho";

        /** Thá»i Ä‘iá»ƒm táº¡o báº£n ghi thanh toÃ¡n. */
        @Column(name = "thoi_gian_tao", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime thoiGianTao = LocalDateTime.now();

        /** Thá»i Ä‘iá»ƒm thanh toÃ¡n thÃ nh cÃ´ng (NULL náº¿u chÆ°a thÃ nh cÃ´ng). */
        @Column(name = "thoi_gian_thanh_cong")
        private LocalDateTime thoiGianThanhCong;

        // -------------------------------------------------------------------------
        // THÃ”NG TIN VNPAY
        // -------------------------------------------------------------------------

        /**
         * MÃ£ tham chiáº¿u giao dá»‹ch do há»‡ thá»‘ng táº¡o, gá»­i sang VNPay. UNIQUE khi NOT NULL.
         */
        @Column(name = "vnp_txn_ref", length = 100)
        private String vnpTxnRef;

        /** MÃ£ giao dá»‹ch do VNPay cáº¥p sau khi thanh toÃ¡n thÃ nh cÃ´ng. */
        @Column(name = "vnp_transaction_no", length = 100)
        private String vnpTransactionNo;

        /** MÃ£ pháº£n há»“i tá»« VNPay (00 = thÃ nh cÃ´ng). */
        @Column(name = "vnp_response_code", length = 10)
        private String vnpResponseCode;

        /** MÃ£ ngÃ¢n hÃ ng khÃ¡ch dÃ¹ng Ä‘á»ƒ thanh toÃ¡n qua VNPay. */
        @Column(name = "vnp_bank_code", length = 20)
        private String vnpBankCode;

        /** MÃ£ giao dá»‹ch táº¡i ngÃ¢n hÃ ng. */
        @Column(name = "vnp_bank_tran_no", length = 100)
        private String vnpBankTranNo;

        /** Loáº¡i tháº»/tÃ i khoáº£n khÃ¡ch dÃ¹ng (vÃ­ dá»¥: ATM, QRCODE). */
        @Column(name = "vnp_card_type", length = 20)
        private String vnpCardType;

        /** Thá»i Ä‘iá»ƒm thanh toÃ¡n theo VNPay (Ä‘á»‹nh dáº¡ng yyyyMMddHHmmss). */
        @Column(name = "vnp_pay_date", length = 20)
        private String vnpPayDate;

        /** Chá»¯ kÃ½ báº£o máº­t VNPay gá»­i vá» trong IPN Ä‘á»ƒ xÃ¡c thá»±c tÃ­nh toÃ n váº¹n. */
        @Column(name = "vnp_secure_hash", length = 256)
        private String vnpSecureHash;

        /**
         * JSON gá»‘c VNPay gá»­i vá» qua IPN (dÃ¹ng Ä‘á»ƒ debug khi cáº§n).
         * NULL vá»›i COD / Chuyá»ƒn khoáº£n.
         */
        @Column(name = "raw_ipn", columnDefinition = "NVARCHAR(MAX)")
        private String rawIpn;
}
