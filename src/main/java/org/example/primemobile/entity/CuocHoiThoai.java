package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng cuoc_hoi_thoai (Module 11: Chatbot AI).
 * <p>
 * Mỗi phiên hội thoại giữa khách hàng và Chatbot AI (Gemini).
 * Hỗ trợ cả khách vãng lai (sessionId) và khách đã đăng nhập (khachHang).
 * <p>
 * Ghi chú (system_rules.md §7 – Tạm hoãn):
 *  Tích hợp Gemini API tạm thời BỊ HOÃN.
 *  Entity được tạo đầy đủ để mapping DB và phục vụ refactor {@link DonHang}.
 * <p>
 *  - 1-N với {@link TinNhanChat}  (mappedBy cuocHoiThoai, CASCADE ALL)
 *  - ID tham chiếu lỏng tới KhachHang (khach_hang_id) và DonHang (thông qua cột trong bảng don_hang)
 */
@Entity
@Table(
        name = "cuoc_hoi_thoai",
        indexes = {
                @Index(name = "idx_cht_kh",      columnList = "khach_hang_id, updated_at"),
                @Index(name = "idx_cht_session",  columnList = "session_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"tinNhanChats"})
@EqualsAndHashCode(exclude = {"tinNhanChats"})
public class CuocHoiThoai {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * ID Khách hàng đã đăng nhập thực hiện hội thoại.
     * Tham chiếu lỏng, NULL nếu là khách vãng lai.
     */
    @Column(name = "khach_hang_id")
    private Integer khachHangId;

    /**
     * Session token định danh khách vãng lai.
     * NULL nếu là khách đã đăng nhập.
     */
    @Column(name = "session_id", length = 100)
    private String sessionId;

    /**
     * Tiêu đề ngắn của hội thoại (tự động sinh bởi AI hoặc dùng tin nhắn đầu tiên).
     * NULL khi mới tạo.
     */
    @Column(name = "tieu_de", length = 255)
    private String tieuDe;

    /** Thời điểm tạo cuộc hội thoại. */
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayTao = LocalDateTime.now();

    /** Thời điểm cập nhật gần nhất (dùng để sắp xếp hội thoại gần đây). */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 CuocHoiThoai → nhiều TinNhanChat (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "cuocHoiThoai", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<TinNhanChat> tinNhanChats = new ArrayList<>();
}
