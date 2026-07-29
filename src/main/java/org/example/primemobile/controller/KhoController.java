package org.example.primemobile.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.TonKho;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.service.IKhoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.example.primemobile.service.impl.ExcelHelperService;
import org.example.primemobile.service.impl.ImeiValidationService;
import org.example.primemobile.dto.kho.ImeiImportRecord;
import org.example.primemobile.dto.kho.ImeiImportPreviewResponse;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest.ChiTietNhapRequest;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/kho")
@RequiredArgsConstructor
public class KhoController {

    private final IKhoService khoService;
    private final TonKhoRepository tonKhoRepository;
    private final ExcelHelperService excelHelperService;
    private final ImeiValidationService imeiValidationService;
    private final ObjectMapper objectMapper;

    /**
     * Tra cứu số lượng tồn kho theo thời gian thực
     * @param khoId (Optional) ID Kho cần lọc
     * @return Danh sách TonKho đã được map sang DTO
     */
    @GetMapping("/ton-kho")
    public List<TonKhoResponse> getTonKho(@RequestParam(required = false) Integer khoId,
                                          @RequestParam(required = false) String maSku,
                                          @RequestParam(required = false) String tenSanPham) {
        Integer finalKhoId = (khoId != null && khoId > 0) ? khoId : null;
        String finalMaSku = (maSku != null && !maSku.trim().isEmpty()) ? maSku.trim() : null;
        String finalTenSanPham = (tenSanPham != null && !tenSanPham.trim().isEmpty()) ? tenSanPham.trim() : null;

        List<TonKho> list = tonKhoRepository.timKiemTonKho(finalKhoId, finalMaSku, finalTenSanPham);

        return list.stream().map(tk -> new TonKhoResponse(
                tk.getKho() != null ? tk.getKho().getTenKho() : "N/A",
                tk.getBienTheSanPham() != null ? tk.getBienTheSanPham().getMaSku() : "N/A",
                tk.getBienTheSanPham() != null && tk.getBienTheSanPham().getSanPham() != null
                        ? tk.getBienTheSanPham().getSanPham().getTenSanPham() : "N/A",
                tk.getSoLuong(),
                tk.getBienTheSanPham() != null ? tk.getBienTheSanPham().getId() : null,
                tk.getKho() != null ? tk.getKho().getId() : null
        )).collect(Collectors.toList());
    }

    /**
     * Upload file Excel chứa IMEI và validate (Preview)
     */
    @PostMapping("/import-imei-preview")
    public ResponseEntity<?> importImeiPreview(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "chiTietNhapJson", required = false) String chiTietNhapJson) {
        
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body("File Excel không được để trống");
            }
            
            // Lấy danh sách hàng hóa đang đặt trong form nhập kho
            List<ChiTietNhapRequest> orderedItems = null;
            if (chiTietNhapJson != null && !chiTietNhapJson.isEmpty()) {
                orderedItems = objectMapper.readValue(chiTietNhapJson, new TypeReference<List<ChiTietNhapRequest>>() {});
            }
            
            // Parse Excel
            List<ImeiImportRecord> records = excelHelperService.parseImeiExcel(file);
            
            // Validate
            ImeiImportPreviewResponse response = imeiValidationService.validateImportRecords(records, orderedItems);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Lỗi khi import IMEI preview: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Đã xảy ra lỗi khi đọc file Excel: " + e.getMessage());
        }
    }

    /**
     * DTO tĩnh để trả về JSON gọn nhẹ
     */
    @Data
    @AllArgsConstructor
    public static class TonKhoResponse {
        private String tenKho;
        private String maSku;
        private String tenSanPham;
        private int soLuong;
        private Integer bienTheSanPhamId;  // ← THÊM MỚI
        private Integer khoId;             // ← THÊM MỚI
    }
}