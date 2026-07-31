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
public class ThongKeKenhDTO {
    private BigDecimal doanhThuOnline;
    private Double tyLeOnline;
    private Integer soDonOnline;

    private BigDecimal doanhThuOffline;
    private Double tyLeOffline;
    private Integer soDonOffline;
}
