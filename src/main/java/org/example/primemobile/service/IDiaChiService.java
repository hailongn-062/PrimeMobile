package org.example.primemobile.service;

import org.example.primemobile.entity.DiaChiKhachHang;

import java.util.List;

/**
 * Nghiệp vụ Địa Chỉ Khách Hàng (Public API, khách hàng tự quản lý).
 * <p>
 * Mỗi địa chỉ lưu 2 nhóm thông tin: ID GHN (tính phí ship) + Tên (hiển thị UI).
 * Giới hạn tối đa {@value #MAX_DIA_CHI} địa chỉ / khách hàng.
 */
public interface IDiaChiService {

    int MAX_DIA_CHI = 5;

    /** Lấy tất cả địa chỉ của khách hàng (mặc định lên đầu). */
    List<DiaChiKhachHang> layDanhSach(Integer khachHangId);

    /** Thêm địa chỉ mới (tối đa 5 địa chỉ / khách). */
    DiaChiKhachHang them(Integer khachHangId, DiaChiKhachHang request);

    /** Sửa thông tin địa chỉ. */
    DiaChiKhachHang sua(Integer diaChiId, Integer khachHangId, DiaChiKhachHang request);

    /**
     * Set địa chỉ làm mặc định.
     * Reset toàn bộ macDinh của khách về false trước, rồi set địa chỉ này = true.
     */
    DiaChiKhachHang setMacDinh(Integer diaChiId, Integer khachHangId);

    /** Xóa địa chỉ (không được xóa địa chỉ duy nhất còn lại). */
    void xoa(Integer diaChiId, Integer khachHangId);
}
