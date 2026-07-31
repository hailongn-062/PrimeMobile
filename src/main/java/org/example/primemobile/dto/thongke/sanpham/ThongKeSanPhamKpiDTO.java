package org.example.primemobile.dto.thongke.sanpham;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThongKeSanPhamKpiDTO {
    private int tongSanPham;
    private int sanPhamDaBan;
    private int sapHetHang;
    private int tonKhoLau;
}
