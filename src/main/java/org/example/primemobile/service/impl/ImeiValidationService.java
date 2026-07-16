package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.kho.ImeiImportPreviewResponse;
import org.example.primemobile.dto.kho.ImeiImportRecord;
import org.example.primemobile.dto.kho.SkuImportStatus;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest.ChiTietNhapRequest;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.MayDienThoaiRepository;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImeiValidationService {

    private final MayDienThoaiRepository mayDienThoaiRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    public ImeiImportPreviewResponse validateImportRecords(List<ImeiImportRecord> records, List<ChiTietNhapRequest> orderedItems) {
        log.info("[ImeiValidation] Bắt đầu validate {} bản ghi Excel", records.size());

        // Gom nhóm theo SKU
        Map<String, List<ImeiImportRecord>> recordsBySku = records.stream()
                .collect(Collectors.groupingBy(ImeiImportRecord::getSku));

        Map<String, Integer> orderedQuantities = new HashMap<>();
        if (orderedItems != null) {
            for (ChiTietNhapRequest req : orderedItems) {
                // req chỉ có bienTheSanPhamId, cần lấy mã SKU từ DB để đối chiếu
                bienTheSanPhamRepository.findById(req.getBienTheSanPhamId()).ifPresent(bt -> {
                    orderedQuantities.put(bt.getMaSku(), req.getSoLuong());
                });
            }
        }

        List<SkuImportStatus> statuses = new ArrayList<>();
        boolean isAllAcceptable = true;
        int totalOrdered = orderedQuantities.values().stream().mapToInt(Integer::intValue).sum();
        int totalExcel = 0;

        Set<String> seenImeis = new HashSet<>();

        // Validate từng SKU có trong Excel
        for (Map.Entry<String, List<ImeiImportRecord>> entry : recordsBySku.entrySet()) {
            String sku = entry.getKey();
            List<ImeiImportRecord> skuRecords = entry.getValue();
            totalExcel += skuRecords.size();

            int orderedQty = orderedQuantities.getOrDefault(sku, 0);

            List<ImeiImportRecord> validRecords = new ArrayList<>();
            List<ImeiImportRecord> errorRecords = new ArrayList<>();
            boolean skuExists = bienTheSanPhamRepository.existsByMaSku(sku);
            
            for (ImeiImportRecord record : skuRecords) {
                validateSingleRecord(record, skuExists, seenImeis);
                if (record.getErrors().isEmpty()) {
                    validRecords.add(record);
                } else {
                    errorRecords.add(record);
                }
            }

            String statusText;
            boolean acceptable = false;

            if (!skuExists || orderedQty == 0) {
                statusText = "❌ Sai SKU (Không có trong phiếu nhập)";
            } else if (!errorRecords.isEmpty()) {
                statusText = "❌ Lỗi định dạng/Trùng IMEI";
            } else if (skuRecords.size() > orderedQty) {
                statusText = "❌ Dư " + (skuRecords.size() - orderedQty);
            } else if (skuRecords.size() < orderedQty) {
                statusText = "⚠️ Thiếu " + (orderedQty - skuRecords.size());
                acceptable = true; // Thiếu thì cho phép nhập 1 phần
            } else {
                statusText = "✅ Hợp lệ";
                acceptable = true;
            }

            if (!acceptable) {
                isAllAcceptable = false;
            }

            statuses.add(SkuImportStatus.builder()
                    .sku(sku)
                    .orderedQuantity(orderedQty)
                    .excelQuantity(skuRecords.size())
                    .statusText(statusText)
                    .isAcceptable(acceptable)
                    .validRecords(validRecords)
                    .errorRecords(errorRecords)
                    .build());
            
            // Xóa khỏi danh sách đối chiếu để lát nữa tìm các SKU có đặt mà không có trong Excel
            orderedQuantities.remove(sku);
        }

        // Các SKU có trong phiếu nhập nhưng KHÔNG CÓ TRONG EXCEL
        for (Map.Entry<String, Integer> entry : orderedQuantities.entrySet()) {
            statuses.add(SkuImportStatus.builder()
                    .sku(entry.getKey())
                    .orderedQuantity(entry.getValue())
                    .excelQuantity(0)
                    .statusText("⚠️ Thiếu " + entry.getValue() + " (Chưa có IMEI)")
                    .isAcceptable(true) // Có thể chưa nhập IMEI bây giờ
                    .validRecords(new ArrayList<>())
                    .errorRecords(new ArrayList<>())
                    .build());
        }

        return ImeiImportPreviewResponse.builder()
                .isAllAcceptable(isAllAcceptable)
                .totalOrdered(totalOrdered)
                .totalExcel(totalExcel)
                .skuStatuses(statuses)
                .build();
    }

    private void validateSingleRecord(ImeiImportRecord record, boolean skuExists, Set<String> seenImeis) {
        if (!skuExists) {
            record.addError("SKU không tồn tại trong hệ thống");
        }

        String imei1 = record.getImei1();
        String imei2 = record.getImei2();

        if (imei1 == null || imei1.isEmpty()) {
            record.addError("IMEI 1 không được để trống");
        } else {
            if (imei1.length() != 15) record.addError("IMEI 1 phải đủ 15 ký tự");
            if (!imei1.matches("\\d+")) record.addError("IMEI 1 chỉ được chứa số");
            
            if (!seenImeis.add(imei1)) {
                record.addError("IMEI 1 bị trùng trong file Excel");
            } else if (mayDienThoaiRepository.existsByImei1(imei1) || mayDienThoaiRepository.existsByImei2(imei1)) {
                record.addError("IMEI 1 đã tồn tại trong hệ thống");
            }
        }

        if (imei2 != null && !imei2.isEmpty()) {
            if (imei2.length() != 15) record.addError("IMEI 2 phải đủ 15 ký tự");
            if (!imei2.matches("\\d+")) record.addError("IMEI 2 chỉ được chứa số");
            
            if (imei1 != null && imei1.equals(imei2)) {
                record.addError("IMEI 1 và IMEI 2 không được giống nhau");
            }
            
            if (!seenImeis.add(imei2)) {
                record.addError("IMEI 2 bị trùng trong file Excel");
            } else if (mayDienThoaiRepository.existsByImei1(imei2) || mayDienThoaiRepository.existsByImei2(imei2)) {
                record.addError("IMEI 2 đã tồn tại trong hệ thống");
            }
        }
    }
}
