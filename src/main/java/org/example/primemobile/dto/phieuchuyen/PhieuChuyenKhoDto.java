package org.example.primemobile.dto.phieuchuyen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho phiếu chuyển kho.
 * <p>
 * Mục đích: Tránh lỗi serialize Hibernate proxy (ByteBuddyInterceptor) khi Jackson
 * chuyển đổi entity {@link org.example.primemobile.entity.PhieuChuyenKho} sang JSON.
 * </p>
 * <p>
 * Được sử dụng trong các API:
 * <ul>
 *   <li>{@code GET /api/admin/phieu-chuyen} - Danh sách phiếu chuyển (phân trang)</li>
 *   <li>{@code GET /api/admin/phieu-chuyen/{id}} - Chi tiết phiếu chuyển</li>
 * </ul>
 * </p>
 * <p>
 * Các trường được chọn lọc để hiển thị trên front-end mà không cần load toàn bộ
 * entity và các quan hệ lazy.
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuChuyenKhoDto {

    /**
     * ID của phiếu chuyển kho (phieu_chuyen_kho.id).
     */
    private Integer id;

    /**
     * Mã phiếu chuyển (ma_phieu).
     */
    private String maPhieu;

    /**
     * Tên kho nguồn (kho_nguon.tenKho).
     */
    private String tenKhoNguon;

    /**
     * Tên kho đích (kho_dich.tenKho).
     */
    private String tenKhoDich;

    /**
     * Lý do chuyển kho (ly_do).
     */
    private String lyDo;

    /**
     * Ngày giờ chuyển kho (ngay_chuyen).
     */
    private LocalDateTime ngayChuyen;

    /**
     * Danh sách chi tiết các sản phẩm được chuyển trong phiếu này.
     */
    private List<ChiTietChuyenKhoDto> chiTiets;
}