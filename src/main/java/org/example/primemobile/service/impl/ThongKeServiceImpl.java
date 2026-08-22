package org.example.primemobile.service.impl;

import org.example.primemobile.dto.thongke.*;
import org.example.primemobile.repository.ThongKeRepository;
import org.example.primemobile.service.ThongKeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ThongKeServiceImpl implements ThongKeService {

    @Autowired
    private ThongKeRepository thongKeRepository;

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private int defaultZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double calculatePercentageChange(double current, double previous) {
        if (previous == 0) return current > 0 ? 100.0 : 0.0;
        return ((current - previous) / previous) * 100.0;
    }

    @Override
    public ThongKeKpiDTO getKpi(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDateTime prevStart = startDate.minusDays(daysBetween).atStartOfDay();
        LocalDateTime prevEnd = startDate.atStartOfDay();

        ThongKeRepository.ThongKeTongQuan current = thongKeRepository.getTongQuan(start, end);
        ThongKeRepository.ThongKeTongQuan previous = thongKeRepository.getTongQuan(prevStart, prevEnd);

        BigDecimal currentRevenue = defaultZero(current.getDoanhThu());
        BigDecimal prevRevenue = defaultZero(previous.getDoanhThu());
        int currentOrders = defaultZero(current.getTongDon());
        int prevOrders = defaultZero(previous.getTongDon());
        int currentCancels = defaultZero(current.getDonHuy());
        int prevCancels = defaultZero(previous.getDonHuy());

        BigDecimal currentAov = currentOrders > 0 ? currentRevenue.divide(BigDecimal.valueOf(currentOrders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal prevAov = prevOrders > 0 ? prevRevenue.divide(BigDecimal.valueOf(prevOrders), 2, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;

        double currentCancelRate = currentOrders > 0 ? (double) currentCancels / currentOrders * 100 : 0.0;
        double prevCancelRate = prevOrders > 0 ? (double) prevCancels / prevOrders * 100 : 0.0;

        return ThongKeKpiDTO.builder()
                .tongDoanhThu(currentRevenue)
                .tongDoanhThuKyTruoc(prevRevenue)
                .ptChangeDoanhThu(calculatePercentageChange(currentRevenue.doubleValue(), prevRevenue.doubleValue()))
                .soDonHang(currentOrders)
                .soDonHangKyTruoc(prevOrders)
                .ptChangeSoDonHang(calculatePercentageChange(currentOrders, prevOrders))
                .aov(currentAov)
                .aovKyTruoc(prevAov)
                .ptChangeAov(calculatePercentageChange(currentAov.doubleValue(), prevAov.doubleValue()))
                .tyLeHuy(currentCancelRate)
                .tyLeHuyKyTruoc(prevCancelRate)
                .ptChangeTyLeHuy(currentCancelRate - prevCancelRate)
                .build();
    }

    @Override
    public List<ThongKeBieuDoNgayDTO> getBieuDoDoanhThu(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        long daysBetween = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        LocalDateTime prevStart = startDate.minusDays(daysBetween).atStartOfDay();
        LocalDateTime prevEnd = startDate.atStartOfDay();

        List<ThongKeRepository.ThongKeNgay> currentList = thongKeRepository.getThongKeTheoNgay(start, end);
        List<ThongKeRepository.ThongKeNgay> prevList = thongKeRepository.getThongKeTheoNgay(prevStart, prevEnd);

        Map<String, ThongKeRepository.ThongKeNgay> currentMap = currentList.stream()
                .collect(Collectors.toMap(ThongKeRepository.ThongKeNgay::getNgay, t -> t));
        Map<String, ThongKeRepository.ThongKeNgay> prevMap = prevList.stream()
                .collect(Collectors.toMap(ThongKeRepository.ThongKeNgay::getNgay, t -> t));

        List<ThongKeBieuDoNgayDTO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate currentDate = startDate;
        LocalDate prevDate = startDate.minusDays(daysBetween);

        while (!currentDate.isAfter(endDate)) {
            String currentStr = currentDate.format(formatter);
            String prevStr = prevDate.format(formatter);

            ThongKeRepository.ThongKeNgay currData = currentMap.get(currentStr);
            ThongKeRepository.ThongKeNgay prevData = prevMap.get(prevStr);

            result.add(ThongKeBieuDoNgayDTO.builder()
                    .ngay(currentStr)
                    .doanhThuHienTai(currData != null ? defaultZero(currData.getDoanhThu()) : BigDecimal.ZERO)
                    .doanhThuKyTruoc(prevData != null ? defaultZero(prevData.getDoanhThu()) : BigDecimal.ZERO)
                    .soDonHienTai(currData != null ? defaultZero(currData.getSoDon()) : 0)
                    .soDonKyTruoc(prevData != null ? defaultZero(prevData.getSoDon()) : 0)
                    .build());

            currentDate = currentDate.plusDays(1);
            prevDate = prevDate.plusDays(1);
        }

        return result;
    }

    @Override
    public ThongKeKenhDTO getThongKeKenh(LocalDate startDate, LocalDate endDate) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<ThongKeRepository.ThongKeKenh> list = thongKeRepository.getThongKeTheoKenh(start, end);
        BigDecimal onlineRev = BigDecimal.ZERO;
        BigDecimal offlineRev = BigDecimal.ZERO;
        int onlineOrders = 0;
        int offlineOrders = 0;

        for (ThongKeRepository.ThongKeKenh k : list) {
            if ("online".equals(k.getKenhBan())) {
                onlineRev = defaultZero(k.getDoanhThu());
                onlineOrders = defaultZero(k.getSoDon());
            } else {
                offlineRev = defaultZero(k.getDoanhThu());
                offlineOrders = defaultZero(k.getSoDon());
            }
        }

        BigDecimal totalRev = onlineRev.add(offlineRev);
        double tyLeOnline = totalRev.compareTo(BigDecimal.ZERO) > 0 ?
                (onlineRev.doubleValue() / totalRev.doubleValue()) * 100 : 0.0;
        double tyLeOffline = totalRev.compareTo(BigDecimal.ZERO) > 0 ?
                (offlineRev.doubleValue() / totalRev.doubleValue()) * 100 : 0.0;

        return ThongKeKenhDTO.builder()
                .doanhThuOnline(onlineRev)
                .soDonOnline(onlineOrders)
                .tyLeOnline(tyLeOnline)
                .doanhThuOffline(offlineRev)
                .soDonOffline(offlineOrders)
                .tyLeOffline(tyLeOffline)
                .build();
    }

    @Override
    public List<ThongKeChiTietNgayDTO> getChiTietTheoNgay(LocalDate startDate, LocalDate endDate, int page, int size) {
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = endDate.plusDays(1).atStartOfDay();

        List<ThongKeRepository.ThongKeNgay> list = thongKeRepository.getThongKeTheoNgay(start, end);
        Map<String, ThongKeRepository.ThongKeNgay> map = list.stream()
                .collect(Collectors.toMap(ThongKeRepository.ThongKeNgay::getNgay, t -> t));

        List<ThongKeChiTietNgayDTO> fullList = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate currentDate = endDate; // Sort by descending date
        while (!currentDate.isBefore(startDate)) {
            String dateStr = currentDate.format(formatter);
            ThongKeRepository.ThongKeNgay data = map.get(dateStr);

            fullList.add(ThongKeChiTietNgayDTO.builder()
                    .ngay(dateStr)
                    .tongDoanhThu(data != null ? defaultZero(data.getDoanhThu()) : BigDecimal.ZERO)
                    .doanhThuOnline(data != null ? defaultZero(data.getDoanhThuOnline()) : BigDecimal.ZERO)
                    .doanhThuOffline(data != null ? defaultZero(data.getDoanhThuOffline()) : BigDecimal.ZERO)
                    .soDonHang(data != null ? defaultZero(data.getSoDon()) : 0)
                    .soDonHuy(data != null ? defaultZero(data.getDonHuy()) : 0)
                    .soDonGiaoThatBai(data != null ? defaultZero(data.getDonGiaoThatBai()) : 0)
                    .build());

            currentDate = currentDate.minusDays(1);
        }

        // Pagination
        int skip = page * size;
        if (skip >= fullList.size()) {
            return new ArrayList<>();
        }
        return fullList.subList(skip, Math.min(skip + size, fullList.size()));
    }
}
