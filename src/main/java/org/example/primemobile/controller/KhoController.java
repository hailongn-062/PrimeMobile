package org.example.primemobile.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.TonKho;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.service.IKhoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/kho")
@RequiredArgsConstructor
public class KhoController {

    private final IKhoService khoService;
    private final TonKhoRepository tonKhoRepository;

    /**
     * Tra cứu số lượng tồn kho theo thời gian thực
     * @param khoId (Optional) ID Kho cần lọc
     * @return Danh sách TonKho đã được map sang DTO
     */
    @GetMapping("/ton-kho")
    public List<TonKhoResponse> getTonKho(@RequestParam(required = false) Integer khoId) {
        List<TonKho> list;
        
        if (khoId != null && khoId > 0) {
            // Sử dụng IKhoService đã có sẵn để lấy tồn kho của 1 kho (eager fetch)
            list = khoService.getTonKhoByKho(khoId);
        } else {
            // Lấy toàn bộ tồn kho nếu không truyền khoId
            list = tonKhoRepository.findAll();
        }

        return list.stream().map(tk -> new TonKhoResponse(
                tk.getKho() != null ? tk.getKho().getTenKho() : "N/A",
                tk.getBienTheSanPham() != null && tk.getBienTheSanPham().getSanPham() != null 
                        ? tk.getBienTheSanPham().getSanPham().getTenSanPham() : "N/A",
                tk.getBienTheSanPham() != null ? tk.getBienTheSanPham().getMaSku() : "N/A",
                tk.getSoLuong()
        )).collect(Collectors.toList());
    }

    /**
     * DTO tĩnh để trả về JSON gọn nhẹ
     */
    @Data
    @AllArgsConstructor
    public static class TonKhoResponse {
        private String tenKho;
        private String tenSanPham;
        private String maSku;
        private int soLuong;
    }
}
