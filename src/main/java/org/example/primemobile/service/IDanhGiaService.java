package org.example.primemobile.service;

import org.example.primemobile.entity.DanhGiaSanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IDanhGiaService {

    /**
     * Kiểm tra xem khách hàng có đơn hàng nào hợp lệ để đánh giá sản phẩm không.
     */
    KiemTraDieuKienDto kiemTraDieuKienDanhGia(Integer khachHangId, Integer sanPhamId);

    /**
     * Khách hàng tạo đánh giá mới (sẽ ở trạng thái cho_duyet).
     */
    DanhGiaSanPham taoDanhGia(Integer khachHangId, DanhGiaRequest request);

    /**
     * Lấy danh sách các đánh giá ĐÃ DUYỆT của một sản phẩm.
     */
    List<DanhGiaSanPham> layDanhGiaTheoSanPham(Integer sanPhamId);

    /**
     * Tính sao trung bình và tổng số lượt đánh giá đã duyệt của một sản phẩm.
     */
    SaoTrungBinhDto tinhSaoTrungBinh(Integer sanPhamId);

    /**
     * Admin/Nhân viên lấy tất cả đánh giá để quản lý (phân trang).
     */
    Page<DanhGiaSanPham> layTatCaDanhGia(Pageable pageable);

    /**
     * Admin/Nhân viên duyệt đánh giá.
     */
    DanhGiaSanPham duyetDanhGia(Integer danhGiaId);

    /**
     * Admin/Nhân viên ẩn đánh giá.
     */
    DanhGiaSanPham anDanhGia(Integer danhGiaId);

    // =========================================================================
    // DTOs
    // =========================================================================

    record DanhGiaRequest(
            Integer sanPhamId,
            Integer donHangId,
            Integer sao,
            String tieuDe,
            String noiDung,
            String hinhAnhJson
    ) {}

    record SaoTrungBinhDto(
            Double saoTrungBinh,
            Long tongDanhGia
    ) {}

    record KiemTraDieuKienDto(
            boolean hopLe,
            Integer donHangId,
            String message
    ) {}
}
