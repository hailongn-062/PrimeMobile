package org.example.primemobile.service;

import org.example.primemobile.entity.GioHang;

import java.math.BigDecimal;

/**
 * Contract cho phân hệ Giỏ Hàng Online.
 * <p>
 * Hỗ trợ 2 loại người dùng song song (system_rules.md §5):
 * <ul>
 * <li><b>Khách đã đăng nhập:</b> truyền {@code khachHangId}, bỏ qua
 * {@code sessionId}.</li>
 * <li><b>Khách vãng lai:</b> truyền {@code sessionId} (UUID từ cookie/browser),
 * bỏ qua {@code khachHangId}.</li>
 * </ul>
 * <p>
 * <b>Ràng buộc kho hàng bắt buộc:</b>
 * Mọi thao tác thêm/cập nhật số lượng đều phải kiểm tra tồn kho tại
 * {@code kho_tong}
 * (kho có {@code loai = 'kho_tong'}). Nếu số lượng yêu cầu vượt quá tồn kho,
 * ném
 * {@link IllegalArgumentException} ngay lập tức.
 */
public interface IGioHangService {

    /**
     * Lấy toàn bộ giỏ hàng kèm chi tiết sản phẩm và tổng tiền tạm tính.
     * <p>
     * Nếu không tìm thấy giỏ hàng, trả về {@code null} (không tạo giỏ rỗng).
     *
     * @param khachHangId ID khách hàng đã đăng nhập. Null nếu là khách vãng lai.
     * @param sessionId   Session token của khách vãng lai. Null nếu đã đăng nhập.
     * @return {@link GioHang} kèm
     *         {@link org.example.primemobile.entity.ChiTietGioHang},
     *         hoặc {@code null} nếu chưa có giỏ hàng.
     */
    GioHang layGioHang(Integer khachHangId, String sessionId);

    /**
     * Thêm sản phẩm vào giỏ hàng (upsert logic).
     * <p>
     * Luồng xử lý:
     * <ol>
     * <li>Kiểm tra giỏ hàng đã tồn tại chưa; nếu chưa thì tạo mới.</li>
     * <li>Kiểm tra SKU đã có trong giỏ chưa.</li>
     * <li>Nếu chưa có: INSERT dòng mới với soLuong = soLuong yêu cầu.</li>
     * <li>Nếu đã có: tổng soLuong_cũ + soLuong_mới.</li>
     * <li><b>LUẬT RÀNG BUỘC:</b> Trước khi lưu, kiểm tra soLuong tổng
     * ≤ tồn kho {@code kho_tong}. Nếu vi phạm →
     * {@link IllegalArgumentException}.</li>
     * </ol>
     *
     * @param khachHangId      ID khách hàng (null nếu vãng lai).
     * @param sessionId        Session token (null nếu đã đăng nhập).
     * @param bienTheSanPhamId ID biến thể sản phẩm cần thêm.
     * @param soLuong          Số lượng cần thêm (phải > 0).
     * @return {@link GioHang} sau khi cập nhật.
     * @throws IllegalArgumentException                    nếu soLuong > tồn kho
     *                                                     kho_tong hoặc soLuong
     *                                                     <= 0.
     * @throws jakarta.persistence.EntityNotFoundException nếu biến thể không tồn
     *                                                     tại.
     */
    GioHang themVaoGioHang(Integer khachHangId, String sessionId,
            Integer bienTheSanPhamId, Integer soLuong);

    /**
     * Cập nhật số lượng của một sản phẩm trong giỏ hàng.
     * <p>
     * <b>LUẬT RÀNG BUỘC:</b> soLuong mới phải ≤ tồn kho {@code kho_tong}.
     *
     * @param chiTietGioHangId ID dòng chi tiết giỏ hàng cần cập nhật.
     * @param soLuongMoi       Số lượng mới (phải > 0).
     * @return {@link GioHang} cha sau khi cập nhật.
     * @throws IllegalArgumentException                    nếu soLuongMoi > tồn kho
     *                                                     kho_tong hoặc
     *                                                     soLuongMoi <= 0.
     * @throws jakarta.persistence.EntityNotFoundException nếu chiTietGioHangId
     *                                                     không tồn tại.
     */
    GioHang capNhatSoLuong(Integer chiTietGioHangId, Integer soLuongMoi);

    /**
     * Xóa một dòng sản phẩm khỏi giỏ hàng.
     *
     * @param chiTietGioHangId ID dòng chi tiết giỏ hàng cần xóa.
     * @throws jakarta.persistence.EntityNotFoundException nếu chiTietGioHangId
     *                                                     không tồn tại.
     */
    void xoaKhoiGioHang(Integer chiTietGioHangId);

    /**
     * Xóa toàn bộ giỏ hàng (dùng sau khi đặt hàng thành công).
     *
     * @param khachHangId ID khách hàng (null nếu vãng lai).
     * @param sessionId   Session token (null nếu đã đăng nhập).
     */
    void xoaToanBoGioHang(Integer khachHangId, String sessionId);

    /**
     * Tính tổng tiền tạm tính của giỏ hàng dựa trên giá sau khuyến mãi động.
     *
     * @param gioHang Giỏ hàng cần tính (đã load chi tiết).
     * @return Tổng tiền tạm tính.
     */
    BigDecimal tinhTongTienTamTinh(GioHang gioHang);
}
