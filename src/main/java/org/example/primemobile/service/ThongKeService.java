package org.example.primemobile.service;

import org.example.primemobile.dto.thongke.*;
import java.time.LocalDate;
import java.util.List;

public interface ThongKeService {
    ThongKeKpiDTO getKpi(LocalDate startDate, LocalDate endDate);
    List<ThongKeBieuDoNgayDTO> getBieuDoDoanhThu(LocalDate startDate, LocalDate endDate);
    ThongKeKenhDTO getThongKeKenh(LocalDate startDate, LocalDate endDate);

    List<ThongKeChiTietNgayDTO> getChiTietTheoNgay(LocalDate startDate, LocalDate endDate, int page, int size);
}
