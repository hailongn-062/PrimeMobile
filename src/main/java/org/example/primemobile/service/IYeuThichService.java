package org.example.primemobile.service;

import org.example.primemobile.entity.YeuThich;

import java.util.List;

public interface IYeuThichService {
    /**
     * Thêm hoặc xóa sản phẩm khỏi danh sách yêu thích.
     * 
     * @param khachHangId ID của khách hàng
     * @param sanPhamId ID của sản phẩm
     * @return true nếu đã thêm vào yêu thích, false nếu đã gỡ khỏi yêu thích
     */
    boolean toggleYeuThich(Integer khachHangId, Integer sanPhamId);

    /**
     * Lấy danh sách sản phẩm yêu thích của khách hàng.
     * 
     * @param khachHangId ID của khách hàng
     * @return Danh sách yêu thích được sắp xếp theo thời gian thêm mới nhất
     */
    List<YeuThich> layDanhSach(Integer khachHangId);

    /**
     * Kiểm tra xem khách hàng đã yêu thích sản phẩm này chưa.
     * 
     * @param khachHangId ID của khách hàng
     * @param sanPhamId ID của sản phẩm
     * @return true nếu đã yêu thích, ngược lại là false
     */
    boolean kiemTraDaYeuThich(Integer khachHangId, Integer sanPhamId);
}
