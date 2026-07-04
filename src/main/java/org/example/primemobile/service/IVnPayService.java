package org.example.primemobile.service;

import org.example.primemobile.entity.DonHang;

import java.util.Map;

/**
 * Service Interface cho tích hợp thanh toán VNPay.
 * <p>
 * Cung cấp các chức năng:
 * <ul>
 *   <li>Tạo URL thanh toán để chuyển hướng khách hàng sang VNPay.</li>
 *   <li>Xử lý IPN (Instant Payment Notification) từ VNPay - cập nhật trạng thái giao dịch.</li>
 *   <li>Xử lý return URL (khách hàng được redirect về sau khi thanh toán).</li>
 * </ul>
 *
 * @see org.example.primemobile.service.impl.VnPayServiceImpl
 */
public interface IVnPayService {

    /**
     * Tạo URL thanh toán VNPay cho một đơn hàng.
     * <p>
     * Phương thức này sẽ tạo các tham số cần thiết theo yêu cầu của VNPay,
     * bao gồm mã giao dịch, số tiền, thông tin đơn hàng, IP client,
     * sau đó xây dựng URL với chữ ký bảo mật.
     * <p>
     * Sau khi gọi phương thức này, frontend sẽ redirect khách hàng đến URL trả về.
     *
     * @param donHang  Đơn hàng cần thanh toán (đã được lưu trong DB, có ID và maDonHang).
     * @param clientIp Địa chỉ IP của khách hàng (lấy từ HttpServletRequest).
     * @return URL thanh toán VNPay (bao gồm cả chữ ký).
     */
    String createPaymentUrl(DonHang donHang, String clientIp);

    /**
     * Xử lý callback IPN từ VNPay.
     * <p>
     * VNPay gọi endpoint này (server-to-server) ngay sau khi giao dịch hoàn tất
     * để thông báo kết quả. Phương thức này sẽ:
     * <ol>
     *   <li>Xác thực chữ ký từ VNPay.</li>
     *   <li>Tìm giao dịch theo mã tham chiếu.</li>
     *   <li>Cập nhật trạng thái thanh toán cho đơn hàng và bản ghi thanh toán.</li>
     * </ol>
     * <p>
     * <b>Quan trọng:</b> Phương thức này phải trả về phản hồi đúng định dạng JSON
     * theo yêu cầu của VNPay (ví dụ: {"RspCode":"00","Message":"Success"})
     * để VNPay không gửi lại IPN nhiều lần.
     *
     * @param params Map chứa tất cả tham số VNPay gửi về (đã được giải mã từ request).
     * @return Chuỗi JSON phản hồi cho VNPay (theo định dạng quy định).
     */
    String processIpn(Map<String, String> params);

    /**
     * Xử lý return callback từ VNPay (khi khách hàng được redirect về).
     * <p>
     * Sau khi khách hàng thanh toán trên VNPay, trình duyệt sẽ được chuyển hướng
     * về {@code vnpay.return-url} với các tham số kết quả. Phương thức này sẽ:
     * <ol>
     *   <li>Xác thực chữ ký.</li>
     *   <li>Cập nhật trạng thái đơn hàng và thanh toán (nếu IPN chưa kịp xử lý).</li>
     *   <li>Trả về đối tượng {@link DonHang} đã được cập nhật để controller chuyển hướng đến trang kết quả.</li>
     * </ol>
     * <p>
     * <b>Lưu ý:</b> IPN thường được gọi trước return, nhưng trong trường hợp IPN thất bại,
     * return sẽ đóng vai trò dự phòng để cập nhật trạng thái.
     *
     * @param params Map chứa tất cả tham số từ VNPay.
     * @return {@link DonHang} đã được cập nhật trạng thái thanh toán.
     * @throws IllegalArgumentException nếu chữ ký không hợp lệ hoặc không tìm thấy giao dịch.
     */
    DonHang processReturn(Map<String, String> params);
}