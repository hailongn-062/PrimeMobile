package org.example.primemobile.repository;

import org.example.primemobile.entity.BienTheSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BienTheSanPhamRepository extends JpaRepository<BienTheSanPham, Integer> {

    /** Lấy tất cả biến thể của một sản phẩm theo sanPhamId. */
    List<BienTheSanPham> findBySanPhamId(Integer sanPhamId);

    /** Kiểm tra trùng mã SKU — dùng khi thêm mới. */
    boolean existsByMaSku(String maSku);

    /** Kiểm tra trùng mã SKU, loại trừ ID hiện tại — dùng khi cập nhật. */
    boolean existsByMaSkuAndIdNot(String maSku, Integer id);
}
