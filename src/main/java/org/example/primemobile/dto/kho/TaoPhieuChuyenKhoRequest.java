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
 * <p>
 * Hỗ trợ chọn IMEI cụ thể khi chuyển kho:
 * <ul>
 *   <li>Nếu {@code imeiList} không rỗng, hệ thống sẽ cập nhật kho_id của các IMEI đó sang kho đích.</li>
 *   <li>Số lượng IMEI phải bằng {@code soLuong}.</li>
 *   <li>Nếu {@code imeiList} rỗng hoặc null, chỉ cập nhật số lượng tồn kho (không cập nhật IMEI).</li>
 * </ul>
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

        /**
         * Danh sách IMEI cụ thể cần chuyển (tùy chọn).
         * <p>
         * Nếu có giá trị, số lượng IMEI phải bằng {@code soLuong}.
         * Hệ thống sẽ kiểm tra từng IMEI có tinh_trang = 'trong_kho' và thuộc kho nguồn.
         * Sau đó cập nhật kho_id của các IMEI này sang kho đích.
         * <p>
         * Nếu null hoặc rỗng, chỉ chuyển số lượng (không cập nhật IMEI).
         */
        private List<String> imeiList;
    }
}