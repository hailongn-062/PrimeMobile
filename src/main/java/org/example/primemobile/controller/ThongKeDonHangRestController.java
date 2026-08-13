package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.thongke.donhang.DonHangHoanThanhDTO;
import org.example.primemobile.dto.thongke.donhang.DonHangThatBaiDTO;
import org.example.primemobile.dto.thongke.donhang.ThongKeDonHangKpiDTO;
import org.example.primemobile.dto.thongke.donhang.ThongKeTrangThaiDonHangDTO;
import org.example.primemobile.service.ThongKeDonHangService;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/thong-ke/don-hang")
@RequiredArgsConstructor
public class ThongKeDonHangRestController {

    private final ThongKeDonHangService thongKeDonHangService;

    @GetMapping("/kpi")
    public ResponseEntity<ThongKeDonHangKpiDTO> getDonHangKpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(thongKeDonHangService.getDonHangKpi(fromDate, toDate));
    }

    @GetMapping("/trang-thai")
    public ResponseEntity<List<ThongKeTrangThaiDonHangDTO>> getThongKeTrangThai(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {
        return ResponseEntity.ok(thongKeDonHangService.getThongKeTrangThai(fromDate, toDate));
    }

    @GetMapping("/that-bai")
    public ResponseEntity<Page<DonHangThatBaiDTO>> getDonHangThatBai(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(thongKeDonHangService.getDonHangThatBai(fromDate, toDate, status, page, size));
    }

    @GetMapping("/hoan-thanh")
    public ResponseEntity<Page<DonHangHoanThanhDTO>> getDonHangHoanThanh(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(thongKeDonHangService.getDonHangHoanThanh(fromDate, toDate, page, size));
    }
}
