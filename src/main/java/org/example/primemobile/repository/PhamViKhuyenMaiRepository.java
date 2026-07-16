package org.example.primemobile.repository;

import org.example.primemobile.entity.PhamViKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhamViKhuyenMaiRepository extends JpaRepository<PhamViKhuyenMai, Integer> {

    /**
     * Lấy danh sách phạm vi khuyến mãi theo ID chương trình khuyến mãi.
     *
     * @param ctkmId ID chương trình khuyến mãi.
     * @return Danh sách phạm vi khuyến mãi.
     */
    List<PhamViKhuyenMai> findByChuongTrinhKhuyenMaiId(Integer ctkmId);

    /**
     * Tìm các phạm vi áp dụng cho chương trình khuyến mãi theo sản phẩm đang hoạt động.
     * <p>
     * Điều kiện lọc:
     * <ul>
     *   <li>Sản phẩm phải khớp với {@code sanPhamId}.</li>
     *   <li>Chương trình khuyến mãi phải có loại {@code 'theo_san_pham'}.</li>
     *   <li>Chương trình khuyến mãi phải có trạng thái {@code 'dang_dien_ra'}.</li>
     * </ul>
     * <p>
     * Phương thức này được sử dụng trong {@code KhuyenMaiServiceImpl.tinhGiaSauKhuyenMai}
     * để áp dụng giảm giá theo sản phẩm trong giỏ hàng/POS.
     *
     * @param sanPhamId ID của sản phẩm cần kiểm tra.
     * @return Danh sách phạm vi khuyến mãi theo sản phẩm đang áp dụng.
     */
    @Query("SELECT p FROM PhamViKhuyenMai p " +
            "WHERE p.sanPham.id = :sanPhamId " +
            "AND p.chuongTrinhKhuyenMai.trangThai = 'dang_dien_ra' " +
            "AND p.chuongTrinhKhuyenMai.loai = 'theo_san_pham'")
    List<PhamViKhuyenMai> findActiveGiamGiaTrucTiepBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}