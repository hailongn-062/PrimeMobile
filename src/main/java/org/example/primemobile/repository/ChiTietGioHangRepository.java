package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietGioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ChiTietGioHangRepository extends JpaRepository<ChiTietGioHang, Integer> {

    /**
     * Tìm dòng chi tiết giỏ hàng theo cặp (gioHangId, bienTheSanPhamId).
     * Dùng để kiểm tra SKU đã có trong giỏ chưa (upsert logic).
     * Khớp với UNIQUE constraint uq_ctgh trên bảng DB.
     */
    Optional<ChiTietGioHang> findByGioHangIdAndBienTheSanPhamId(Integer gioHangId, Integer bienTheSanPhamId);
}
