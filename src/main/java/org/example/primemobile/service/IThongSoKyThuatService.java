package org.example.primemobile.service;

import org.example.primemobile.entity.ThongSoKyThuat;

import java.util.List;

/**
 * Nghiệp vụ quản lý Thông Số Kỹ Thuật của Sản Phẩm (Admin).
 * <p>
 * Thông số gắn với {@code SanPham} (model), không phải với biến thể,
 * vì các thông số như CPU, màn hình, camera giống nhau giữa các biến thể.
 */
public interface IThongSoKyThuatService {

    /** Lấy danh sách thông số kỹ thuật của 1 sản phẩm (nhóm + thứ tự). */
    List<ThongSoKyThuat> layTheoSanPham(Integer sanPhamId);

    /** Thêm 1 thông số kỹ thuật mới cho sản phẩm. */
    ThongSoKyThuat them(Integer sanPhamId, ThongSoKyThuat request);

    /** Sửa thông số kỹ thuật (nhóm, tên, giá trị, thứ tự). */
    ThongSoKyThuat sua(Integer id, ThongSoKyThuat request);

    /** Xóa 1 thông số kỹ thuật theo ID. */
    void xoa(Integer id);

    /**
     * Thay thế toàn bộ thông số của sản phẩm bằng danh sách mới (upsert bulk).
     * Xóa hết cũ → lưu tất cả mới trong 1 transaction.
     */
    List<ThongSoKyThuat> capNhatToanBo(Integer sanPhamId, List<ThongSoKyThuat> danhSach);
}
