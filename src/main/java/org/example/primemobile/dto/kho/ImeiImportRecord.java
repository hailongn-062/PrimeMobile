package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImeiImportRecord {
    private String sku;
    private String imei1;
    private String imei2;
    private int rowIndex;
    
    // Lưu các lỗi validate cụ thể của dòng này (ví dụ "Trùng IMEI 1", "Sai định dạng")
    private List<String> errors;

    public void addError(String error) {
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(error);
    }
}
