package org.example.primemobile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO phản hồi cho yêu cầu tạo thanh toán VNPay.
 * <p>
 * Trả về cho frontend sau khi khách hàng chọn phương thức thanh toán VNPay
 * trong luồng đặt hàng. Frontend sẽ sử dụng {@code paymentUrl} để redirect
 * trình duyệt sang trang thanh toán của VNPay.
 *
 * <h3>Luồng sử dụng:</h3>
 * <ol>
 *   <li>Khách hàng chọn VNPay và bấm "Đặt hàng".</li>
 *   <li>Backend tạo đơn hàng, tạo URL thanh toán và trả về DTO này.</li>
 *   <li>Frontend nhận được paymentUrl và thực hiện redirect.</li>
 *   <li>Khách hàng thanh toán trên VNPay, sau đó được redirect về Return URL.</li>
 * </ol>
 *
 * @see org.example.primemobile.service.IVnPayService#createPaymentUrl(org.example.primemobile.entity.DonHang, String)
 * @see org.example.primemobile.controller.DatHangOnlineController
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VnPayPaymentResponse {

    /**
     * URL thanh toán VNPay (bao gồm chữ ký và tất cả tham số).
     * <p>
     * Frontend sử dụng URL này để redirect trình duyệt:
     * <pre>
     *   window.location.href = response.paymentUrl;
     * </pre>
     */
    private String paymentUrl;

    /**
     * Mã đơn hàng (hiển thị cho khách hàng).
     * <p>
     * Ví dụ: "DHO-20260702-629428"
     */
    private String maDonHang;

    /**
     * ID đơn hàng trong database.
     * <p>
     * Dùng để tra cứu hoặc debug.
     */
    private Integer donHangId;
}