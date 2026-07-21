package org.example.primemobile.repository;

import org.example.primemobile.entity.DanhGiaSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface DanhGiaSanPhamRepository extends JpaRepository<DanhGiaSanPham, Integer> {

    List<DanhGiaSanPham> findBySanPhamIdAndTrangThaiOrderByNgayTaoDesc(Integer sanPhamId, String trangThai);

    @Query("SELECT AVG(d.sao) FROM DanhGiaSanPham d WHERE d.sanPham.id = :sanPhamId AND d.trangThai = 'da_duyet'")
    Double calculateAverageSaoBySanPhamId(@Param("sanPhamId") Integer sanPhamId);

    @Query("SELECT COUNT(d) FROM DanhGiaSanPham d WHERE d.sanPham.id = :sanPhamId AND d.trangThai = 'da_duyet'")
    Long countBySanPhamIdAndTrangThaiDaDuyet(@Param("sanPhamId") Integer sanPhamId);

    @Query("SELECT d.sanPham.id, AVG(d.sao), COUNT(d) FROM DanhGiaSanPham d WHERE d.sanPham.id IN :sanPhamIds AND d.trangThai = 'da_duyet' GROUP BY d.sanPham.id")
    List<Object[]> getAverageSaoAndCountBySanPhamIds(@Param("sanPhamIds") List<Integer> sanPhamIds);

    boolean existsByKhachHangIdAndDonHangIdAndSanPhamId(Integer khachHangId, Integer donHangId, Integer sanPhamId);

    Page<DanhGiaSanPham> findAllByOrderByNgayTaoDesc(Pageable pageable);
}
