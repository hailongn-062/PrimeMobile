package org.example.primemobile.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng khach_hang (Module 1: NgÆ°á»i dÃ¹ng & PhÃ¢n quyá»n).
 * <p>
 * Há»— trá»£ 2 loáº¡i khÃ¡ch hÃ ng:
 * - KhÃ¡ch vÃ£ng lai (guest): nguoiDung = NULL, khÃ´ng cáº§n tÃ i khoáº£n.
 * - KhÃ¡ch cÃ³ tÃ i khoáº£n: nguoiDung != NULL, quan há»‡ 1-1 vá»›i báº£ng nguoi_dung.
 * <p>
 * Há»‡ thá»‘ng Ä‘Ã£ loáº¡i bá» chá»©c nÄƒng tÃ­ch Ä‘iá»ƒm vÃ  háº¡ng thÃ nh viÃªn.
 */
@Entity
@Table(name = "khach_hang", indexes = {
                @Index(name = "idx_kh_sdt", columnList = "so_dien_thoai"),
                @Index(name = "idx_kh_email", columnList = "email")
}, uniqueConstraints = {
                @UniqueConstraint(name = "uq_kh_nguoi_dung", columnNames = "nguoi_dung_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KhachHang {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * LiÃªn káº¿t 1-1 vá»›i tÃ i khoáº£n nguoi_dung.
         * NULL = khÃ¡ch vÃ£ng lai (chÆ°a cÃ³ tÃ i khoáº£n).
         * ON DELETE SET NULL â€” khi xÃ³a nguoi_dung, khach_hang váº«n Ä‘Æ°á»£c giá»¯ láº¡i.
         */
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "nguoi_dung_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_kh_nd"), unique = true)
        @JsonIgnore
        private NguoiDung nguoiDung;

        /** Há» vÃ  tÃªn ngÆ°á»i dÃ¹ng (báº¯t buá»™c). */
        @Column(name = "ho_ten", nullable = false, length = 100)
        private String hoTen;

        /**
         * Email liÃªn láº¡c cá»§a khÃ¡ch hÃ ng.
         * CÃ³ thá»ƒ NULL vá»›i khÃ¡ch vÃ£ng lai.
         */
        @Column(name = "email", length = 100)
        private String email;

        /** Sá»‘ Ä‘iá»‡n thoáº¡i liÃªn láº¡c (báº¯t buá»™c). */
        @Column(name = "so_dien_thoai", nullable = false, length = 20)
        private String soDienThoai;

        /**
         * Giá»›i tÃ­nh.
         * GiÃ¡ trá»‹ há»£p lá»‡: "Nam" | "Nu" | "Khac" | NULL.
         */
        @Column(name = "gioi_tinh", length = 5)
        private String gioiTinh;

        /** NgÃ y sinh (chá»‰ lÆ°u pháº§n ngÃ y, khÃ´ng lÆ°u giá»). */
        @Column(name = "ngay_sinh")
        private LocalDate ngaySinh;

        /** Thá»i Ä‘iá»ƒm táº¡o báº£n ghi. Máº·c Ä‘á»‹nh = GETDATE() á»Ÿ DB. */
        @Column(name = "ngay_tao", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime ngayTao = LocalDateTime.now();

        /** Thá»i Ä‘iá»ƒm cáº­p nháº­t báº£n ghi gáº§n nháº¥t. */
        @Column(name = "updated_at", nullable = false)
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();
}
