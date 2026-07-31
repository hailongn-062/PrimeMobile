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
public class ThongKeKpiDTO {
    private BigDecimal tongDoanhThu;
    private BigDecimal tongDoanhThuKyTruoc;
    private Double ptChangeDoanhThu;

    private Integer soDonHang;
    private Integer soDonHangKyTruoc;
    private Double ptChangeSoDonHang;

    private BigDecimal aov; // Average Order Value
    private BigDecimal aovKyTruoc;
    private Double ptChangeAov;

    private Double tyLeHuy; // Phần trăm
    private Double tyLeHuyKyTruoc;
    private Double ptChangeTyLeHuy;
}
