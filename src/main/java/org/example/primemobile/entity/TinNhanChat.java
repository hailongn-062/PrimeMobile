package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entity mapping bảng tin_nhan_chat (Module 11: Chatbot AI).
 * <p>
 * Mỗi bản ghi = 1 tin nhắn trong cuộc hội thoại.
 * <p>
 * Vai trò người gửi (CHECK chk_tnc_vai — chuẩn Gemini API):
 *  "user"   : Tin nhắn từ khách hàng.
 *  "model"  : Phản hồi từ Gemini AI (đã đổi từ 'assistant' theo chuẩn Gemini).
 *  "system" : Tin nhắn hệ thống nội bộ (ngữ cảnh, thông báo...).
 * <p>
 * Intent phân loại (CHECK chk_tnc_intent, nullable):
 *  "tu_van_sp" | "tra_cuu_dh" | "bao_hanh" | "khuyen_mai" | "chinh_sach" | "khac"
 * <p>
 * Quan hệ:
 *  - N:1 với {@link CuocHoiThoai} (FK cuoc_hoi_thoai_id, ON DELETE CASCADE)
 *  - donHangIdRef (Integer)        — FK ON DELETE SET NULL tới don_hang (tham chiếu ngữ cảnh)
 *  - sanPhamIdRef (Integer)        — FK ON DELETE SET NULL tới san_pham (tham chiếu ngữ cảnh)
 */
@Entity
@Table(
        name = "tin_nhan_chat",
        indexes = {
                @Index(name = "idx_tnc_cht", columnList = "cuoc_hoi_thoai_id, thoi_gian")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TinNhanChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Cuộc hội thoại chứa tin nhắn này.
     * ON DELETE CASCADE – xóa hội thoại thì tin nhắn tự xóa theo.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "cuoc_hoi_thoai_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_tnc_cht")
    )
    private CuocHoiThoai cuocHoiThoai;

    /**
     * Vai trò người gửi (chuẩn Gemini API).
     * Giá trị hợp lệ: "user" | "model" | "system"
     */
    @Column(name = "vai", nullable = false, length = 10)
    private String vai;

    /** Nội dung tin nhắn (có thể là text, hoặc mô tả ảnh nếu có hinhAnhUrl). */
    @Column(name = "noi_dung", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String noiDung;

    /**
     * URL ảnh đính kèm (hỗ trợ Gemini Vision API).
     * NULL nếu tin nhắn chỉ có text.
     */
    @Column(name = "hinh_anh_url", length = 255)
    private String hinhAnhUrl;

    /**
     * Số token tiêu thụ của tin nhắn này (dùng để quản lý quota API Free).
     * NULL nếu là tin nhắn của "user".
     */
    @Column(name = "so_token")
    private Integer soToken;

    /**
     * Phân loại ý định của tin nhắn (nullable).
     * Giá trị hợp lệ: "tu_van_sp" | "tra_cuu_dh" | "bao_hanh"
     *               | "khuyen_mai" | "chinh_sach" | "khac" | NULL
     */
    @Column(name = "intent", length = 30)
    private String intent;

    /**
     * ID đơn hàng được nhắc đến trong tin nhắn (ngữ cảnh hội thoại).
     * FK fk_tnc_dh với ON DELETE SET NULL — giữ nguyên khi đơn bị xóa.
     * Tạm giữ Integer — FK tham chiếu ngữ cảnh không cần load full DonHang.
     */
    @Column(name = "don_hang_id_ref")
    private Integer donHangIdRef;

    /**
     * ID sản phẩm được nhắc đến trong tin nhắn (ngữ cảnh hội thoại).
     * FK fk_tnc_sp với ON DELETE SET NULL.
     * Tạm giữ Integer — FK tham chiếu ngữ cảnh không cần load full SanPham.
     */
    @Column(name = "san_pham_id_ref")
    private Integer sanPhamIdRef;

    /** Thời điểm gửi tin nhắn. */
    @Column(name = "thoi_gian", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime thoiGian = LocalDateTime.now();
}
