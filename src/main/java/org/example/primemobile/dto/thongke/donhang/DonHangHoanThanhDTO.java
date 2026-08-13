package org.example.primemobile.dto.thongke.donhang;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonHangHoanThanhDTO {
    private Integer id;
    private String maDonHang;
    private String tenKhachHang;
    private String sdtKhachHang;
    private String trangThai;
    private BigDecimal tongThanhToan;
    private LocalDateTime ngayDat;
}
