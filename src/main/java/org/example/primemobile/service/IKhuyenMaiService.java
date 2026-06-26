package org.example.primemobile.service;

import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.MaGiamGia;

import java.math.BigDecimal;
import java.util.List;

/**
 * Nghiệp vụ Khuyến Mãi & Mã Giảm Giá cho PrimeMobile.
 *
 * <h3>Thiết kế API check mã (checkMa):</h3>
 * Trả về {@code BigDecimal} — số tiền được giảm.
 * Frontend dùng để hiển thị "Bạn được giảm X đồng" trước khi đặt hàng.
 * Giá trị 0 = mã không hợp lệ (đã có message lỗi ném ra).
 */
public interface IKhuyenMaiService {

    // ── Admin: Chương trình khuyến mãi ────────────────────────────────
    List<ChuongTrinhKhuyenMai> layDanhSach();
    ChuongTrinhKhuyenMai taoMoi(ChuongTrinhKhuyenMai request);
    ChuongTrinhKhuyenMai capNhat(Integer id, ChuongTrinhKhuyenMai request);

    // ── Public: Chương trình khuyến mãi ───────────────────────────────
    List<ChuongTrinhKhuyenMai> layKhuyenMaiDangDienRa();


    /** Lấy chi tiết 1 chương trình khuyến mãi theo ID. */
    ChuongTrinhKhuyenMai layTheoId(Integer id);

    /** Lấy danh sách mã giảm giá của 1 chương trình. */
    List<MaGiamGia> layMaGiamGia(Integer ctkmId);

    // ── Admin: Sinh mã giảm giá ───────────────────────────────────────
    /**
     * Sinh danh sách mã giảm giá ngẫu nhiên (duy nhất) cho 1 chương trình.
     *
     * @param ctkmId   ID chương trình khuyến mãi loại "ma_code".
     * @param soLuong  Số mã cần sinh.
     * @param prefix   Tiền tố (ví dụ: "SUMMER24" → "SUMMER24-XXXXXX").
     * @return Danh sách mã vừa tạo.
     */
    List<MaGiamGia> sinhMaGiamGia(Integer ctkmId, int soLuong, String prefix);

    // ── Admin: Chi tiết Flash Sale ────────────────────────────────────
    /** Thêm một biến thể vào Flash Sale. */
    void themChiTietFlashSale(Integer ctkmId, Integer bienTheId, java.math.BigDecimal giaFlash, Integer soLuongGioiHan);

    /** Xóa một dòng chi tiết Flash Sale theo ID. */
    void xoaChiTietFlashSale(Integer chiTietId);

    /** Lấy danh sách chi tiết Flash Sale của 1 chương trình. */
    List<org.example.primemobile.entity.ChiTietFlashSale> layChiTietFlashSale(Integer ctkmId);

    // ── Admin: Phạm vi khuyến mãi ─────────────────────────────────────
    /** Thêm phạm vi áp dụng. */
    void themPhamVi(Integer ctkmId, Integer sanPhamId, Integer danhMucId, Integer hangSanXuatId);

    /** Xóa phạm vi theo ID. */
    void xoaPhamVi(Integer phamViId);

    /** Lấy danh sách phạm vi của 1 chương trình. */
    List<org.example.primemobile.entity.PhamViKhuyenMai> layPhamVi(Integer ctkmId);

    // ── Public: Check mã tại Checkout ────────────────────────────────
    /**
     * Kiểm tra mã giảm giá hợp lệ và tính số tiền được giảm.
     *
     * <h3>Các điều kiện kiểm tra:</h3>
     * <ol>
     *   <li>Mã tồn tại trong hệ thống.</li>
     *   <li>Chương trình KM đang ở trạng thái {@code "dang_dien_ra"}.</li>
     *   <li>Chương trình KM chưa hết hạn ({@code ngayKetThuc} > now).</li>
     *   <li>Mã còn lượt dùng ({@code daSuDung} &lt; {@code soLuongToiDa}).</li>
     *   <li>Tổng đơn hàng >= {@code donHangToiThieu}.</li>
     * </ol>
     *
     * @param maCode   Mã code khách nhập.
     * @param tongTien Tổng tiền đơn hàng (trước giảm giá).
     * @return Số tiền được giảm (VNĐ). Ném {@link IllegalArgumentException} nếu mã không hợp lệ.
     */
    BigDecimal checkMa(String maCode, BigDecimal tongTien);
}
