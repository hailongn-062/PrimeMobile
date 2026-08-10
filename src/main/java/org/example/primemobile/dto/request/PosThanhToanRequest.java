package org.example.primemobile.dto.request;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload nhận từ màn hình POS khi nhân viên bấm "Thanh toán".
 *
 * <p>Cấu trúc JSON mẫu:
 * <pre>{@code
 * {
 *   "khachHangId": 5,          // null → fallback Khách lẻ '0000000000'
 *   "tongTien"   : 15000000,
 *   "tienGiam"   : 750000,
 *   "ctkmId"     : 3,          // ID CTKM áp dụng, null nếu không có
 *   "chiTiets"   : [
 *     {
 *       "bienTheId" : 12,
 *       "soLuong"   : 2,
 *       "donGia"    : 7500000,
 *       "imeis"     : ["356938035643809", "356938035643810"]
 *     }
 *   ]
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
public class PosThanhToanRequest {

    /** ID đơn hàng chờ (nếu đang tiếp tục từ một đơn chờ trước đó). */
    private Integer donHangId;

    /**
     * ID khách hàng (có thể null → hệ thống tự gán Khách lẻ mặc định).
     */
    private Integer khachHangId;

    /** Tổng tiền hàng (chưa giảm giá). */
    private BigDecimal tongTien;

    /** Số tiền được giảm (0 nếu không có khuyến mãi). */
    private BigDecimal tienGiam;

    /** ID chương trình khuyến mãi áp dụng (null nếu không có). */
    private Integer ctkmId;

    /** ID phương thức thanh toán. */
    private Integer phuongThucThanhToanId;

    /** Danh sách chi tiết từng dòng hàng. */
    private List<ChiTietPosRequest> chiTiets;

    // ─────────────────────────────────────────────────────────────
    // Inner class: Chi tiết 1 dòng hàng trong đơn POS
    // ─────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    public static class ChiTietPosRequest {

        /** ID biến thể sản phẩm (SKU). */
        private Integer bienTheId;

        /** Số lượng mua. */
        private Integer soLuong;

        /** Đơn giá bán tại thời điểm POS (price snapshot). */
        private BigDecimal donGia;

        /**
         * Danh sách mã IMEI (imei1) tương ứng với số lượng mua.
         * Số phần tử PHẢI bằng {@code soLuong}. Validate ở cả JS và Service.
         */
        private List<String> imeis;
    }
}
