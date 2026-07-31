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
public class ThongKeChiTietNgayDTO {
    private String ngay;
    private Integer soDonHang;
    private BigDecimal tongDoanhThu;
    private BigDecimal doanhThuOnline;
    private BigDecimal doanhThuOffline;
    private Integer soDonHuy;
}
