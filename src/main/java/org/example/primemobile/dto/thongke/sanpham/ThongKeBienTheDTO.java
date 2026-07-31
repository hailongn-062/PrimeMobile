package org.example.primemobile.dto.thongke.sanpham;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ThongKeBienTheDTO {
    private Integer bienTheId;
    private String tenBienThe;
    private Integer soLuongBan;
    private Integer tonKho;
    private BigDecimal doanhThu;
}
