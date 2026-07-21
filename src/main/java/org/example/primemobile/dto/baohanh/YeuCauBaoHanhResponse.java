package org.example.primemobile.dto.baohanh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class YeuCauBaoHanhResponse {
    private Integer id;
    private String maYeuCau;
    private String maPhieu;
    private String trangThai;
    private String moTaLoi;
    private String ghiChu;
    private String ketQuaTtbh;
    private LocalDateTime ngayTiepNhan;
    private LocalDateTime ngayGuiTtbh;
    private LocalDateTime ngayNhanLaiTtbh;
    private LocalDateTime ngayTraKhach;
    
    private String imei;
    private String tenTrungTam;
    private String tenNguoiTiepNhan;
    
    // Additional fields for detail view
    private String tenKhachHang;
    private String soDienThoai;
    private String tenSanPham;
    private String tenBienThe;
}
