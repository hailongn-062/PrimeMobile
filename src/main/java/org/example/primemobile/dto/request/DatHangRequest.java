package org.example.primemobile.dto.request;

import java.math.BigDecimal;

/**
 * DTO nhận request Đặt Hàng Online từ phía Frontend.
 *
 * <h3>Hỗ trợ 2 loại địa chỉ giao hàng:</h3>
 * <ul>
 *   <li><b>Địa chỉ đã lưu:</b> Truyền {@code diaChiGiaoId} — Service tự snapshot từ entity.</li>
 *   <li><b>Địa chỉ mới:</b> Truyền các field text thủ công (hoTenNguoiNhan, sdtNguoiNhan, ...).</li>
 * </ul>
 * Nếu cả 2 đều được gửi lên, {@code diaChiGiaoId} sẽ được ưu tiên.
 * <p>
 * Ít nhất 1 trong 2 ({@code khachHangId} hoặc {@code sessionId}) phải khác null.
 */
public record DatHangRequest(

        // ───────────────────────────────────────────────────────────────────
        // ĐỊNH DANH NGƯỜI DÙNG
        // ───────────────────────────────────────────────────────────────────

        /** ID khách hàng đã đăng nhập. NULL nếu là khách vãng lai. */
        Integer khachHangId,

        /** Session ID của khách vãng lai. NULL nếu khách đã đăng nhập. */
        String sessionId,

        // ───────────────────────────────────────────────────────────────────
        // ĐỊA CHỈ GIAO HÀNG — Lựa chọn 1: Dùng địa chỉ sổ địa chỉ đã lưu
        // ───────────────────────────────────────────────────────────────────

        /**
         * ID địa chỉ trong bảng dia_chi_khach_hang (nullable).
         * Service sẽ snapshot tên/sdt/địa chỉ từ entity này vào DonHang.
         */
        Integer diaChiGiaoId,

        // ───────────────────────────────────────────────────────────────────
        // ĐỊA CHỈ GIAO HÀNG — Lựa chọn 2: Nhập tay địa chỉ mới
        // ───────────────────────────────────────────────────────────────────

        /** Họ tên người nhận hàng — snapshot vào don_hang.ho_ten_nguoi_nhan. */
        String hoTenNguoiNhan,

        /** Số điện thoại người nhận — snapshot vào don_hang.sdt_nguoi_nhan. */
        String sdtNguoiNhan,

        /** Địa chỉ cụ thể (số nhà, tên đường) — snapshot vào don_hang.dia_chi_giao_cu_the. */
        String diaChiGiaoCuThe,

        /** Tên tỉnh/thành phố — snapshot vào don_hang.tinh_thanh_giao. */
        String tinhThanhGiao,

        /** Tên quận/huyện — snapshot vào don_hang.quan_huyen_giao. */
        String quanHuyenGiao,

        /** Tên phường/xã — snapshot vào don_hang.phuong_xa_giao. */
        String phuongXaGiao,

        // ───────────────────────────────────────────────────────────────────
        // THANH TOÁN & PHÍ
        // ───────────────────────────────────────────────────────────────────

        /**
         * ID phương thức thanh toán (NOT NULL).
         * Quyết định trạng thái thanh toán ban đầu của đơn:
         *   VNPay → "dang_chuyen_huong" + thoiGianHetHanTt = now + 15 phút.
         *   COD/khác → "chua_thanh_toan".
         */
        Integer phuongThucThanhToanId,



        /**
         * Phí vận chuyển (từ GHN API hoặc frontend tính).
         * Mặc định 0 nếu miễn phí hoặc API lỗi (system_rules.md §6).
         */
        BigDecimal phiShip,

        /** Ghi chú của khách (nullable). Ví dụ: "Gọi trước khi giao". */
        String ghiChu

) {
    /**
     * Compact constructor — chuẩn hoá phiShip về ZERO nếu client gửi null.
     */
    public DatHangRequest {
        if (phiShip == null) {
            phiShip = BigDecimal.ZERO;
        }
    }
}
