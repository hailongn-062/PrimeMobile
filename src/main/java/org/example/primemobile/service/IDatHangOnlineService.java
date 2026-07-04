package org.example.primemobile.service;

import org.example.primemobile.dto.VnPayPaymentResponse;
import org.example.primemobile.dto.request.DatHangRequest;
import org.example.primemobile.entity.DonHang;

/**
 * Hợp đồng (interface) cho phân hệ Đặt Hàng Online (Checkout).
 * <p>
 * Phân hệ xử lý toàn bộ luồng: Giỏ hàng → Đơn hàng chính thức,
 * bao gồm kiểm tra kho, áp mã giảm giá, tạo bản ghi thanh toán và dọn giỏ hàng.
 *
 * <h3>Luật kho bắt buộc:</h3>
 * <ul>
 *   <li>Kiểm tra {@code kho_online} tại thời điểm checkout → ném lỗi nếu không đủ.</li>
 *   <li><b>KHÔNG trừ kho lúc đặt hàng.</b> Kho bị trừ khi nhân viên <b>xác nhận đơn</b>.</li>
 * </ul>
 *
 * @see org.example.primemobile.service.impl.DatHangOnlineServiceImpl
 */
public interface IDatHangOnlineService {

    /**
     * Thực hiện toàn bộ luồng checkout trong 1 {@code @Transactional}.
     *
     * <h3>Các bước xử lý:</h3>
     * <ol>
     *   <li><b>Lấy giỏ hàng</b> theo khachHangId / sessionId — ném lỗi nếu giỏ trống.</li>
     *   <li><b>Kiểm tra kho_online</b> cho từng SKU — ném {@link IllegalArgumentException} nếu hết hàng.</li>
     *   <li><b>Lưu DonHang</b>: kenh_ban="online", trang_thai="cho_xac_nhan", snapshot địa chỉ.</li>
     *   <li><b>Lưu ChiTietDonHang</b>: price snapshot. <b>Tuyệt đối KHÔNG trừ kho.</b></li>
     *   <li><b>Tạo ThanhToan</b>:
     *       VNPay → trangThaiThanhToan="dang_chuyen_huong" + thoiGianHetHanTt=now+15p;
     *       COD → trangThaiThanhToan="chua_thanh_toan".</li>
     *   <li><b>Dọn giỏ hàng</b>: Xóa toàn bộ ChiTietGioHang sau khi chốt đơn.</li>
     * </ol>
     *
     * @param request DTO chứa thông tin đặt hàng từ frontend.
     * @return {@link DonHang} đã được persist (chưa có tong_thanh_toan vì là computed column).
     * @throws IllegalArgumentException             nếu giỏ trống, kho không đủ, mã giảm giá không hợp lệ.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy entity liên quan.
     */
    DonHang taoDonHang(DatHangRequest request);

    /**
     * Tạo đơn hàng và URL thanh toán VNPay (dành cho phương thức thanh toán VNPay).
     * <p>
     * Phương thức này tương tự {@link #taoDonHang(DatHangRequest)} nhưng thay vì trả về
     * {@link DonHang}, nó trả về {@link VnPayPaymentResponse} chứa URL thanh toán.
     * <p>
     * Luồng xử lý khác biệt:
     * <ul>
     *   <li>Tạo đơn hàng với {@code trangThaiThanhToan = "dang_chuyen_huong"}.</li>
     *   <li>Thiết lập {@code thoiGianHetHanTt = now + 15 phút} (theo quy định của VNPay).</li>
     *   <li>Tạo bản ghi {@code ThanhToan} với {@code trangThai = "cho"}.</li>
     *   <li>Gọi {@link IVnPayService#createPaymentUrl(DonHang, String)} để tạo URL.</li>
     *   <li>Trả về DTO chứa URL để frontend redirect.</li>
     * </ul>
     *
     * @param request  DTO chứa thông tin đặt hàng từ frontend.
     * @param clientIp Địa chỉ IP của khách hàng (lấy từ HttpServletRequest).
     * @return {@link VnPayPaymentResponse} chứa URL thanh toán và thông tin đơn hàng.
     * @throws IllegalArgumentException             nếu giỏ trống, kho không đủ, mã giảm giá không hợp lệ.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy entity liên quan.
     */
    VnPayPaymentResponse taoDonHangVnPay(DatHangRequest request, String clientIp);
}