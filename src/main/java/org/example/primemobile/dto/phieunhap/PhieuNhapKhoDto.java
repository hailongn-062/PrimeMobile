package org.example.primemobile.dto.phieunhap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO cho phiếu nhập kho (không chứa Hibernate proxy).
 * Dùng để trả về danh sách lịch sử phiếu nhập.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhieuNhapKhoDto {

    /** ID của phiếu nhập */
    private Integer id;

    /** Mã phiếu nhập (ma_phieu) */
    private String maPhieu;

    /** Tên kho nhận */
    private String tenKho;

    /** Tên nhà cung cấp (nếu có) */
    private String tenNhaCungCap;

    /** Ngày nhập kho */
    private LocalDateTime ngayNhap;

    /** Tổng tiền của phiếu nhập */
    private BigDecimal tongTien;

    /** Ghi chú (nếu có) */
    private String ghiChu;

    /** Danh sách chi tiết phiếu nhập (chỉ dùng cho màn hình chi tiết) */
    private List<ChiTietPhieuNhapDto> chiTiets;
}