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

    @Query("SELECT d FROM DanhGiaSanPham d WHERE " +
           "(:tuKhoa IS NULL OR " +
           "  LOWER(d.khachHang.hoTen) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
           "  LOWER(d.khachHang.email) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
           "  LOWER(d.sanPham.tenSanPham) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
           "  LOWER(d.tieuDe) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
           "  LOWER(d.noiDung) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))) AND " +
           "(:trangThai IS NULL OR d.trangThai = :trangThai) AND " +
           "(:sao IS NULL OR d.sao = :sao) AND " +
           "(CAST(:tuNgay AS timestamp) IS NULL OR d.ngayTao >= :tuNgay) AND " +
           "(CAST(:denNgay AS timestamp) IS NULL OR d.ngayTao <= :denNgay)")
    Page<DanhGiaSanPham> timKiemVaLocDanhGia(
            @Param("tuKhoa") String tuKhoa,
            @Param("trangThai") String trangThai,
            @Param("sao") Integer sao,
            @Param("tuNgay") java.time.LocalDateTime tuNgay,
            @Param("denNgay") java.time.LocalDateTime denNgay,
            Pageable pageable);
}
