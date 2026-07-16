package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping báº£ng phuong_thuc_thanh_toan (Module 7: ÄÆ¡n hÃ ng).
 * <p>
 * Danh má»¥c cÃ¡c phÆ°Æ¡ng thá»©c thanh toÃ¡n Ä‘Æ°á»£c há»— trá»£.
 * <p>
 * PhÆ°Æ¡ng thá»©c hiá»‡n táº¡i:
 * - "Tien mat" : Thanh toÃ¡n tiá»n máº·t táº¡i quáº§y.
 * - "Chuyen khoan" : Chuyá»ƒn khoáº£n qua QR tÄ©nh táº¡i quáº§y, nhÃ¢n viÃªn xÃ¡c nháº­n.
 * - "Diem thuong" : Thanh toÃ¡n báº±ng Ä‘iá»ƒm tÃ­ch lÅ©y.
 * - "VNPay" : Thanh toÃ¡n trá»±c tuyáº¿n qua cá»•ng VNPay (redirect).
 * <p>
 * Quan há»‡:
 * - 1-N vá»›i {@link ThanhToan} (mappedBy phuongThucThanhToan)
 */
@Entity
@Table(name = "phuong_thuc_thanh_toan", uniqueConstraints = {
                @UniqueConstraint(name = "uq_pttt_ten", columnNames = "ten_pttt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "thanhToans")
@EqualsAndHashCode(exclude = "thanhToans")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class PhuongThucThanhToan {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * TÃªn phÆ°Æ¡ng thá»©c thanh toÃ¡n â€“ duy nháº¥t.
         * VÃ­ dá»¥: "Tien mat", "Chuyen khoan", "Diem thuong", "VNPay".
         */
        @Column(name = "ten_pttt", nullable = false, length = 50, unique = true)
        private String tenPttt;

        /** MÃ´ táº£ chi tiáº¿t vá» phÆ°Æ¡ng thá»©c thanh toÃ¡n. */
        @Column(name = "mo_ta", length = 255)
        private String moTa;

        /**
         * Tráº¡ng thÃ¡i kÃ­ch hoáº¡t (DEFAULT true).
         * false = táº¡m ngá»«ng há»— trá»£ phÆ°Æ¡ng thá»©c nÃ y.
         */
        @Column(name = "kich_hoat", nullable = false)
        @Builder.Default
        private Boolean kichHoat = true;

        // -------------------------------------------------------------------------
        // Quan há»‡ 1-N: 1 PhuongThucThanhToan â†’ nhiá»u ThanhToan
        // -------------------------------------------------------------------------
        @OneToMany(mappedBy = "phuongThucThanhToan", fetch = FetchType.LAZY)
        @Builder.Default
        private List<ThanhToan> thanhToans = new ArrayList<>();
}
