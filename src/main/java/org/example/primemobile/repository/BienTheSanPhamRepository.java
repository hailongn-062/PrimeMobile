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

    /** Tìm kiếm biến thể sản phẩm theo từ khóa (tên sản phẩm hoặc mã SKU). */
    @org.springframework.data.jpa.repository.Query("SELECT b FROM BienTheSanPham b WHERE b.sanPham.trangThai = 'dang_ban' " +
           "AND (LOWER(b.sanPham.tenSanPham) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(b.maSku) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY b.sanPham.tenSanPham ASC")
    List<BienTheSanPham> searchBienTheSanPham(@org.springframework.data.repository.query.Param("keyword") String keyword, org.springframework.data.domain.Pageable pageable);
}
