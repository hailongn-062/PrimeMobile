package org.example.primemobile.dto.baohanh;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaoYeuCauBaoHanhRequest {
    
    private String imei;
    
    private String moTaLoi;
    
    private Integer trungTamBaoHanhId;
    
    private String ghiChu;
}
