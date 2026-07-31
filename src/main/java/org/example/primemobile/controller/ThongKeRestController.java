package org.example.primemobile.controller;

import org.example.primemobile.dto.thongke.*;
import org.example.primemobile.service.ThongKeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/thong-ke")
public class ThongKeRestController {

    @Autowired
    private ThongKeService thongKeService;

    @GetMapping("/kpi")
    public ResponseEntity<ThongKeKpiDTO> getKpi(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(thongKeService.getKpi(startDate, endDate));
    }

    @GetMapping("/bieu-do")
    public ResponseEntity<List<ThongKeBieuDoNgayDTO>> getBieuDo(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(thongKeService.getBieuDoDoanhThu(startDate, endDate));
    }

    @GetMapping("/kenh")
    public ResponseEntity<ThongKeKenhDTO> getThongKeKenh(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(thongKeService.getThongKeKenh(startDate, endDate));
    }



    @GetMapping("/chi-tiet-ngay")
    public ResponseEntity<List<ThongKeChiTietNgayDTO>> getChiTietNgay(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(thongKeService.getChiTietTheoNgay(startDate, endDate, page, size));
    }
}
