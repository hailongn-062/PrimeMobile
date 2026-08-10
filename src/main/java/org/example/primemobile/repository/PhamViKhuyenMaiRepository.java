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
     * Tìm các phạm vi áp dụng cho chương trình khuyến mãi theo biến thể sản phẩm đang hoạt động.
     * <p>
     * Điều kiện lọc:
     * <ul>
     *   <li>Biến thể phải khớp với {@code bienTheId}.</li>
     *   <li>Chương trình khuyến mãi phải có loại {@code 'theo_san_pham'}.</li>
     *   <li>Chương trình khuyến mãi phải có trạng thái {@code 'dang_dien_ra'}.</li>
     * </ul>
     * <p>
     * Phương thức này được sử dụng trong {@code KhuyenMaiServiceImpl.tinhGiaSauKhuyenMai}
     * để áp dụng giảm giá theo biến thể trong giỏ hàng/POS.
     *
     * @param bienTheId ID của biến thể cần kiểm tra.
     * @return Danh sách phạm vi khuyến mãi theo biến thể đang áp dụng.
     */
    @Query("SELECT p FROM PhamViKhuyenMai p " +
            "WHERE p.bienThe.id = :bienTheId " +
            "AND p.chuongTrinhKhuyenMai.trangThai = 'dang_dien_ra' " +
            "AND p.chuongTrinhKhuyenMai.loai = 'theo_san_pham'")
    List<PhamViKhuyenMai> findActiveGiamGiaTrucTiepByBienTheId(@Param("bienTheId") Integer bienTheId);
}