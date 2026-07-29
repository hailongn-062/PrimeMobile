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

    @Query("SELECT y FROM YeuCauBaoHanh y "
        + "LEFT JOIN FETCH y.phieuBaoHanh p "
        + "LEFT JOIN FETCH p.mayDienThoai m "
        + "LEFT JOIN FETCH y.trungTamBaoHanh ttbh "
        + "LEFT JOIN FETCH y.nguoiTiepNhan "
        + "LEFT JOIN FETCH p.khachHang k "
        + "WHERE (:trangThai IS NULL OR y.trangThai = :trangThai) "
        + "AND (:trungTamBhId IS NULL OR ttbh.id = :trungTamBhId) "
        + "AND (:tuNgay IS NULL OR y.ngayTiepNhan >= :tuNgay) "
        + "AND (:denNgay IS NULL OR y.ngayTiepNhan <= :denNgay) "
        + "AND (:tuKhoa IS NULL OR "
        + "    LOWER(p.maPhieu) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR "
        + "    LOWER(m.imei1) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR "
        + "    LOWER(m.imei2) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR "
        + "    LOWER(k.hoTen) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR "
        + "    LOWER(k.soDienThoai) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) "
        + ") "
        + "ORDER BY y.ngayTiepNhan DESC")
    List<YeuCauBaoHanh> timKiemVaLocBaoHanh(
        @org.springframework.data.repository.query.Param("tuKhoa") String tuKhoa,
        @org.springframework.data.repository.query.Param("trangThai") String trangThai,
        @org.springframework.data.repository.query.Param("trungTamBhId") Integer trungTamBhId,
        @org.springframework.data.repository.query.Param("tuNgay") java.time.LocalDateTime tuNgay,
        @org.springframework.data.repository.query.Param("denNgay") java.time.LocalDateTime denNgay
    );
}
