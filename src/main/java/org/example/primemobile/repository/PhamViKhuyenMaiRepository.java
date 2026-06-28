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
     * Phương thức này vẫn được giữ lại để đảm bảo tương thích với các module khác.
     *
     * @param ctkmId ID chương trình khuyến mãi.
     * @return Danh sách phạm vi khuyến mãi.
     */
    List<PhamViKhuyenMai> findByChuongTrinhKhuyenMaiId(Integer ctkmId);

    /**
     * Tìm các phạm vi áp dụng cho giảm giá trực tiếp đang hoạt động của một sản phẩm.
     * <p>
     * Điều kiện lọc:
     * <ul>
     *   <li>Sản phẩm phải khớp với {@code sanPhamId}.</li>
     *   <li>Chương trình khuyến mãi phải có loại {@code 'giam_gia_truc_tiep'}.</li>
     *   <li>Chương trình khuyến mãi phải có trạng thái {@code 'dang_dien_ra'}.</li>
     * </ul>
     * <p>
     * Phương thức này được sử dụng trong {@code KhuyenMaiServiceImpl.tinhGiaSauKhuyenMai}
     * để áp dụng giảm giá trực tiếp cho từng sản phẩm trong giỏ hàng/POS.
     *
     * @param sanPhamId ID của sản phẩm cần kiểm tra.
     * @return Danh sách phạm vi khuyến mãi trực tiếp đang áp dụng (thường chỉ 0 hoặc 1 phần tử).
     */
    @Query("SELECT p FROM PhamViKhuyenMai p " +
            "WHERE p.sanPham.id = :sanPhamId " +
            "AND p.chuongTrinhKhuyenMai.trangThai = 'dang_dien_ra' " +
            "AND p.chuongTrinhKhuyenMai.loai = 'giam_gia_truc_tiep'")
    List<PhamViKhuyenMai> findActiveGiamGiaTrucTiepBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}