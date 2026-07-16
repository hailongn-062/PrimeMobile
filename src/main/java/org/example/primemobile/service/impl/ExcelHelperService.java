package org.example.primemobile.service.impl;

import org.apache.poi.ss.usermodel.*;
import org.example.primemobile.dto.kho.ImeiImportRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExcelHelperService {

    public List<ImeiImportRecord> parseImeiExcel(MultipartFile file) throws Exception {
        List<ImeiImportRecord> records = new ArrayList<>();
        
        try (InputStream is = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(is)) {
             
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên
            
            // Bỏ qua dòng header (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                String sku = getCellValue(row.getCell(0));
                String imei1 = getCellValue(row.getCell(1));
                String imei2 = getCellValue(row.getCell(2));
                
                // Bỏ qua nếu cả 3 cột đều rỗng
                if (sku.isEmpty() && imei1.isEmpty() && imei2.isEmpty()) {
                    continue;
                }
                
                ImeiImportRecord record = ImeiImportRecord.builder()
                        .sku(sku)
                        .imei1(imei1)
                        .imei2(imei2)
                        .rowIndex(i + 1)
                        .errors(new ArrayList<>())
                        .build();
                        
                records.add(record);
            }
        }
        return records;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) return "";
        
        if (cell.getCellType() == CellType.NUMERIC) {
            double num = cell.getNumericCellValue();
            long longVal = (long) num;
            if (num == longVal) {
                return String.valueOf(longVal);
            }
            return java.math.BigDecimal.valueOf(num).toPlainString();
        } else if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }
}
