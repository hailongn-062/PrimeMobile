package org.example.primemobile.dto.pos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO trả về kết quả tính khuyến mãi tự động cho đơn POS.
 *
 * <p>Nếu không có CTKM phù hợp:
 * <pre>{@code { "ctkmId": null, "tenCtkm": null, "tienGiam": 0 } }</pre>
 *
 * <p>Nếu có CTKM:
 * <pre>{@code { "ctkmId": 3, "tenCtkm": "Giảm 5%", "tienGiam": 750000.00 } }</pre>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMaiPosDto {

    /** ID chương trình khuyến mãi được áp dụng (null nếu không có). */
    private Integer ctkmId;

    /** Tên chương trình khuyến mãi (null nếu không có). */
    private String tenCtkm;

    /** Giá trị ưu đãi (phần trăm). */
    private BigDecimal giaTriUuDai;

    /**
     * Số tiền thực tế được giảm (= tongTien × giaTriUuDai / 100).
     * Luôn >= 0, bằng 0 khi không có CTKM phù hợp.
     */
    private BigDecimal tienGiam;
}
