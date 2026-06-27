package org.example.primemobile.dto.phieunhap;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO cho chi tiết phiếu nhập kho (không chứa Hibernate proxy).
 * Dùng để trả về danh sách chi tiết của 1 phiếu nhập.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChiTietPhieuNhapDto {

    /** Số thứ tự dòng (tính từ 1) */
    private Integer stt;

    /** ID của chi tiết phiếu nhập (nếu cần) */
    private Integer id;

    /** Mã SKU của biến thể sản phẩm */
    private String maSku;

    /** Tên sản phẩm (lấy từ SanPham.tenSanPham) */
    private String tenSanPham;

    /** Số lượng nhập */
    private Integer soLuong;

    /** Đơn giá nhập */
    private BigDecimal donGiaNhap;

    /** Thành tiền = soLuong * donGiaNhap */
    private BigDecimal thanhTien;
}