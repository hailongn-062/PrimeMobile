package org.example.primemobile.service;

import org.example.primemobile.entity.DonHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface định nghĩa hợp đồng cho phân hệ Quản lý Đơn Hàng (dành cho Nhân viên / Admin).
 *
 * <h2>Các chức năng:</h2>
 * <ol>
 *   <li>Xem danh sách đơn hàng có phân trang và bộ lọc.</li>
 *   <li>Xem chi tiết 1 đơn hàng kèm danh sách sản phẩm.</li>
 *   <li>Xác nhận đơn hàng: kiểm tra Safety Stock §3.1 → trừ kho_online → chuyển trạng thái.</li>
 *   <li>Cập nhật lộ trình giao hàng: da_xac_nhan → dang_giao → da_giao.</li>
 *   <li>Hủy đơn hàng kèm hoàn kho nếu kho đã bị trừ trước đó (§2.2.7).</li>
 * </ol>
 *
 * @see org.example.primemobile.service.impl.QuanLyDonHangServiceImpl
 */
public interface IQuanLyDonHangService {

    /**
     * Lấy danh sách đơn hàng có phân trang, hỗ trợ lọc nhiều tiêu chí.
     *
     * @param trangThai   Lọc theo trạng thái đơn (NULL = tất cả).
     *                    Giá trị hợp lệ: "cho_xac_nhan" | "da_xac_nhan" | "dang_giao" | "da_giao" | "da_huy"
     * @param maDonHang   Tìm LIKE theo mã đơn hàng (NULL = bỏ qua).
     * @param soDienThoai Tìm LIKE theo SĐT khách hàng (NULL = bỏ qua).
     * @param pageable    Thông tin phân trang và sắp xếp.
     * @return Trang kết quả {@link DonHang}.
     */
    Page<DonHang> layDanhSachDonHang(String trangThai, String maDonHang,
                                      String soDienThoai, Pageable pageable);

    /**
     * Lấy chi tiết 1 đơn hàng kèm eager-load danh sách sản phẩm bên trong.
     *
     * @param donHangId ID đơn hàng cần xem chi tiết.
     * @return {@link DonHang} đã được load đầy đủ ChiTietDonHang + BienTheSanPham.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    DonHang layChiTietDonHang(Integer donHangId);

    /**
     * Xác nhận đơn hàng online (Bước nghiệp vụ quan trọng nhất).
     *
     * <h3>Logic:</h3>
     * <ol>
     *   <li>Kiểm tra trạng thái phải là {@code "cho_xac_nhan"}.</li>
     *   <li>Fail-Fast: Kiểm tra toàn bộ SKU xem {@code kho_online} có đủ không.
     *       Áp dụng Safety Stock Rule §3.1: tồn kho sau khi trừ KHÔNG được &lt; 5.</li>
     *   <li>Trừ thực tế vào {@code ton_kho} của {@code kho_online}.</li>
     *   <li>Chuyển trạng thái đơn → {@code "da_xac_nhan"}.</li>
     * </ol>
     *
     * @param donHangId   ID đơn hàng cần xác nhận.
     * @param nhanVienId  ID nhân viên thực hiện xác nhận (ghi vào nguoiXuLy).
     * @return {@link DonHang} sau khi xác nhận.
     * @throws IllegalArgumentException      nếu đơn sai trạng thái hoặc vi phạm Safety Stock.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hoặc kho không tồn tại.
     */
    DonHang xacNhanDonHang(Integer donHangId, Integer nhanVienId);

    /**
     * Cập nhật lộ trình giao hàng theo luồng: da_xac_nhan → dang_giao → da_giao.
     *
     * <h3>Lưu ý (system_rules.md §7.4):</h3>
     * Tạm hoãn logic cộng điểm thưởng và cộng tong_chi_tieu.
     * Không viết code xử lý điểm ở đây cho đến khi có lệnh mới.
     *
     * @param donHangId ID đơn hàng cần cập nhật trạng thái.
     * @param trangThaiMoi Trạng thái mới muốn chuyển sang ("dang_giao" hoặc "da_giao").
     * @return {@link DonHang} sau khi cập nhật.
     * @throws IllegalArgumentException nếu chuyển trạng thái không hợp lệ theo luồng.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    DonHang capNhatTrangThai(Integer donHangId, String trangThaiMoi);

    /**
     * Hủy đơn hàng kèm hoàn kho nếu cần (system_rules.md §2.2.7).
     *
     * <h3>Logic hoàn kho:</h3>
     * <ul>
     *   <li>{@code cho_xac_nhan}: Kho chưa bị trừ → chỉ đổi trạng thái, KHÔNG hoàn kho.</li>
     *   <li>{@code da_xac_nhan} hoặc {@code dang_giao}: Kho ĐÃ bị trừ trước đó
     *       → BẮT BUỘC cộng hoàn lại số lượng vào {@code kho_online}.</li>
     * </ul>
     *
     * @param donHangId  ID đơn hàng cần hủy.
     * @param lyDoHuy    Lý do hủy đơn (ghi vào ghiChu).
     * @return {@link DonHang} sau khi hủy.
     * @throws IllegalArgumentException nếu đơn đã ở trạng thái {@code "da_giao"} hoặc
     *                                   {@code "da_huy"} (không thể hủy).
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    DonHang huyDonHang(Integer donHangId, String lyDoHuy);
}
