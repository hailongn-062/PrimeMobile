package org.example.primemobile.repository;

import org.example.primemobile.entity.YeuCauBaoHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;

@Repository
public interface YeuCauBaoHanhRepository extends JpaRepository<YeuCauBaoHanh, Integer> {
    List<YeuCauBaoHanh> findByPhieuBaoHanhIdOrderByNgayTiepNhanDesc(Integer phieuBaoHanhId);

    @Query("SELECT y FROM YeuCauBaoHanh y "
        + "LEFT JOIN FETCH y.phieuBaoHanh p "
        + "LEFT JOIN FETCH p.mayDienThoai "
        + "LEFT JOIN FETCH y.trungTamBaoHanh "
        + "LEFT JOIN FETCH y.nguoiTiepNhan "
        + "ORDER BY y.ngayTiepNhan DESC")
    List<YeuCauBaoHanh> findAllWithDetails();

    Page<YeuCauBaoHanh> findAllByOrderByNgayTiepNhanDesc(Pageable pageable);
}
