package org.example.primemobile.repository;

import org.example.primemobile.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    /**
     * Tìm giỏ hàng theo khachHangId (dành cho khách đã đăng nhập).
     * Eager-load chiTietGioHangs để tránh N+1 query khi hiển thị giỏ.
     */
    @Query("SELECT g FROM GioHang g LEFT JOIN FETCH g.chiTietGioHangs ctgh " +
           "LEFT JOIN FETCH ctgh.bienTheSanPham " +
           "WHERE g.khachHang.id = :khachHangId")
    Optional<GioHang> findByKhachHangIdWithDetails(@Param("khachHangId") Integer khachHangId);

    /**
     * Tìm giỏ hàng theo sessionId (dành cho khách vãng lai chưa đăng nhập).
     * Eager-load tương tự để phục vụ layGioHang().
     */
    @Query("SELECT g FROM GioHang g LEFT JOIN FETCH g.chiTietGioHangs ctgh " +
           "LEFT JOIN FETCH ctgh.bienTheSanPham " +
           "WHERE g.sessionId = :sessionId")
    Optional<GioHang> findBySessionIdWithDetails(@Param("sessionId") String sessionId);

    /** Tìm giỏ hàng theo khachHangId — không load chi tiết (dùng cho kiểm tra tồn tại). */
    Optional<GioHang> findByKhachHangId(Integer khachHangId);

    /** Tìm giỏ hàng theo sessionId — không load chi tiết (dùng cho kiểm tra tồn tại). */
    Optional<GioHang> findBySessionId(String sessionId);
}
