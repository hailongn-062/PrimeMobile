package org.example.primemobile.dto.thongke.sanpham;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ThongKeTonKhoWarningDTO {
    private Integer sanPhamId;
    private String tenSanPham;
    private Integer tonKho;
    private Double soLuongBanTB;
    private Integer soNgayDuKienHetHang;
    private Long soNgayKhongBan;
    private BigDecimal giaTriVonTonDong;
}
