package org.example.primemobile.service;

import org.example.primemobile.dto.common.DropdownDTO;
import org.example.primemobile.dto.thongke.sanpham.*;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface ThongKeSanPhamService {

    ThongKeSanPhamKpiDTO getKpi(LocalDate fromDate, LocalDate toDate);

    Page<ThongKeTopSanPhamDTO> getTopSellingProducts(LocalDate fromDate, LocalDate toDate, int page, int size, String sortBy);

    List<ThongKeDanhMucDTO> getCategoryDistribution(LocalDate fromDate, LocalDate toDate);

    List<ThongKeBienTheDTO> getVariantStats(Integer sanPhamId, LocalDate fromDate, LocalDate toDate);

    Page<ThongKeTonKhoWarningDTO> getLowStockWarnings(int page, int size);

    Page<ThongKeTonKhoWarningDTO> getOldStockWarnings(int page, int size);
    
    List<DropdownDTO> getAllSanPhamDropdown();
}
