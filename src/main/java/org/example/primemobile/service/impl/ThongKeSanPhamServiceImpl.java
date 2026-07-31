package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.common.DropdownDTO;
import org.example.primemobile.dto.thongke.sanpham.*;
import org.example.primemobile.repository.ThongKeSanPhamRepository;
import org.example.primemobile.service.ThongKeSanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ThongKeSanPhamServiceImpl implements ThongKeSanPhamService {

    private final ThongKeSanPhamRepository thongKeSanPhamRepository;
    private static final int LOW_STOCK_THRESHOLD = 5;
    private static final int OLD_STOCK_DAYS = 30;

    @Override
    public ThongKeSanPhamKpiDTO getKpi(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(OLD_STOCK_DAYS);

        int tongSanPham = thongKeSanPhamRepository.countActiveProducts();
        int sanPhamDaBan = thongKeSanPhamRepository.countProductsSold(start, end);
        int sapHetHang = thongKeSanPhamRepository.countLowStockProducts(LOW_STOCK_THRESHOLD);
        int tonKhoLau = thongKeSanPhamRepository.countOldStockProducts(last30Days);

        return ThongKeSanPhamKpiDTO.builder()
                .tongSanPham(tongSanPham)
                .sanPhamDaBan(sanPhamDaBan)
                .sapHetHang(sapHetHang)
                .tonKhoLau(tonKhoLau)
                .build();
    }

    @Override
    public Page<ThongKeTopSanPhamDTO> getTopSellingProducts(LocalDate fromDate, LocalDate toDate, int page, int size, String sortBy) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);
        Pageable pageable = PageRequest.of(page, size);

        Page<ThongKeSanPhamRepository.TopSanPhamProjection> topProjections = thongKeSanPhamRepository.getTopSellingProducts(start, end, sortBy, pageable);
        
        // Cần tính tổng doanh thu để tính % 
        BigDecimal tongDoanhThuToanKy = BigDecimal.ZERO;
        if (!topProjections.isEmpty()) {
            // Lấy tổng doanh thu của top 100 hoặc query riêng. Ở đây ta tính xấp xỉ phần trăm dựa trên item bán chạy nhất. 
            // Tuy nhiên, theo yêu cầu: Thanh progress bar thể hiện tỷ lệ so với sản phẩm bán chạy nhất.
            // Vậy ta có thể lấy doanh thu hoặc số lượng của sản phẩm đứng top 1 trong list hiện tại.
        }
        
        List<ThongKeTopSanPhamDTO> dtos = topProjections.getContent().stream().map(p -> {
            return ThongKeTopSanPhamDTO.builder()
                    .sanPhamId(p.getSanPhamId())
                    .tenSanPham(p.getTenSanPham())
                    .hinhAnh(p.getHinhAnh() != null ? p.getHinhAnh() : "/images/default-product.png")
                    .soLuongBan(p.getSoLuongBan() != null ? p.getSoLuongBan() : 0)
                    .doanhThu(p.getDoanhThu() != null ? p.getDoanhThu() : BigDecimal.ZERO)
                    .phanTramDoanhThu(0.0) // Sẽ tính sau
                    .build();
        }).collect(Collectors.toList());
        
        if (!dtos.isEmpty()) {
            double maxValue = "doanhThu".equals(sortBy) 
                    ? dtos.get(0).getDoanhThu().doubleValue() 
                    : dtos.get(0).getSoLuongBan().doubleValue();
            
            for (ThongKeTopSanPhamDTO dto : dtos) {
                double currentValue = "doanhThu".equals(sortBy) 
                        ? dto.getDoanhThu().doubleValue() 
                        : dto.getSoLuongBan().doubleValue();
                
                if (maxValue > 0) {
                    dto.setPhanTramDoanhThu((currentValue / maxValue) * 100.0);
                }
            }
        }

        return new PageImpl<>(dtos, pageable, topProjections.getTotalElements());
    }

    @Override
    public List<ThongKeDanhMucDTO> getCategoryDistribution(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        List<ThongKeSanPhamRepository.DanhMucProjection> projections = thongKeSanPhamRepository.getCategoryDistribution(start, end);
        
        BigDecimal totalDoanhThu = projections.stream()
                .map(p -> p.getDoanhThu() != null ? p.getDoanhThu() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return projections.stream().map(p -> {
            BigDecimal dt = p.getDoanhThu() != null ? p.getDoanhThu() : BigDecimal.ZERO;
            double phanTram = 0.0;
            if (totalDoanhThu.compareTo(BigDecimal.ZERO) > 0) {
                phanTram = dt.divide(totalDoanhThu, 4, RoundingMode.HALF_UP).doubleValue() * 100.0;
            }
            return ThongKeDanhMucDTO.builder()
                    .tenDanhMuc(p.getTenDanhMuc())
                    .soLuongBan(p.getSoLuongBan() != null ? p.getSoLuongBan() : 0)
                    .doanhThu(dt)
                    .phanTram(phanTram)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<ThongKeBienTheDTO> getVariantStats(Integer sanPhamId, LocalDate fromDate, LocalDate toDate) {
        LocalDateTime start = fromDate.atStartOfDay();
        LocalDateTime end = toDate.atTime(LocalTime.MAX);

        return thongKeSanPhamRepository.getVariantStats(sanPhamId, start, end).stream().map(p ->
            ThongKeBienTheDTO.builder()
                    .bienTheId(p.getBienTheId())
                    .tenBienThe(p.getTenBienThe())
                    .soLuongBan(p.getSoLuongBan() != null ? p.getSoLuongBan() : 0)
                    .doanhThu(p.getDoanhThu() != null ? p.getDoanhThu() : BigDecimal.ZERO)
                    .tonKho(p.getTonKho() != null ? p.getTonKho() : 0)
                    .build()
        ).collect(Collectors.toList());
    }

    @Override
    public Page<ThongKeTonKhoWarningDTO> getLowStockWarnings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        Page<ThongKeSanPhamRepository.TonKhoWarningProjection> projections = 
            thongKeSanPhamRepository.getLowStockWarnings(LOW_STOCK_THRESHOLD, last30Days, pageable);

        List<ThongKeTonKhoWarningDTO> dtos = projections.getContent().stream().map(p -> {
            int tonKho = p.getTonKho() != null ? p.getTonKho() : 0;
            int soLuongBan30Ngay = p.getSoLuongBan30Ngay() != null ? p.getSoLuongBan30Ngay() : 0;
            double banTrungBinhNgay = soLuongBan30Ngay / 30.0;
            int soNgayHetHang = banTrungBinhNgay > 0 ? (int) (tonKho / banTrungBinhNgay) : 999;

            return ThongKeTonKhoWarningDTO.builder()
                    .sanPhamId(p.getSanPhamId())
                    .tenSanPham(p.getTenSanPham())
                    .tonKho(tonKho)
                    .soLuongBanTB(banTrungBinhNgay)
                    .soNgayDuKienHetHang(soNgayHetHang)
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, projections.getTotalElements());
    }

    @Override
    public Page<ThongKeTonKhoWarningDTO> getOldStockWarnings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        LocalDateTime dateThreshold = LocalDateTime.now().minusDays(OLD_STOCK_DAYS);
        LocalDateTime last30Days = LocalDateTime.now().minusDays(30);

        Page<ThongKeSanPhamRepository.TonKhoWarningProjection> projections = 
            thongKeSanPhamRepository.getOldStockWarnings(dateThreshold, last30Days, pageable);

        List<ThongKeTonKhoWarningDTO> dtos = projections.getContent().stream().map(p -> {
            int tonKho = p.getTonKho() != null ? p.getTonKho() : 0;
            long daysNotSold = OLD_STOCK_DAYS;
            if (p.getNgayBanCuoi() != null) {
                daysNotSold = ChronoUnit.DAYS.between(p.getNgayBanCuoi(), LocalDateTime.now());
            }

            BigDecimal giaNhap = thongKeSanPhamRepository.getLatestImportPriceBySanPhamId(p.getSanPhamId());
            if (giaNhap == null) giaNhap = BigDecimal.ZERO;
            
            BigDecimal giaTriVon = giaNhap.multiply(new BigDecimal(tonKho));

            return ThongKeTonKhoWarningDTO.builder()
                    .sanPhamId(p.getSanPhamId())
                    .tenSanPham(p.getTenSanPham())
                    .tonKho(tonKho)
                    .soNgayKhongBan(daysNotSold)
                    .giaTriVonTonDong(giaTriVon)
                    .build();
        }).collect(Collectors.toList());

        return new PageImpl<>(dtos, pageable, projections.getTotalElements());
    }

    @Override
    public List<DropdownDTO> getAllSanPhamDropdown() {
        return thongKeSanPhamRepository.getAllSanPhamDropdown();
    }
}
