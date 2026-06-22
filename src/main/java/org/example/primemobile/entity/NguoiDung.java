package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng nguoi_dung (Module 1: Người dùng & Phân quyền).
 * <p>
 * Vai trò hợp lệ (CHECK constraint chk_nd_vai_tro): Admin | NhanVien | KhachHang
 * Trạng thái hợp lệ (CHECK constraint chk_nd_trang_thai): hoat_dong | khoa
 * <p>
 * Ghi chú system_rules.md:
 *  - Không dùng Spring Security / JWT. Phiên làm việc quản lý bằng HttpSession + HandlerInterceptor.
 *  - Đăng nhập bằng email HOẶC so_dien_thoai kết hợp mat_khau (đã băm).
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
public class NguoiDung {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Email đăng nhập – duy nhất trong hệ thống.
     * Có thể dùng thay thế so_dien_thoai khi xác thực.
     */
    @Column(name = "email", nullable = false, length = 100, unique = true)
    private String email;

    /**
     * Mật khẩu đã được băm (BCrypt hoặc tương đương).
     * Không bao giờ lưu plain-text.
     */
    @Column(name = "mat_khau", nullable = false, length = 255)
    private String matKhau;

    /** Họ và tên đầy đủ của người dùng. */
    @Column(name = "ho_ten", nullable = false, length = 100)
    private String hoTen;

    /**
     * Số điện thoại – có thể dùng thay email khi đăng nhập.
     * Nullable theo thiết kế DB.
     */
    @Column(name = "so_dien_thoai", length = 20)
    private String soDienThoai;

    /**
     * Vai trò phân quyền.
     * Giá trị hợp lệ: "Admin" | "NhanVien" | "KhachHang" (DEFAULT 'KhachHang').
     * Hardcode theo system_rules.md — không dùng bảng phân quyền riêng.
     */
    @Column(name = "vai_tro", nullable = false, length = 15)
    @Builder.Default
    private String vaiTro = "KhachHang";

    /**
     * Trạng thái tài khoản.
     * Giá trị hợp lệ: "hoat_dong" | "khoa" (DEFAULT 'hoat_dong').
     */
    @Column(name = "trang_thai", nullable = false, length = 10)
    @Builder.Default
    private String trangThai = "hoat_dong";

    /** Thời điểm đăng nhập lần cuối — NULL nếu chưa đăng nhập lần nào. */
    @Column(name = "lan_dang_nhap_cuoi")
    private LocalDateTime lanDangNhapCuoi;

    /** Thời điểm tạo bản ghi. Mặc định = GETDATE() ở DB. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật bản ghi gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
