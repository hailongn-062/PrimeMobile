package org.example.primemobile.repository;

import org.example.primemobile.entity.DiaChiKhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Integer> {

    /** Lấy toàn bộ địa chỉ của 1 khách hàng (địa chỉ mặc định lên đầu). */
    @Query("""
            SELECT d FROM DiaChiKhachHang d
            WHERE d.khachHang.id = :khachHangId
            ORDER BY d.macDinh DESC, d.id ASC
            """)
    List<DiaChiKhachHang> findByKhachHangIdOrderByMacDinhDesc(@Param("khachHangId") Integer khachHangId);

    /**
     * Bỏ toàn bộ cờ macDinh của 1 khách hàng về false.
     * Gọi trước khi set địa chỉ mới làm mặc định để đảm bảo chỉ có 1 địa chỉ mặc định.
     */
    @Modifying
    @Query("""
            UPDATE DiaChiKhachHang d SET d.macDinh = false
            WHERE d.khachHang.id = :khachHangId
            """)
    void resetMacDinhByKhachHangId(@Param("khachHangId") Integer khachHangId);

    /** Đếm số địa chỉ của 1 khách hàng (giới hạn tối đa bao nhiêu địa chỉ). */
    long countByKhachHangId(Integer khachHangId);
}
