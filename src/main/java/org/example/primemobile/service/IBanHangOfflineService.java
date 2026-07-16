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
     *   <li>Cập nhật {@code trang_thai} đơn hàng → {@code "da_hoan_thanh"} (giao ngay tại quầy).</li>
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

    /**
     * Cập nhật / chốt khách hàng cho đơn hàng POS.
     *
     * <p>Logic nghiệp vụ (system_rules.md §2.1):
     * <ul>
     *   <li><b>Trường hợp 1 — Khách có tài khoản:</b>
     *       {@code khachHangId} truyền lên hợp lệ → Lấy thông tin khách đó gán vào DonHang.</li>
     *   <li><b>Trường hợp 2 — Khách lẻ vãng lai:</b>
     *       {@code khachHangId} là {@code null} hoặc không tìm thấy trong DB →
     *       Tự động gán vào tài khoản khách lẻ mặc định ({@code so_dien_thoai = '0000000000'}).</li>
     * </ul>
     *
     * @param donHangId   ID đơn hàng đang nháp cần cập nhật khách hàng.
     * @param khachHangId ID khách hàng (có thể null — Khách lẻ).
     * @return Đơn hàng đã được gán khách hàng.
     * @throws jakarta.persistence.EntityNotFoundException Nếu đơn hàng không tồn tại
     *         hoặc tài khoản khách lẻ mặc định chưa được khởi tạo trong DB.
     * @throws IllegalStateException Nếu đơn hàng không ở trạng thái có thể sửa.
     */
    DonHang capNhatKhachHangChoDon(Integer donHangId, Integer khachHangId);

    /**
     * Lưu đơn hàng hiện tại thành đơn chờ (trạng thái don_hang_cho).
     * Nếu donHangId null → tạo mới đơn chờ (dùng dữ liệu giỏ hàng hiện tại).
     * Nếu donHangId != null → cập nhật đơn chờ đã tồn tại.
     * Khi lưu: IMEI được đổi sang da_ban ngay lập tức (khóa IMEI).
     *
     * @param donHangId  ID đơn hàng cần lưu (có thể null)
     * @param nhanVienId ID nhân viên tạo đơn (nếu tạo mới)
     * @param payload    Dữ liệu giỏ hàng, khách hàng, khuyến mãi
     * @return DonHang đã được lưu với trạng thái don_hang_cho
     */
    DonHang luuDonHangCho(Integer donHangId, Integer nhanVienId, org.example.primemobile.dto.request.PosThanhToanRequest payload);

    /**
     * Lấy danh sách tất cả đơn hàng chờ (kênh tai_quay, trạng thái don_hang_cho).
     * Sắp xếp mới nhất lên đầu.
     */
    java.util.List<DonHang> layDanhSachDonHangCho();

    /**
     * Hủy đơn hàng chờ.
     * - Đổi trạng thái don_hang_cho → da_huy
     * - Trạng thái thanh toán → chua_thanh_toan
     * - Ghi lý do hủy vào ghiChu
     * - Hoàn trả IMEI về trạng thái trong_kho (vì đã khóa trước đó)
     *
     * @param donHangId ID đơn chờ cần hủy
     * @param lyDoHuy   Lý do hủy
     * @return DonHang sau khi hủy
     */
    DonHang huyDonHangCho(Integer donHangId, String lyDoHuy);

    /**
     * Tiếp tục đơn hàng chờ.
     * - Đổi trạng thái don_hang_cho → cho_xac_nhan
     * - Load lại dữ liệu để nhân viên chỉnh sửa và thanh toán.
     *
     * @param donHangId ID đơn chờ cần tiếp tục
     * @return DonHang đã chuyển về cho_xac_nhan
     */
    DonHang tiepTucDonHangCho(Integer donHangId);

    /**
     * Giữ (lock) 1 IMEI khi nhân viên chọn vào giỏ hàng.
     */
    void giuImei(String imei, Integer nhanVienId);

    /**
     * Nhả (unlock) 1 IMEI khỏi giỏ hàng.
     */
    void nhaImei(String imei, Integer nhanVienId);

    /**
     * Nhả toàn bộ IMEI mà nhân viên đang giữ (khi đóng trình duyệt/hủy toàn bộ).
     */
    void nhaTatCaImeiCuaNhanVien(Integer nhanVienId);
}
