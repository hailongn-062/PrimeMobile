package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping báº£ng nguoi_dung (Module 1: NgÆ°á»i dÃ¹ng & PhÃ¢n quyá»n).
 * <p>
 * Vai trÃ² há»£p lá»‡ (CHECK constraint chk_nd_vai_tro): Admin | NhanVien | KhachHang
 * Tráº¡ng thÃ¡i há»£p lá»‡ (CHECK constraint chk_nd_trang_thai): hoat_dong | khoa
 * <p>
 * Ghi chÃº system_rules.md:
 *  - KhÃ´ng dÃ¹ng Spring Security / JWT. PhiÃªn lÃ m viá»‡c quáº£n lÃ½ báº±ng HttpSession + HandlerInterceptor.
 *  - ÄÄƒng nháº­p báº±ng email HOáº¶C so_dien_thoai káº¿t há»£p mat_khau (Ä‘Ã£ bÄƒm).
 */
@Entity
@Table(
        name = "nguoi_dung",
        indexes = {
                @Index(name = "idx_nd_email", columnList = "email")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_nd_email", columnNames = "email")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Email Ä‘Äƒng nháº­p â€“ duy nháº¥t trong há»‡ thá»‘ng.
     * CÃ³ thá»ƒ dÃ¹ng thay tháº¿ so_dien_thoai khi xÃ¡c thá»±c.
     */
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    /**
     * Máº­t kháº©u Ä‘Ã£ Ä‘Æ°á»£c bÄƒm (BCrypt hoáº·c tÆ°Æ¡ng Ä‘Æ°Æ¡ng).
     * KhÃ´ng bao giá» lÆ°u plain-text.
     */
    @Column(name = "mat_khau", nullable = false, length = 255)
    private String matKhau;

    /** Há» vÃ  tÃªn Ä‘áº§y Ä‘á»§ cá»§a ngÆ°á»i dÃ¹ng. */
    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    /**
     * Sá»‘ Ä‘iá»‡n thoáº¡i â€“ cÃ³ thá»ƒ dÃ¹ng thay email khi Ä‘Äƒng nháº­p.
     * Nullable theo thiáº¿t káº¿ DB.
     */
    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    /**
     * Vai trÃ² phÃ¢n quyá»n.
     * GiÃ¡ trá»‹ há»£p lá»‡: "Admin" | "NhanVien" | "KhachHang" (DEFAULT 'KhachHang').
     * Hardcode theo system_rules.md â€” khÃ´ng dÃ¹ng báº£ng phÃ¢n quyá»n riÃªng.
     */
    @Column(name = "vai_tro", nullable = false, length = 15)
    @Builder.Default
    private String vaiTro = "KhachHang";

    /**
     * Tráº¡ng thÃ¡i tÃ i khoáº£n.
     * GiÃ¡ trá»‹ há»£p lá»‡: "hoat_dong" | "khoa" (DEFAULT 'hoat_dong').
     */
    @Column(name = "trang_thai", nullable = false, length = 10)
    @Builder.Default
    private String trangThai = "hoat_dong";

    /** Thá»i Ä‘iá»ƒm Ä‘Äƒng nháº­p láº§n cuá»‘i â€” NULL náº¿u chÆ°a Ä‘Äƒng nháº­p láº§n nÃ o. */
    @Column(name = "lan_dang_nhap_cuoi")
    private LocalDateTime lanDangNhapCuoi;

    /** Thá»i Ä‘iá»ƒm táº¡o báº£n ghi. Máº·c Ä‘á»‹nh = GETDATE() á»Ÿ DB. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thá»i Ä‘iá»ƒm cáº­p nháº­t báº£n ghi gáº§n nháº¥t. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
