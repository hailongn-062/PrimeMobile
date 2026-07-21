package org.example.primemobile.dto.baohanh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraCuuBaoHanhResponse {
    private Integer idPhieu;
    private String tenSanPham;
    private String tenBienThe;
    private String imei;
    private String tenKhachHang;
    private String soDienThoai;
    private LocalDate ngayBatDau;
    private LocalDate ngayHetHan;
}
