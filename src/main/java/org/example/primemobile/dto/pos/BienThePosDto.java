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
     * Giá bán gốc (không áp dụng khuyến mãi).
     * Dùng để hiển thị giá gốc (có gạch ngang) khi có khuyến mãi.
     */
    private BigDecimal giaGoc;

    /**
     * Giá bán hiển thị: Giá này sẽ được tính động dựa trên chương trình khuyến mãi
     * hiện tại (sau khi áp dụng flash sale, giảm trực tiếp, v.v.).
     */
    private BigDecimal giaBan;

    /**
     * Số lượng còn lại tại Kho Tổng (đã trừ Safety Stock nếu cần).
     * Frontend chỉ hiển thị sản phẩm có tonKho > 0.
     */
    private Integer tonKho;

    /** URL ảnh đại diện đầu tiên (null nếu chưa có ảnh). */
    private String anhDaiDien;
}