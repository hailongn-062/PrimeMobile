package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.thongke.donhang.*;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.repository.ThongKeRepository;
import org.example.primemobile.service.ThongKeDonHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ThongKeDonHangServiceImpl implements ThongKeDonHangService {

    private final ThongKeRepository thongKeRepository;

    @Override
    public ThongKeDonHangKpiDTO getDonHangKpi(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        ThongKeRepository.ThongKeDonHangKpi rawKpi = thongKeRepository.getDonHangKpi(start, end);
        return new ThongKeDonHangKpiDTO(
                rawKpi.getTongDon() != null ? rawKpi.getTongDon() : 0,
                rawKpi.getDonHoanThanh() != null ? rawKpi.getDonHoanThanh() : 0,
                rawKpi.getDonHuy() != null ? rawKpi.getDonHuy() : 0,
                rawKpi.getDonGiaoThatBai() != null ? rawKpi.getDonGiaoThatBai() : 0
        );
    }

    @Override
    public List<ThongKeTrangThaiDonHangDTO> getThongKeTrangThai(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        
        List<ThongKeRepository.ThongKeTrangThai> rawData = thongKeRepository.getThongKeTrangThai(start, end);
        
        int total = rawData.stream().mapToInt(t -> t.getSoLuong() != null ? t.getSoLuong() : 0).sum();
        
        List<ThongKeTrangThaiDonHangDTO> result = new ArrayList<>();
        for (ThongKeRepository.ThongKeTrangThai raw : rawData) {
            String label = getTrangThaiLabel(raw.getTrangThai());
            int soLuong = raw.getSoLuong() != null ? raw.getSoLuong() : 0;
            double tyTrong = total > 0 ? (double) soLuong / total * 100 : 0;
            result.add(new ThongKeTrangThaiDonHangDTO(label, soLuong, tyTrong));
        }
        return result;
    }

    @Override
    public Page<DonHangThatBaiDTO> getDonHangThatBai(LocalDate startDate, LocalDate endDate, String status, int page, int size) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<DonHang> donHangs = thongKeRepository.getDonHangThatBai(start, end, status != null ? status : "all", pageable);
        return donHangs.map(dh -> DonHangThatBaiDTO.builder()
                .id(dh.getId())
                .maDonHang(dh.getMaDonHang())
                .tenKhachHang(dh.getKhachHang() != null ? dh.getKhachHang().getHoTen() : "")
                .sdtKhachHang(dh.getKhachHang() != null ? dh.getKhachHang().getSoDienThoai() : "")
                .trangThai(dh.getTrangThai())
                .tongThanhToan(dh.getTongThanhToan())
                .lyDo(dh.getGhiChu())
                .ngayDat(dh.getNgayDat())
                .build());
    }

    @Override
    public Page<DonHangHoanThanhDTO> getDonHangHoanThanh(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);
        
        Page<DonHang> donHangs = thongKeRepository.getDonHangHoanThanh(start, end, pageable);
        return donHangs.map(dh -> DonHangHoanThanhDTO.builder()
                .id(dh.getId())
                .maDonHang(dh.getMaDonHang())
                .tenKhachHang(dh.getKhachHang() != null ? dh.getKhachHang().getHoTen() : "")
                .sdtKhachHang(dh.getKhachHang() != null ? dh.getKhachHang().getSoDienThoai() : "")
                .trangThai(dh.getTrangThai())
                .tongThanhToan(dh.getTongThanhToan())
                .ngayDat(dh.getNgayDat())
                .build());
    }

    private String getTrangThaiLabel(String code) {
        if (code == null) return "Khác";
        return switch (code) {
            case "cho_xac_nhan" -> "Chờ xác nhận";
            case "cho_hoan_tien" -> "Chờ hoàn tiền";
            case "da_xac_nhan" -> "Đã xác nhận";
            case "dang_giao" -> "Đang giao";
            case "da_hoan_thanh" -> "Đã hoàn thành";
            case "da_huy" -> "Đã hủy";
            case "giao_that_bai" -> "Giao thất bại";
            case "don_hang_cho" -> "Đơn hàng chờ";
            default -> code;
        };
    }
}
