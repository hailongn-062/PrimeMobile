package org.example.primemobile.dto.thongke.donhang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ThongKeTrangThaiDonHangDTO {
    private String trangThai;
    private Integer soLuong;
    private Double tyTrong;
}
