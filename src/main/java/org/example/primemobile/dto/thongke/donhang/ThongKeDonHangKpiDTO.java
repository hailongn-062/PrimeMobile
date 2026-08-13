package org.example.primemobile.dto.thongke.donhang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeDonHangKpiDTO {
    private Integer tongDon;
    private Integer donHoanThanh;
    private Integer donHuy;
    private Integer donGiaoThatBai;
}
