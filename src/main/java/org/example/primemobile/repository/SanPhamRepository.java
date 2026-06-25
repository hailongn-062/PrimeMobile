package org.example.primemobile.repository;

import org.example.primemobile.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Integer> {

    /** Kiểm tra trùng mã sản phẩm — dùng khi thêm mới. */
    boolean existsByMaSanPham(String maSanPham);

    /** Kiểm tra trùng mã, loại trừ ID hiện tại — dùng khi cập nhật. */
    boolean existsByMaSanPhamAndIdNot(String maSanPham, Integer id);

    /**
     * Phân trang + lọc linh hoạt theo danh mục và/hoặc hãng sản xuất.
     * Tham số null = bỏ qua điều kiện đó (lọc tất cả).
     * Dùng cho Admin Dashboard — bao gồm cả sản phẩm ngừng bán.
     */
    @Query("SELECT s FROM SanPham s WHERE " +
           "(:danhMucId IS NULL OR s.danhMuc.id = :danhMucId) AND " +
           "(:hangSanXuatId IS NULL OR s.hangSanXuat.id = :hangSanXuatId)")
    Page<SanPham> timKiemVaLocSanPham(
            @Param("danhMucId")      Integer danhMucId,
            @Param("hangSanXuatId") Integer hangSanXuatId,
            Pageable pageable);

    /**
     * Phân trang + lọc dành cho Frontend công khai — chỉ lấy sản phẩm {@code trangThai = 'dang_ban'}.
     * Khách vãng lai và khách hàng không được thấy hàng ngừng bán.
     */
    @Query("SELECT s FROM SanPham s WHERE " +
           "s.trangThai = 'dang_ban' AND " +
           "(:danhMucId IS NULL OR s.danhMuc.id = :danhMucId) AND " +
           "(:hangSanXuatId IS NULL OR s.hangSanXuat.id = :hangSanXuatId)")
    Page<SanPham> timKiemSanPhamPublic(
            @Param("danhMucId")      Integer danhMucId,
            @Param("hangSanXuatId") Integer hangSanXuatId,
            Pageable pageable);
}
