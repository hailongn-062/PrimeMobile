package org.example.primemobile.dto.thongke.sanpham;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ThongKeTopSanPhamDTO {
    private Integer sanPhamId;
    private String tenSanPham;
    private String hinhAnh;
    private Integer soLuongBan;
    private BigDecimal doanhThu;
    private Double phanTramDoanhThu;
}
