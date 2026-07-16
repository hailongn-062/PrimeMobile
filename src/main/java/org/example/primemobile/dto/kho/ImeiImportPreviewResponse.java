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
public class ImeiImportPreviewResponse {
    private boolean isAllAcceptable; // Có thể submit được không (không có lỗi Dư, Sai SKU, Lỗi IMEI)
    private int totalOrdered;
    private int totalExcel;
    
    // Tóm tắt kết quả theo từng SKU
    private List<SkuImportStatus> skuStatuses;
}
