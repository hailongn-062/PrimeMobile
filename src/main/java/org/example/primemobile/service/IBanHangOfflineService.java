package org.example.primemobile.service;

import org.example.primemobile.entity.DonHang;

/**
 * Contract cho phân hệ Bán hàng Offline (tại quầy).
 * <p>
 * Áp dụng cho: {@code NhanVien} hoặc {@code Admin} thao tác trực tiếp tại cửa hàng.
 * <p>
 * Luồng nghiệp vụ đầy đủ (system_rules.md §2.1):
 * <pre>
 *   1. taoDonHangMoi(nhanVienId)               → Tạo đơn nháp "cho_thanh_toan"
 *   2. themSanPhamVaoDon(donId, btspId, qty)   → Thêm / cộng dồn SKU vào đơn
 *   3. thanhToanDonHang(donId, ptttId)         → Chốt đơn, lưu thanh toán, trừ kho
 * </pre>
 * <p>
 * Ràng buộc Safety Stock (system_rules.md §3.1):
 * Mỗi lần thêm sản phẩm, hệ thống kiểm tra tồn kho Kho Tổng sau khi trừ
 * phải còn {@code >= 5} đơn vị. Vi phạm → ném {@link IllegalArgumentException}.
 */
public interface IBanHangOfflineService {

    /**
     * Tạo một đơn hàng offline mới ở trạng thái nháp.
     * <p>
     * Đơn hàng được gắn với nhân viên tạo và khách lẻ mặc định
     * (so_dien_thoai = '0000000000') nếu khách không muốn để lại thông tin.
     * Nhân viên sẽ chọn / cập nhật thông tin khách sau nếu cần.
     *
     * @param nhanVienId ID của nhân viên/admin đang thao tác tại quầy.
     * @return Đơn hàng mới được tạo với {@code trang_thai = "cho_thanh_toan"}.
     * @throws jakarta.persistence.EntityNotFoundException nếu nhanVienId không tồn tại.
     */
    DonHang taoDonHangMoi(Integer nhanVienId);

    /**
     * Thêm sản phẩm vào đơn hàng đang nháp (hoặc cộng dồn số lượng nếu SKU đã có).
     * <p>
     * Ràng buộc bắt buộc trước khi thêm:
     * <ol>
     *   <li>Đơn hàng phải tồn tại và đang ở trạng thái {@code "cho_thanh_toan"}.</li>
     *   <li>Kiểm tra Safety Stock tại Kho Tổng:
     *       {@code (tồn kho hiện tại - soLuong) >= 5} — vi phạm → ném ngoại lệ.</li>
     * </ol>
     * Sau khi pass validation, tính lại {@code tong_tien_hang} của đơn hàng.
     *
     * @param donHangId          ID đơn hàng đang nháp.
     * @param bienTheSanPhamId   ID biến thể SKU cần thêm.
     * @param soLuong            Số lượng cần thêm (phải > 0).
     * @return Đơn hàng đã được cập nhật.
     * @throws IllegalArgumentException    Vi phạm Safety Stock hoặc soLuong <= 0.
     * @throws IllegalStateException       Đơn hàng không ở trạng thái "cho_thanh_toan".
     * @throws jakarta.persistence.EntityNotFoundException Nếu đơn hàng hoặc biến thể không tồn tại.
     */
    DonHang themSanPhamVaoDon(Integer donHangId, Integer bienTheSanPhamId, int soLuong);

    /**
     * Hoàn tất thanh toán đơn hàng offline.
     * <p>
     * Thao tác theo thứ tự trong 1 transaction:
     * <ol>
     *   <li>Cập nhật {@code trang_thai} đơn hàng → {@code "da_giao"} (giao ngay tại quầy).</li>
     *   <li>Cập nhật {@code trang_thai_thanh_toan} → {@code "da_thanh_toan"}.</li>
     *   <li>Tạo bản ghi {@link org.example.primemobile.entity.ThanhToan} với
     *       {@code trang_thai = "thanh_cong"}, {@code so_tien = tong_thanh_toan}.</li>
     *   <li>Duyệt từng {@code ChiTietDonHang} → trừ trực tiếp vào {@code ton_kho} Kho Tổng.</li>
     * </ol>
     *
     * @param donHangId              ID đơn hàng cần thanh toán.
     * @param phuongThucThanhToanId  ID phương thức thanh toán (COD là mặc định).
     * @return Đơn hàng đã được cập nhật trạng thái hoàn tất.
     * @throws IllegalStateException       Nếu đơn hàng không ở trạng thái "cho_thanh_toan".
     * @throws IllegalArgumentException    Nếu tồn kho tại Kho Tổng không đủ khi trừ thực tế.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy đơn, PTTT, hoặc tồn kho.
     */
    DonHang thanhToanDonHang(Integer donHangId, Integer phuongThucThanhToanId);
}
