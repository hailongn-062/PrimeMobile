package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.common.DropdownDTO;
import org.example.primemobile.dto.thongke.sanpham.*;
import org.example.primemobile.service.ThongKeSanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/thong-ke/san-pham")
@RequiredArgsConstructor
public class ThongKeSanPhamRestController {

    private final ThongKeSanPhamService thongKeSanPhamService;

    @GetMapping("/kpi")
    public ResponseEntity<ThongKeSanPhamKpiDTO> getKpi(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(thongKeSanPhamService.getKpi(fromDate, toDate));
    }

    @GetMapping("/top")
    public ResponseEntity<Page<ThongKeTopSanPhamDTO>> getTopSellingProducts(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "sortBy", defaultValue = "soLuong") String sortBy) {
        return ResponseEntity.ok(thongKeSanPhamService.getTopSellingProducts(fromDate, toDate, page, size, sortBy));
    }

    @GetMapping("/danh-muc")
    public ResponseEntity<List<ThongKeDanhMucDTO>> getCategoryDistribution(
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(thongKeSanPhamService.getCategoryDistribution(fromDate, toDate));
    }

    @GetMapping("/bien-the/{sanPhamId}")
    public ResponseEntity<List<ThongKeBienTheDTO>> getVariantStats(
            @PathVariable Integer sanPhamId,
            @RequestParam("fromDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam("toDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(thongKeSanPhamService.getVariantStats(sanPhamId, fromDate, toDate));
    }

    @GetMapping("/canh-bao/sap-het-hang")
    public ResponseEntity<Page<ThongKeTonKhoWarningDTO>> getLowStockWarnings(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(thongKeSanPhamService.getLowStockWarnings(page, size));
    }

    @GetMapping("/canh-bao/ton-kho-lau")
    public ResponseEntity<Page<ThongKeTonKhoWarningDTO>> getOldStockWarnings(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {
        return ResponseEntity.ok(thongKeSanPhamService.getOldStockWarnings(page, size));
    }
    
    @GetMapping("/dropdown")
    public ResponseEntity<List<DropdownDTO>> getAllSanPhamDropdown() {
        return ResponseEntity.ok(thongKeSanPhamService.getAllSanPhamDropdown());
    }
}
