package org.example.primemobile.repository;

import org.example.primemobile.entity.YeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface YeuThichRepository extends JpaRepository<YeuThich, Integer> {
    
    List<YeuThich> findByKhachHangIdOrderByNgayThemDesc(Integer khachHangId);
    
    Optional<YeuThich> findByKhachHangIdAndSanPhamId(Integer khachHangId, Integer sanPhamId);
    
    @org.springframework.data.jpa.repository.Query("SELECT y.sanPham.id FROM YeuThich y WHERE y.khachHang.id = :khachHangId AND y.sanPham.id IN :sanPhamIds")
    List<Integer> findSanPhamIdsByKhachHangIdAndSanPhamIds(@org.springframework.data.repository.query.Param("khachHangId") Integer khachHangId, @org.springframework.data.repository.query.Param("sanPhamIds") List<Integer> sanPhamIds);
    
    boolean existsByKhachHangIdAndSanPhamId(Integer khachHangId, Integer sanPhamId);
    
    void deleteByKhachHangIdAndSanPhamId(Integer khachHangId, Integer sanPhamId);
}
