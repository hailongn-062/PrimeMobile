package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO đầu vào cho chức năng Tạo Phiếu Nhập Kho.
 * <p>
 * Nhân viên / Admin điền thông tin lô hàng nhận từ Nhà Cung Cấp.
 * Kho đích BẮT BUỘC phải là Kho Tổng (loai = 'kho_tong').
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaoPhieuNhapKhoRequest {

    /** ID kho nhận hàng — PHẢI là Kho Tổng. */
    private Integer khoId;

    /** ID nhà cung cấp (nullable — cho phép nhập không rõ NCC). */
    private Integer nhaCungCapId;

    /** Ghi chú nội bộ về lô hàng. */
    private String ghiChu;

    /** Danh sách các dòng chi tiết nhập hàng. Không được rỗng. */
    private List<ChiTietNhapRequest> chiTiets;

    // -------------------------------------------------------------------------
    // Inner DTO: Chi tiết 1 dòng nhập kho
    // -------------------------------------------------------------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChiTietNhapRequest {

        /** ID biến thể sản phẩm (SKU) nhập vào. */
        private Integer bienTheSanPhamId;

        /** Số lượng nhập. Phải > 0. */
        private Integer soLuong;

        /** Đơn giá nhập từ NCC. Phải >= 0. */
        private BigDecimal donGiaNhap;
    }
}
