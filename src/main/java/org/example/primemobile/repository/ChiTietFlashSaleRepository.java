package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietFlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietFlashSaleRepository extends JpaRepository<ChiTietFlashSale, Integer> {

    /**
     * Lấy danh sách chi tiết flash sale theo ID chương trình khuyến mãi.
     * Phương thức này vẫn được giữ lại để đảm bảo tương thích với các module khác.
     *
     * @param ctkmId ID chương trình khuyến mãi.
     * @return Danh sách chi tiết flash sale.
     */
    List<ChiTietFlashSale> findByChuongTrinhKhuyenMaiId(Integer ctkmId);

    /**
     * Tìm các chi tiết flash sale áp dụng cho một biến thể sản phẩm tại thời điểm hiện tại.
     * <p>
     * Điều kiện lọc:
     * <ul>
     *   <li>Biến thể sản phẩm phải khớp với {@code bienTheId}.</li>
     *   <li>Chương trình khuyến mãi phải có trạng thái {@code 'dang_dien_ra'}.</li>
     *   <li>Số lượng đã bán (daBan) phải nhỏ hơn số lượng giới hạn (soLuongGioiHan).</li>
     *   <li>Thời điểm hiện tại (CURRENT_TIMESTAMP) phải nằm trong khoảng
     *       {@code gioFlashBatDau} và {@code gioFlashKetThuc} của chương trình flash sale.</li>
     * </ul>
     * <p>
     * Phương thức này được sử dụng trong {@code KhuyenMaiServiceImpl.tinhGiaSauKhuyenMai}
     * để tính giá sau flash sale cho từng biến thể trong giỏ hàng/POS.
     *
     * @param bienTheId ID của biến thể sản phẩm (SKU) cần kiểm tra.
     * @return Danh sách chi tiết flash sale đang áp dụng (thường chỉ 0 hoặc 1 phần tử,
     *         nhưng vẫn trả về List để linh hoạt).
     */
    @Query("SELECT c FROM ChiTietFlashSale c " +
            "WHERE c.bienTheSanPham.id = :bienTheId " +
            "AND c.chuongTrinhKhuyenMai.trangThai = 'dang_dien_ra' " +
            "AND c.daBan < c.soLuongGioiHan " +
            "AND CURRENT_TIMESTAMP BETWEEN c.chuongTrinhKhuyenMai.gioFlashBatDau " +
            "AND c.chuongTrinhKhuyenMai.gioFlashKetThuc")
    List<ChiTietFlashSale> findActiveFlashSaleByBienTheId(@Param("bienTheId") Integer bienTheId);
}