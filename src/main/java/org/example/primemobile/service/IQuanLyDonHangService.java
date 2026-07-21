package org.example.primemobile.service;

import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.MayDienThoai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa hợp đồng cho phân hệ Quản lý Đơn Hàng (dành cho Nhân viên / Admin).
 *
 * <h2>Các chức năng:</h2>
 * <ol>
 *   <li>Xem danh sách đơn hàng có phân trang và bộ lọc.</li>
 *   <li>Xem chi tiết 1 đơn hàng kèm danh sách sản phẩm.</li>
 *   <li>Xác nhận đơn hàng: kiểm tra Safety Stock §3.1 → trừ kho_tong → chuyển trạng thái.</li>
 *   <li>Cập nhật lộ trình giao hàng: da_xac_nhan → dang_giao → da_hoan_thanh.</li>
 *   <li>Hủy đơn hàng kèm hoàn kho nếu kho đã bị trừ trước đó (§2.2.7).</li>
 *   <li><b>Xác nhận đơn hàng có chọn IMEI</b> – chỉ cho phép chọn IMEI ở kho t?ng.</li>
 *   <li><b>Xác nhận thanh toán cho đơn hàng COD</b> – khi đơn đã giao và chưa thanh toán.</li>
 *   <li><b>Lấy danh sách IMEI đã gán cho đơn hàng</b> – phục vụ hiển thị chi tiết đơn.</li>
 * </ol>
 *
 * @see org.example.primemobile.service.impl.QuanLyDonHangServiceImpl
 */
public interface IQuanLyDonHangService {

    /**
     * Lấy danh sách đơn hàng có phân trang, hỗ trợ lọc nhiều tiêu chí.
     *
     * @param trangThai   Lọc theo trạng thái đơn (NULL = tất cả).
     *                    Giá trị hợp lệ: "cho_xac_nhan" | "da_xac_nhan" | "dang_giao" | "da_hoan_thanh" | "da_huy"
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
     *   <li>Fail-Fast: Kiểm tra toàn bộ SKU xem {@code kho_tong} có đủ không.
     *       Áp dụng Safety Stock Rule §3.1: tồn kho sau khi trừ KHÔNG được &lt; 5.</li>
     *   <li>Trừ thực tế vào {@code ton_kho} của {@code kho_tong}.</li>
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
     * Xác nhận đơn hàng online với danh sách IMEI được chọn.
     * <p>
     * Phương thức này được sử dụng thay cho {@link #xacNhanDonHang} khi nhân viên cần
     * chọn IMEI cụ thể cho từng sản phẩm trong đơn hàng.
     *
     * <h3>Logic:</h3>
     * <ol>
     *   <li>Kiểm tra trạng thái đơn phải là {@code "cho_xac_nhan"}.</li>
     *   <li>Với mỗi {@link ImeiSelection}:
     *     <ul>
     *       <li>Kiểm tra số lượng IMEI khớp với số lượng sản phẩm trong chi tiết đơn.</li>
     *       <li>Kiểm tra từng IMEI tồn tại, có {@code tinhTrang = 'trong_kho'} và {@code kho_id} = kho t?ng.</li>
     *       <li>Cập nhật IMEI: {@code tinhTrang = 'da_ban'}, gán {@code donHang}.</li>
     *     </ul>
     *   </li>
     *   <li>Trừ tồn kho kho t?ng cho từng SKU (kiểm tra Safety Stock).</li>
     *   <li>Chuyển trạng thái đơn → {@code "da_xac_nhan"}, gán nhân viên xử lý.</li>
     * </ol>
     *
     * @param donHangId        ID đơn hàng cần xác nhận.
     * @param nhanVienId       ID nhân viên thực hiện xác nhận.
     * @param imeiSelections   Danh sách lựa chọn IMEI cho từng chi tiết đơn hàng.
     * @return {@link DonHang} sau khi xác nhận.
     * @throws IllegalArgumentException      nếu số lượng IMEI không khớp, IMEI không hợp lệ,
     *                                       hoặc vi phạm Safety Stock.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn, chi tiết đơn, IMEI hoặc kho không tồn tại.
     */
    DonHang xacNhanDonHangVoiImei(Integer donHangId, Integer nhanVienId, List<ImeiSelection> imeiSelections);

    /**
     * Cập nhật lộ trình giao hàng theo luồng: da_xac_nhan → dang_giao → da_hoan_thanh.
     *
     * <h3>Lưu ý (system_rules.md §7.4):</h3>
     * Tạm hoãn logic cộng điểm thưởng và cộng tong_chi_tieu.
     * Không viết code xử lý điểm ở đây cho đến khi có lệnh mới.
     *
     * @param donHangId ID đơn hàng cần cập nhật trạng thái.
     * @param trangThaiMoi Trạng thái mới muốn chuyển sang ("dang_giao" hoặc "da_hoan_thanh").
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
     *       → BẮT BUỘC cộng hoàn lại số lượng vào {@code kho_tong}.</li>
     * </ul>
     *
     * @param donHangId  ID đơn hàng cần hủy.
     * @param lyDoHuy    Lý do hủy đơn (ghi vào ghiChu).
     * @return {@link DonHang} sau khi hủy.
     * @throws IllegalArgumentException nếu đơn đã ở trạng thái {@code "da_hoan_thanh"} hoặc
     *                                   {@code "da_huy"} (không thể hủy).
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    DonHang huyDonHang(Integer donHangId, String lyDoHuy);

    /**
     * Xác nhận đã hoàn tiền cho đơn hàng bị hủy (chuyển trạng thái từ cho_hoan_tien -> da_huy, da_hoan_tien).
     * @param donHangId ID của đơn hàng
     * @param idNhanVien ID nhân viên thao tác
     * @return Đối tượng DonHang đã cập nhật
     */
    DonHang xacNhanHoanTien(Integer donHangId, Integer idNhanVien);

    /**
     * Xác nhận đã thanh toán cho đơn hàng COD (Cash on Delivery).
     * <p>
     * Chỉ áp dụng khi đơn hàng đã ở trạng thái {@code "da_hoan_thanh"}
     * và trạng thái thanh toán {@code "chua_thanh_toan"}.
     * <p>
     * Sau khi xác nhận, trạng thái thanh toán sẽ được cập nhật thành {@code "da_thanh_toan"}.
     *
     * @param donHangId ID đơn hàng cần xác nhận thanh toán.
     * @return {@link DonHang} sau khi cập nhật.
     * @throws IllegalArgumentException nếu đơn không ở trạng thái {@code "da_hoan_thanh"}
     *                                  hoặc đã thanh toán.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    DonHang xacNhanThanhToan(Integer donHangId);

    /**
     * Lấy danh sách IMEI đã được gán cho một đơn hàng.
     * <p>
     * Phương thức này được sử dụng để hiển thị danh sách IMEI trong trang chi tiết đơn hàng,
     * giúp nhân viên kiểm tra và đối soát khi giao hàng.
     * <p>
     * Chỉ những IMEI có {@code donHang.id = donHangId} và {@code tinhTrang = 'da_ban'}
     * mới được trả về (vì khi xác nhận đơn, IMEI đã được chuyển sang trạng thái 'da_ban').
     *
     * @param donHangId ID đơn hàng cần lấy danh sách IMEI.
     * @return Danh sách {@link MayDienThoai} thuộc đơn hàng đó.
     * @throws jakarta.persistence.EntityNotFoundException nếu đơn hàng không tồn tại.
     */
    List<MayDienThoai> layDanhSachImeiTheoDonHang(Integer donHangId);

    /**
     * DTO đại diện cho việc chọn IMEI cho một chi tiết đơn hàng.
     * <p>
     * Được sử dụng trong {@link #xacNhanDonHangVoiImei}.
     */
    class ImeiSelection {
        private Integer chiTietDonHangId;
        private List<String> imeiList;

        public Integer getChiTietDonHangId() {
            return chiTietDonHangId;
        }

        public void setChiTietDonHangId(Integer chiTietDonHangId) {
            this.chiTietDonHangId = chiTietDonHangId;
        }

        public List<String> getImeiList() {
            return imeiList;
        }

        public void setImeiList(List<String> imeiList) {
            this.imeiList = imeiList;
        }
    }

}
