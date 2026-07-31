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
public class ThongKeBieuDoNgayDTO {
    private String ngay; // Format: yyyy-MM-dd
    private BigDecimal doanhThuHienTai;
    private BigDecimal doanhThuKyTruoc;
    private Integer soDonHienTai;
    private Integer soDonKyTruoc;
}
