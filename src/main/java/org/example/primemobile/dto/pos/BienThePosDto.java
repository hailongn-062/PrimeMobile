package org.example.primemobile.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO trả về danh sách sản phẩm cho màn hình POS.
 * Gộp thông tin biến thể + tồn kho Kho Tổng trong 1 object phẳng
 * để tránh N+1 query và serialize gọn cho Frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BienThePosDto {

    /** ID biến thể sản phẩm. */
    private Integer bienTheId;

    /** Tên sản phẩm cha (ví dụ: "iPhone 15 Pro Max"). */
    private String tenSanPham;

    /** Mã SKU duy nhất (ví dụ: "IP15PM-TIT-8-256"). */
    private String maSku;

    /** Màu sắc (ví dụ: "Titan Đen"). */
    private String mauSac;

    /** RAM GB. */
    private Integer ramGb;

    /** Lưu trữ GB. */
    private Integer luuTruGb;

    /**
     * Giá bán hiển thị: Ưu tiên {@code giaKhuyenMai} nếu đang áp dụng,
     * fallback về {@code giaBan}.
     */
    private BigDecimal giaBan;

    /** Giá khuyến mãi (null nếu không có). */
    private BigDecimal giaKhuyenMai;

    /**
     * Số lượng còn lại tại Kho Tổng (đã trừ Safety Stock nếu cần).
     * Frontend chỉ hiển thị sản phẩm có tonKho > 0.
     */
    private Integer tonKho;

    /** URL ảnh đại diện đầu tiên (null nếu chưa có ảnh). */
    private String anhDaiDien;
}
