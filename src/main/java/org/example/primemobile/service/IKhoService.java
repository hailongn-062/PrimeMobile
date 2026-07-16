package org.example.primemobile.service;

import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest;
import org.example.primemobile.entity.MayDienThoai;
import org.example.primemobile.entity.PhieuNhapKho;
import org.example.primemobile.entity.TonKho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Interface định nghĩa các nghiệp vụ quản lý Kho hàng.
 * <p>
 * Hệ thống có 1 kho duy nhất (ID = 1). Toàn bộ nhập/xuất kho đều thực hiện
 * trên kho này.
 */
public interface IKhoService {

    /**
     * Tạo mới phiếu nhập kho và tự động cộng tồn kho.
     * <p>
     * Quy trình: Tạo {@link PhieuNhapKho} + các {@link org.example.primemobile.entity.ChiTietPhieuNhap}
     * → Với mỗi dòng, gọi helper {@code congTonKho()} để cập nhật bảng {@link TonKho}.
     *
     * @param request    Thông tin phiếu nhập (khoId, danh sách SKU + số lượng + đơn giá nhập).
     * @param nguoiTaoId ID của người dùng (NhanVien/Admin) thực hiện thao tác.
     * @return Đối tượng {@link PhieuNhapKho} đã được lưu vào DB.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy kho, NCC, hoặc biến thể.
     * @throws IllegalArgumentException                   Nếu dữ liệu không hợp lệ.
     */
    PhieuNhapKho taoPhieuNhapKho(TaoPhieuNhapKhoRequest request, Integer nguoiTaoId);

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

    /**
     * Lấy danh sách phiếu nhập kho có phân trang.
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

    /**
     * Lấy danh sách IMEI đang trong kho (tinh_trang = 'trong_kho') của một biến thể.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm.
     * @return Danh sách {@link MayDienThoai} có tinh_trang = 'trong_kho'.
     * @throws jakarta.persistence.EntityNotFoundException Nếu không tìm thấy biến thể.
     */
    List<MayDienThoai> layDanhSachImeiTrongKho(Integer bienTheSanPhamId);
}