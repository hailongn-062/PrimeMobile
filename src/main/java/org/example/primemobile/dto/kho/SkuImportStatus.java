package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuImportStatus {
    private String sku;
    private int orderedQuantity;   // Số lượng trong phiếu nhập
    private int excelQuantity;     // Số lượng IMEI trong file Excel
    
    // Trạng thái hiển thị: "Hợp lệ", "Thiếu X", "Dư Y", "Sai SKU", "Lỗi IMEI"
    private String statusText;     
    
    // true nếu dòng này hoàn toàn hợp lệ hoặc chấp nhận được (nhập thiếu nhưng IMEI đúng định dạng)
    private boolean isAcceptable;  
    
    // Danh sách các bản ghi IMEI hợp lệ
    private List<ImeiImportRecord> validRecords;
    
    // Danh sách các bản ghi IMEI bị lỗi của SKU này
    private List<ImeiImportRecord> errorRecords;
}
