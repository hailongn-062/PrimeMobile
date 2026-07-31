package org.example.primemobile.dto.thongke.sanpham;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ThongKeDanhMucDTO {
    private String tenDanhMuc;
    private Integer soLuongBan;
    private BigDecimal doanhThu;
    private Double phanTram;
}
