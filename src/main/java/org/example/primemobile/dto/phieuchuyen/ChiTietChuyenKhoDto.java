package org.example.primemobile.dto.phieuchuyen;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho chi tiết phiếu chuyển kho.
 * <p>
 * Mục đích: Tránh lỗi serialize Hibernate proxy (ByteBuddyInterceptor) khi Jackson
 * chuyển đổi entity {@link org.example.primemobile.entity.ChiTietChuyenKho} sang JSON.
 * </p>
 * <p>
 * Chỉ chứa các trường cần thiết để hiển thị trên front-end:
 * <ul>
 *   <li>Mã SKU</li>
 *   <li>Tên sản phẩm</li>
 *   <li>Số lượng chuyển</li>
 * </ul>
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietChuyenKhoDto {

    /**
     * ID của chi tiết phiếu chuyển (chi_tiet_chuyen_kho.id).
     * Có thể dùng để debug hoặc tham chiếu.
     */
    private Integer id;

    /**
     * Mã SKU của biến thể sản phẩm (bien_the_san_pham.maSku).
     */
    private String maSku;

    /**
     * Tên sản phẩm (san_pham.tenSanPham).
     * Có thể null nếu biến thể chưa được gán sản phẩm.
     */
    private String tenSanPham;

    /**
     * Số lượng sản phẩm được chuyển trong dòng này.
     */
    private Integer soLuong;
}