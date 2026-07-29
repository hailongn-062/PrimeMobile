package org.example.primemobile.service;

import org.example.primemobile.dto.baohanh.TaoYeuCauBaoHanhRequest;
import org.example.primemobile.entity.PhieuBaoHanh;
import org.example.primemobile.entity.YeuCauBaoHanh;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IBaoHanhService {
    
    PhieuBaoHanh traCuuPhieuBaoHanh(String imei);

    List<org.example.primemobile.dto.baohanh.TraCuuBaoHanhResponse> traCuuTheoSoDienThoai(String sdt);

    /**
     * Tự động tạo Phiếu bảo hành cho tất cả MayDienThoai thuộc đơn hàng vừa hoàn thành.
     * Được gọi ngay sau khi đơn hàng chuyển sang trạng thái "da_hoan_thanh".
     * Chỉ tạo phiếu cho sản phẩm có baoHanhThang > 0.
     */
    void taoPhieuBaoHanhChoDonHang(org.example.primemobile.entity.DonHang donHang);
    
    YeuCauBaoHanh taoYeuCauBaoHanh(Integer nguoiTiepNhanId, TaoYeuCauBaoHanhRequest request);
    
    YeuCauBaoHanh capNhatTrangThaiYeuCau(Integer yeuCauId, String trangThaiMoi, String ketQua);
    
    List<String> goiYSoDienThoai(String sdt);

    List<String> goiYImei(String imei);
    
    Page<YeuCauBaoHanh> layDanhSachYeuCau(Pageable pageable);

    /**
     * Trả về danh sách DTO an toàn (không có circular reference) cho trang quản lý bảo hành.
     */
    List<org.example.primemobile.dto.baohanh.YeuCauBaoHanhResponse> layDanhSachYeuCauDtos();

    List<org.example.primemobile.dto.baohanh.YeuCauBaoHanhResponse> timKiemVaLoc(
        String tuKhoa, String trangThai, Integer trungTamBhId, java.time.LocalDateTime tuNgay, java.time.LocalDateTime denNgay
    );
    
    List<YeuCauBaoHanh> layLichSuBaoHanhKhachHang(Integer khachHangId);
}
