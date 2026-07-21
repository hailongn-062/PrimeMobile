package org.example.primemobile.repository;

import org.example.primemobile.entity.PhieuBaoHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PhieuBaoHanhRepository extends JpaRepository<PhieuBaoHanh, Integer> {
    Optional<PhieuBaoHanh> findByMayDienThoaiImei1AndTrangThai(String imei, String trangThai);
    List<PhieuBaoHanh> findByKhachHangIdOrderByNgayBatDauDesc(Integer khachHangId);

    @Query("SELECT p FROM PhieuBaoHanh p "
        + "JOIN FETCH p.mayDienThoai m "
        + "JOIN FETCH m.bienTheSanPham bts "
        + "JOIN FETCH bts.sanPham sp "
        + "JOIN FETCH p.khachHang kh "
        + "WHERE kh.soDienThoai = :sdt AND p.trangThai = :trangThai")
    List<PhieuBaoHanh> findBySdtWithDetails(@Param("sdt") String sdt, @Param("trangThai") String trangThai);
}
