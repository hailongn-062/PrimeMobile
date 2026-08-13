package org.example.primemobile.service;

import org.example.primemobile.dto.thongke.donhang.*;
import org.springframework.data.domain.Page;
import java.time.LocalDate;
import java.util.List;

public interface ThongKeDonHangService {
    ThongKeDonHangKpiDTO getDonHangKpi(LocalDate startDate, LocalDate endDate);
    List<ThongKeTrangThaiDonHangDTO> getThongKeTrangThai(LocalDate startDate, LocalDate endDate);
    Page<DonHangThatBaiDTO> getDonHangThatBai(LocalDate startDate, LocalDate endDate, String status, int page, int size);
    Page<DonHangHoanThanhDTO> getDonHangHoanThanh(LocalDate startDate, LocalDate endDate, int page, int size);
}
