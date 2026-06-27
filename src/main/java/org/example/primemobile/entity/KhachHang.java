package org.example.primemobile.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity mapping bảng khach_hang (Module 1: Người dùng & Phân quyền).
 * <p>
 * Hỗ trợ 2 loại khách hàng:
 * - Khách vãng lai (guest): nguoiDung = NULL, không cần tài khoản.
 * - Khách có tài khoản: nguoiDung != NULL, quan hệ 1-1 với bảng nguoi_dung.
 * <p>
 * Hệ thống đã loại bỏ chức năng tích điểm và hạng thành viên.
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
public class KhachHang {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        /**
         * Liên kết 1-1 với tài khoản nguoi_dung.
         * NULL = khách vãng lai (chưa có tài khoản).
         * ON DELETE SET NULL — khi xóa nguoi_dung, khach_hang vẫn được giữ lại.
         */
        @OneToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "nguoi_dung_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_kh_nd"), unique = true)
        @JsonIgnore
        private NguoiDung nguoiDung;

        /** Họ và tên người dùng (bắt buộc). */
        @Column(name = "ho_ten", nullable = false, length = 100)
        private String hoTen;

        /**
         * Email liên lạc của khách hàng.
         * Có thể NULL với khách vãng lai.
         */
        @Column(name = "email", length = 100)
        private String email;

        /** Số điện thoại liên lạc (bắt buộc). */
        @Column(name = "so_dien_thoai", nullable = false, length = 20)
        private String soDienThoai;

        /**
         * Giới tính.
         * Giá trị hợp lệ: "Nam" | "Nu" | "Khac" | NULL.
         */
        @Column(name = "gioi_tinh", length = 5)
        private String gioiTinh;

        /** Ngày sinh (chỉ lưu phần ngày, không lưu giờ). */
        @Column(name = "ngay_sinh")
        private LocalDate ngaySinh;

        /** Thời điểm tạo bản ghi. Mặc định = GETDATE() ở DB. */
        @Column(name = "ngay_tao", nullable = false, updatable = false)
        @Builder.Default
        private LocalDateTime ngayTao = LocalDateTime.now();

        /** Thời điểm cập nhật bản ghi gần nhất. */
        @Column(name = "updated_at", nullable = false)
        @Builder.Default
        private LocalDateTime updatedAt = LocalDateTime.now();
}