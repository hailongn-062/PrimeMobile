package org.example.primemobile.repository;

import org.example.primemobile.entity.ThongSoKyThuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ThongSoKyThuatRepository extends JpaRepository<ThongSoKyThuat, Integer> {

    /** Lấy toàn bộ thông số kỹ thuật của 1 sản phẩm, sắp xếp theo nhóm rồi thứ tự. */
    List<ThongSoKyThuat> findBySanPhamIdOrderByNhomAscThuTuAsc(Integer sanPhamId);

    /** Xóa toàn bộ thông số kỹ thuật của 1 sản phẩm (bulk delete). */
    @Modifying
    @Query("DELETE FROM ThongSoKyThuat t WHERE t.sanPham.id = :sanPhamId")
    void deleteBySanPhamId(@Param("sanPhamId") Integer sanPhamId);
}
