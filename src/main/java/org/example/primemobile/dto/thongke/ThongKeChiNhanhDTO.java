package org.example.primemobile.dto.thongke;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeChiNhanhDTO {
    private String maChiNhanh;
    private String tenChiNhanh;
    private BigDecimal doanhThu;
    private Integer soDonHang;
    private BigDecimal aov;
    private Double ptChangeDoanhThu;
}
