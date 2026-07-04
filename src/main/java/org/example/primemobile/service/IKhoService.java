package org.example.primemobile.service;

import org.example.primemobile.dto.kho.TaoPhieuChuyenKhoRequest;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest;
import org.example.primemobile.entity.MayDienThoai;
import org.example.primemobile.entity.PhieuChuyenKho;
import org.example.primemobile.entity.PhieuNhapKho;
import org.example.primemobile.entity.TonKho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ quản lý Kho hàng.
 * <p>
 * Toàn bộ luồng phụ thuộc vào 2 quy tắc cốt lõi (system_rules.md §3.1, §3.2):
 * <ol>
 *   <li><b>Chỉ nhập hàng vào Kho Tổng</b> — NCC → Kho Tổng</li>
 *   <li><b>Safety Stock Rule</b> — Tồn kho sau khi trừ KHÔNG được dưới 5 đơn vị/SKU/kho.</li>
 * </ol>
 */
public interface IKhoService {

    /**
     * Tạo mới phiếu nhập kho và tự động cộng tồn kho vào Kho Tổng.
     * <p>
     * Quy trình: Tạo {@link PhieuNhapKho} + các {@link org.example.primemobile.entity.ChiTietPhieuNhap}
     * → Với mỗi dòng, gọi helper {@code congTonKho()} để cập nhật bảng {@link TonKho}.
     *
     * @param request    Thông tin phiếu nhập (khoId, danh sách SKU + số lượng + đơn giá nhập).
     * @param nguoiTaoId ID của người dùng (NhanVien/Admin) thực hiện thao tác.
     * @return Đối tượng {@link PhieuNhapKho} đã được lưu vào DB.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy kho, NCC, hoặc biến thể.
     * @throws IllegalArgumentException                   Nếu kho đích không phải Kho Tổng, hoặc dữ liệu không hợp lệ.
     */
    PhieuNhapKho taoPhieuNhapKho(TaoPhieuNhapKhoRequest request, Integer nguoiTaoId);

    /**
     * Tạo mới phiếu chuyển kho giữa 2 kho, có kiểm tra Safety Stock Rule (tồn kho tối thiểu = 5).
     * <p>
     * Quy trình:
     * <ol>
     *   <li>Validate: khoNguon ≠ khoDich, danh sách chiTiets không rỗng.</li>
     *   <li>Pre-validate (Fail-fast): Duyệt TOÀN BỘ danh sách, kiểm tra Safety Stock Rule trước.
     *       Nếu bất kỳ dòng nào vi phạm → ném ngoại lệ NGAY, không thực hiện bất cứ thay đổi nào.</li>
     *   <li>Execute: Lưu phiếu → Trừ kho nguồn → Cộng kho đích → Lưu chi tiết.</li>
     * </ol>
     *
     * @param request    Thông tin phiếu chuyển (khoNguonId, khoDichId, danh sách SKU + số lượng).
     * @param nguoiTaoId ID của người dùng thực hiện thao tác.
     * @return Đối tượng {@link PhieuChuyenKho} đã được lưu.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy kho, biến thể, hoặc tồn kho nguồn.
     * @throws IllegalArgumentException                   Nếu vi phạm Safety Stock Rule hoặc khoNguon trùng khoDich.
     */
    PhieuChuyenKho taoPhieuChuyenKho(TaoPhieuChuyenKhoRequest request, Integer nguoiTaoId);

    /**
     * Lấy danh sách tồn kho chi tiết của một kho (kèm thông tin SKU và sản phẩm).
     *
     * @param khoId ID kho cần xem.
     * @return Danh sách {@link TonKho}, eager-loaded thông tin biến thể và sản phẩm.
     */
    List<TonKho> getTonKhoByKho(Integer khoId);

    /**
     * Lấy thông tin tồn kho của một SKU cụ thể tại một kho cụ thể.
     *
     * @param khoId            ID kho.
     * @param bienTheSanPhamId ID biến thể sản phẩm.
     * @return Đối tượng {@link TonKho} tìm thấy.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tồn tại bản ghi tồn kho.
     */
    TonKho getTonKhoChiTiet(Integer khoId, Integer bienTheSanPhamId);

    // =========================================================================
    // LỊCH SỬ PHIẾU NHẬP KHO
    // =========================================================================

    /**
     * Lấy danh sách phiếu nhập kho có phân trang — dùng cho màn hình lịch sử nhập kho.
     * <p>
     * Kết quả JOIN FETCH kho, NCC, người tạo để tránh N+1.
     * Sắp xếp mặc định: mới nhất lên đầu (truyền qua {@code Pageable}).
     *
     * @param pageable Phân trang và sắp xếp.
     * @return Trang {@link PhieuNhapKho} có đủ thông tin để hiển thị bảng danh sách.
     */
    Page<PhieuNhapKho> layDanhSachPhieuNhap(Pageable pageable);

    /**
     * Lấy chi tiết 1 phiếu nhập kho kèm toàn bộ dòng chi tiết (eager-load).
     *
     * @param id ID phiếu nhập kho.
     * @return {@link PhieuNhapKho} kèm danh sách {@code ChiTietPhieuNhap} → biến thể → sản phẩm.
     * @throws jakarta.persistence.EntityNotFoundException Nếu ID không tồn tại.
     */
    PhieuNhapKho layChiTietPhieuNhap(Integer id);

    // =========================================================================
    // LỊCH SỬ PHIẾU CHUYỂN KHO
    // =========================================================================

    /**
     * Lấy danh sách phiếu chuyển kho có phân trang — dùng cho màn hình lịch sử chuyển kho.
     * <p>
     * Kết quả JOIN FETCH kho nguồn, kho đích, người tạo để tránh N+1.
     *
     * @param pageable Phân trang và sắp xếp.
     * @return Trang {@link PhieuChuyenKho} có đủ thông tin để hiển thị bảng danh sách.
     */
    Page<PhieuChuyenKho> layDanhSachPhieuChuyen(Pageable pageable);

    /**
     * Lấy chi tiết 1 phiếu chuyển kho kèm toàn bộ dòng chi tiết (eager-load).
     *
     * @param id ID phiếu chuyển kho.
     * @return {@link PhieuChuyenKho} kèm danh sách {@code ChiTietChuyenKho} → biến thể → sản phẩm.
     * @throws jakarta.persistence.EntityNotFoundException Nếu ID không tồn tại.
     */
    PhieuChuyenKho layChiTietPhieuChuyen(Integer id);

    // =========================================================================
    // QUẢN LÝ IMEI THEO KHO
    // =========================================================================

    /**
     * Lấy danh sách IMEI đang trong kho (tinh_trang = 'trong_kho') của một biến thể.
     * <p>
     * Phục vụ cho:
     * <ul>
     *   <li>Xác nhận đơn online: lấy IMEI ở Kho Online để nhân viên chọn.</li>
     *   <li>Chuyển kho: lấy IMEI ở kho nguồn để nhân viên chọn chuyển đi.</li>
     * </ul>
     *
     * @param khoId            ID kho cần lấy IMEI.
     * @param bienTheSanPhamId ID biến thể sản phẩm.
     * @return Danh sách {@link MayDienThoai} có tinh_trang = 'trong_kho' và kho_id = khoId.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy kho hoặc biến thể.
     */
    List<MayDienThoai> layDanhSachImeiTrongKho(Integer khoId, Integer bienTheSanPhamId);
}