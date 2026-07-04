package org.example.primemobile.service;

import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.entity.ChiTietFlashSale;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.PhamViKhuyenMai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nghiệp vụ Khuyến Mãi cho PrimeMobile.
 *
 * 4 loại khuyến mãi (loai):
 * - "phan_tram" → Giảm % cơ bản theo thời gian
 * - "don_hang_toi_thieu" → Giảm % nếu tổng tiền >= donHangToiThieu
 * - "giam_gia_truc_tiep" → Giảm % cho sản phẩm cụ thể (PhamViKhuyenMai)
 * - "flash_sale" → Giảm % cho biến thể cụ thể theo khung giờ (ChiTietFlashSale)
 */
public interface IKhuyenMaiService {

    // ── CRUD chính ─────────────────────────────────────────────────────────

    /** Lấy toàn bộ danh sách chương trình (sắp xếp theo id DESC). */
    List<ChuongTrinhKhuyenMai> layDanhSach();

    /** Lấy chi tiết 1 chương trình theo ID. */
    ChuongTrinhKhuyenMai layTheoId(Integer id);

    /**
     * Lưu (Thêm mới / Cập nhật) chương trình khuyến mãi.
     * Validate dựa trên trường {@code loai}.
     *
     * @throws IllegalArgumentException nếu dữ liệu không hợp lệ.
     */
    ChuongTrinhKhuyenMai luu(ChuongTrinhKhuyenMai ctkm);

    /** Xóa mềm (đổi trạng thái) hoặc xóa hẳn 1 chương trình. */
    void xoa(Integer id);

    // ── Sub-form: Flash Sale ───────────────────────────────────────────────

    /** Lấy danh sách chi tiết Flash Sale theo ctkmId. */
    List<ChiTietFlashSale> layChiTietFlashSale(Integer ctkmId);

    /**
     * Thêm biến thể vào Flash Sale.
     *
     * @param ctkmId         ID chương trình flash sale.
     * @param bienTheId      ID biến thể sản phẩm.
     * @param phanTramGiam   Phần trăm giảm (0-100).
     * @param soLuongGioiHan Số lượng tối đa bán với giá flash.
     */
    void themChiTietFlashSale(Integer ctkmId, Integer bienTheId, BigDecimal phanTramGiam, Integer soLuongGioiHan);

    /** Xóa 1 dòng chi tiết Flash Sale theo chiTietId. */
    void xoaChiTietFlashSale(Integer chiTietId);

    // ── Sub-form: Phạm vi áp dụng (giam_gia_truc_tiep) ────────────────────

    /** Lấy danh sách phạm vi của 1 chương trình. */
    List<PhamViKhuyenMai> layPhamVi(Integer ctkmId);

    /**
     * Thêm sản phẩm vào phạm vi áp dụng.
     *
     * @param ctkmId    ID chương trình.
     * @param sanPhamId ID sản phẩm.
     */
    void themPhamVi(Integer ctkmId, Integer sanPhamId);

    /** Xóa 1 dòng phạm vi theo phamViId. */
    void xoaPhamVi(Integer phamViId);

    // ── Trạng thái ──────────────────────────────────────────────────────────

    /**
     * Toggle trạng thái Tạm dừng / Mở lại cho 1 chương trình khuyến mãi.
     *
     * <ul>
     * <li>Nếu đang {@code tam_dung}: tính lại trạng thái thực tế dựa vào thời gian
     * hiện tại
     * ({@code chua_bat_dau} / {@code dang_dien_ra} / {@code da_ket_thuc}).</li>
     * <li>Ngược lại: đặt thành {@code tam_dung}.</li>
     * </ul>
     *
     * @param id ID của chương trình khuyến mãi.
     */
    void toggleTrangThai(Integer id);

    // ── Public (frontend) ──────────────────────────────────────────────────

    /** Lấy danh sách khuyến mãi đang diễn ra (dùng cho trang chủ / checkout). */
    List<ChuongTrinhKhuyenMai> layKhuyenMaiDangDienRa();

    // ── POS / Bán hàng tại quầy ────────────────────────────────────────────

    /**
     * Tìm chương trình khuyến mãi mang lại số tiền giảm LỚN NHẤT cho đơn hàng.
     *
     * <p>
     * Tiêu chí lọc:
     * <ul>
     * <li>Chỉ xét các CTKM có {@code trangThai = 'dang_dien_ra'}.</li>
     * <li>Chỉ xét loại áp dụng toàn bộ đơn hàng:
     * {@code "phan_tram"} hoặc {@code "don_hang_toi_thieu"}.</li>
     * <li>Lọc bỏ nếu {@code donHangToiThieu} > {@code tongTienHang}.</li>
     * </ul>
     *
     * <p>
     * Công thức tính tiền giảm:
     * <ul>
     * <li>{@code "phan_tram"}: {@code tongTienHang × (giaTriUuDai / 100)}</li>
     * <li>{@code "don_hang_toi_thieu"}: {@code tongTienHang × (giaTriUuDai / 100)}</li>
     * </ul>
     *
     * @param tongTienHang Tổng tiền hàng của đơn (chưa giảm, chưa cộng phí ship).
     * @return CTKM tốt nhất, hoặc {@code null} nếu không có CTKM nào phù hợp.
     */
    ChuongTrinhKhuyenMai timKhuyenMaiTotNhatChoDonHang(BigDecimal tongTienHang);

    // ── Tính giá sau khuyến mãi động ──────────────────────────────────────

    /**
     * Tính giá bán thực tế của một biến thể sản phẩm sau khi áp dụng
     * các chương trình khuyến mãi đang diễn ra.
     *
     * <p>
     * Thứ tự ưu tiên áp dụng khuyến mãi (theo system_rules.md §5):
     * <ol>
     * <li><b>Flash Sale</b> – áp dụng cho biến thể cụ thể trong khung giờ
     * và còn số lượng.</li>
     * <li><b>Giảm giá trực tiếp</b> – áp dụng cho sản phẩm thông qua
     * {@code pham_vi_khuyen_mai}.</li>
     * <li><b>Phần trăm</b> hoặc <b>Đơn hàng tối thiểu</b> – áp dụng cho
     * toàn đơn, cần {@code tongTienHang} để kiểm tra điều kiện.</li>
     * </ol>
     * Nếu không có khuyến mãi nào phù hợp, trả về giá gốc ({@code giaBan}).
     *
     * <p>
     * Lưu ý: Đối với loại {@code "don_hang_toi_thieu"}, cần truyền
     * {@code tongTienHang} để kiểm tra ngưỡng. Nếu truyền {@code null},
     * loại này sẽ bị bỏ qua.
     *
     * @param bienTheId    ID của biến thể sản phẩm (SKU) cần tính giá.
     * @param tongTienHang Tổng tiền hàng của đơn (có thể {@code null}
     *                     nếu chưa có hoặc không cần áp dụng loại toàn đơn).
     * @return Giá bán thực tế sau khuyến mãi, hoặc {@code giaBan} nếu không có KM.
     */
    BigDecimal tinhGiaSauKhuyenMai(Integer bienTheId, BigDecimal tongTienHang);

    // ── Tính khuyến mãi cho đơn hàng (dùng chung Online & POS) ────────────

    /**
     * Tính khuyến mãi tốt nhất cho một đơn hàng dựa trên tổng tiền hàng gốc.
     * Chỉ áp dụng các CTKM loại toàn đơn: 'phan_tram' và 'don_hang_toi_thieu'.
     * Trả về kết quả gồm CTKM được chọn và số tiền giảm.
     *
     * <p>
     * Phương thức này được sử dụng thống nhất cho cả:
     * <ul>
     *   <li><b>Bán hàng Online:</b> Khi tạo đơn, tính tiền giảm để lưu vào DonHang.</li>
     *   <li><b>Bán hàng POS:</b> Tính khuyến mãi trước khi thanh toán.</li>
     *   <li><b>Giỏ hàng:</b> Hiển thị tổng tiền sau giảm.</li>
     * </ul>
     *
     * @param tongTienHang Tổng tiền hàng gốc (chưa áp dụng bất kỳ khuyến mãi nào).
     * @return {@link KhuyenMaiResult} chứa thông tin CTKM và tiền giảm;
     *         nếu không có CTKM phù hợp, tienGiam = 0 và tongSauGiam = tongTienHang.
     */
    KhuyenMaiResult tinhKhuyenMaiChoDonHang(BigDecimal tongTienHang);
}