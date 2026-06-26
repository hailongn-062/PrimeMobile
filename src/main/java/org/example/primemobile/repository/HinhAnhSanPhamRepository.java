package org.example.primemobile.repository;

import org.example.primemobile.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Integer> {
    
    List<HinhAnhSanPham> findByBienTheSanPhamIdOrderByThuTuAsc(Integer bienTheId);

    @Modifying
    @Query("UPDATE HinhAnhSanPham h SET h.laAnhChinh = false WHERE h.bienTheSanPham.id = :bienTheId")
    void resetAnhChinhByBienTheId(Integer bienTheId);
}
