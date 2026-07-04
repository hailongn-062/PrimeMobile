package org.example.primemobile.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.primemobile.entity.MayDienThoai;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO chi tiết đơn hàng – dùng để trả về thông tin đơn hàng cùng danh sách sản phẩm
 * cho màn hình quản lý đơn hàng (Admin) và modal xác nhận IMEI.
 * <p>
 * Mục đích:
 * <ul>
 *   <li>Tránh vòng lặp vô hạn khi serialize entity {@link org.example.primemobile.entity.DonHang}
 *       (do quan hệ 1-N với {@link org.example.primemobile.entity.ChiTietDonHang}).</li>
 *   <li>Kiểm soát dữ liệu trả về API, chỉ expose các trường cần thiết.</li>
 *   <li>Giảm tải dữ liệu không cần thiết (các quan hệ lazy không được load).</li>
 * </ul>
 *
 * @see org.example.primemobile.entity.DonHang
 * @see ChiTietDonHangDto
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DonHangChiTietDto {

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN CHUNG ĐƠN HÀNG
    // ────────────────────────────────────────────────────────────────────────

    /** ID đơn hàng (PK) */
    private Integer id;

    /** Mã đơn hàng hiển thị (ví dụ: "DHO-20260702-629428") */
    private String maDonHang;

    /** Thời điểm khách đặt hàng */
    private LocalDateTime ngayDat;

    /** Trạng thái xử lý đơn hàng: "cho_xac_nhan", "da_xac_nhan", "dang_giao", "da_giao", "da_huy" */
    private String trangThai;

    /** Trạng thái thanh toán: "chua_thanh_toan", "dang_chuyen_huong", "da_thanh_toan", "that_bai" */
    private String trangThaiThanhToan;

    /** Kênh bán: "online" hoặc "tai_quay" */
    private String kenhBan;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN KHÁCH HÀNG
    // ────────────────────────────────────────────────────────────────────────

    /** Tên khách hàng (lấy từ khach_hang.hoTen hoặc snapshot ho_ten_nguoi_nhan) */
    private String tenKhachHang;

    /** Số điện thoại khách hàng (lấy từ khach_hang.soDienThoai hoặc snapshot sdt_nguoi_nhan) */
    private String soDienThoaiKhach;

    /** Email khách hàng (lấy từ khach_hang.email) */
    private String emailKhach;

    // ────────────────────────────────────────────────────────────────────────
    // SNAPSHOT ĐỊA CHỈ GIAO HÀNG (lưu tại thời điểm đặt hàng, không đổi)
    // ────────────────────────────────────────────────────────────────────────

    /** Tên người nhận hàng (snapshot) */
    private String hoTenNguoiNhan;

    /** Số điện thoại người nhận (snapshot) */
    private String sdtNguoiNhan;

    /** Địa chỉ cụ thể (số nhà, tên đường) – snapshot */
    private String diaChiGiaoCuThe;

    /** Tên phường/xã – snapshot */
    private String phuongXaGiao;

    /** Tên quận/huyện – snapshot */
    private String quanHuyenGiao;

    /** Tên tỉnh/thành phố – snapshot */
    private String tinhThanhGiao;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN VẬN CHUYỂN
    // ────────────────────────────────────────────────────────────────────────

    /** Ngày giao hàng dự kiến (lấy từ GHN API) */
    private LocalDate ngayGiaoDuKien;

    /** Ngày giao hàng thực tế (được cập nhật khi đơn chuyển sang trạng thái da_giao) */
    private LocalDateTime ngayGiaoThucTe;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN XỬ LÝ ĐƠN HÀNG
    // ────────────────────────────────────────────────────────────────────────

    /** Tên nhân viên/Admin xử lý đơn hàng (nguoi_xu_ly.hoTen) */
    private String tenNguoiXuLy;

    // ────────────────────────────────────────────────────────────────────────
    // TÀI CHÍNH
    // ────────────────────────────────────────────────────────────────────────

    /** Tổng tiền hàng (chưa giảm giá, chưa cộng phí ship) */
    private BigDecimal tongTienHang;

    /** Số tiền được giảm (từ chương trình khuyến mãi toàn đơn) */
    private BigDecimal tienGiamGia;

    /** Phí vận chuyển (lấy từ GHN API hoặc fallback = 0) */
    private BigDecimal phiShip;

    /** Tổng thanh toán = tongTienHang - tienGiamGia + phiShip (cột computed) */
    private BigDecimal tongThanhToan;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN KHÁC
    // ────────────────────────────────────────────────────────────────────────

    /** Ghi chú của khách hàng hoặc nhân viên */
    private String ghiChu;

    /**
     * Lý do hủy đơn (nếu đơn hàng đã hủy, lấy từ ghiChu hoặc một trường riêng).
     * Hiện tại lý do hủy được ghi vào ghiChu, nên có thể không cần trường riêng.
     * Nếu cần, có thể parse từ ghiChu.
     */

    // ────────────────────────────────────────────────────────────────────────
    // DANH SÁCH IMEI ĐÃ GÁN CHO ĐƠN HÀNG
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Danh sách IMEI đã được gán cho đơn hàng (chỉ có khi đơn hàng đã xác nhận).
     * Mỗi phần tử là một {@link MayDienThoai} chứa thông tin IMEI và serial.
     * <p>
     * Lưu ý: Chỉ những IMEI có {@code tinhTrang = 'da_ban'} và
     * {@code donHang.id = id} mới được đưa vào danh sách này.
     */
    private List<MayDienThoai> imeiList;

    // ────────────────────────────────────────────────────────────────────────
    // DANH SÁCH SẢN PHẨM TRONG ĐƠN HÀNG (chi tiết)
    // ────────────────────────────────────────────────────────────────────────

    /**
     * Danh sách chi tiết sản phẩm thuộc đơn hàng này.
     * Mỗi phần tử là một {@link ChiTietDonHangDto} chứa thông tin SKU,
     * số lượng, giá bán snapshot, tên sản phẩm và danh sách IMEI đã gán.
     */
    private List<ChiTietDonHangDto> chiTietDonHangs;
}