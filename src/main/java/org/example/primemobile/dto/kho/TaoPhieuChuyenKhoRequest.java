package org.example.primemobile.dto.kho;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO đầu vào cho chức năng Tạo Phiếu Chuyển Kho.
 * <p>
 * Nhân viên / Admin khởi tạo lệnh chuyển hàng giữa 2 kho.
 * Quy trình thường dùng: Kho Tổng → Kho Online.
 * <p>
 * ⚠️ Ràng buộc nghiệp vụ: khoNguonId PHẢI khác khoDichId.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaoPhieuChuyenKhoRequest {

    /** ID kho xuất hàng (nguồn). */
    private Integer khoNguonId;

    /** ID kho nhận hàng (đích). */
    private Integer khoDichId;

    /** Lý do chuyển kho (bổ sung hàng online, hoàn kho, kiểm kê...). */
    private String lyDo;

    /** Danh sách các dòng chi tiết chuyển kho. Không được rỗng. */
    private List<ChiTietChuyenRequest> chiTiets;

    // -------------------------------------------------------------------------
    // Inner DTO: Chi tiết 1 dòng chuyển kho
    // -------------------------------------------------------------------------

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChiTietChuyenRequest {

        /** ID biến thể sản phẩm (SKU) cần chuyển. */
        private Integer bienTheSanPhamId;

        /** Số lượng cần chuyển. Phải > 0. */
        private Integer soLuong;
    }
}
