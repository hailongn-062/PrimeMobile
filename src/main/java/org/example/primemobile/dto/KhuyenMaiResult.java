package org.example.primemobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO chứa kết quả tính khuyến mãi cho một đơn hàng.
 * Được sử dụng thống nhất cho cả Online và POS.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KhuyenMaiResult {

    /**
     * ID của chương trình khuyến mãi được áp dụng.
     * Có thể null nếu không có CTKM phù hợp.
     */
    private Integer ctkmId;

    /**
     * Tên chương trình khuyến mãi (để hiển thị).
     */
    private String tenCtkm;

    /**
     * Giá trị ưu đãi (phần trăm).
     */
    private BigDecimal giaTriUuDai;

    /**
     * Số tiền thực tế được giảm (đã làm tròn 2 chữ số thập phân).
     */
    private BigDecimal tienGiam;

    /**
     * Tổng tiền sau khi đã trừ khuyến mãi.
     */
    private BigDecimal tongSauGiam;
}