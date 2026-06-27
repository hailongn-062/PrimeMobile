package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO cho thông tin tồn kho của một biến thể sản phẩm tại một kho.
 * <p>
 * Mục đích: Tránh lỗi serialize khi Jackson gặp Hibernate proxy (ByteBuddyInterceptor)
 * do lazy loading. DTO chỉ chứa các trường đơn giản, không có proxy.
 * </p>
 * <p>
 * DTO này được sử dụng trong các API:
 * <ul>
 *   <li>{@code /api/admin/phieu-chuyen/ton-kho/{khoId}/{bienTheId}} - Xem tồn kho chi tiết 1 SKU tại 1 kho</li>
 *   <li>(Có thể mở rộng cho các màn hình tra cứu tồn kho khác)</li>
 * </ul>
 * </p>
 *
 * @see org.example.primemobile.entity.TonKho
 * @see org.example.primemobile.controller.PhieuChuyenKhoController#xemTonKhoChiTiet(Integer, Integer, SessionUser)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TonKhoDto {

    /**
     * ID của bản ghi tồn kho (ton_kho.id) – có thể dùng để debug hoặc cập nhật sau.
     */
    private Integer tonKhoId;

    /**
     * ID của kho (kho.id).
     */
    private Integer khoId;

    /**
     * Tên kho (kho.tenKho).
     */
    private String tenKho;

    /**
     * ID của biến thể sản phẩm (bien_the_san_pham.id).
     */
    private Integer bienTheSanPhamId;

    /**
     * Mã SKU của biến thể sản phẩm (bien_the_san_pham.maSku).
     */
    private String maSku;

    /**
     * Tên sản phẩm (san_pham.tenSanPham).
     * Có thể null nếu biến thể chưa được gán sản phẩm (không nên xảy ra).
     */
    private String tenSanPham;

    /**
     * Số lượng tồn kho hiện tại (ton_kho.soLuong).
     */
    private Integer soLuong;

    /**
     * Thời điểm cập nhật tồn kho gần nhất (ton_kho.updatedAt).
     * Có thể null nếu không cần hiển thị.
     */
    private String updatedAt;
}