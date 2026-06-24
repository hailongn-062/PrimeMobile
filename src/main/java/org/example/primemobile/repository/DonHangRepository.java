package org.example.primemobile.repository;

import org.example.primemobile.entity.DonHang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DonHangRepository extends JpaRepository<DonHang, Integer> {

        /**
         * Lấy danh sách đơn hàng có phân trang, hỗ trợ lọc đồng thời theo:
         * - trangThai (NULL = lấy tất cả)
         * - maDonHang (NULL = bỏ qua, tìm LIKE chứa chuỗi)
         * - soDienThoai của khách hàng (NULL = bỏ qua, tìm LIKE)
         * Kết quả sắp xếp mặc định theo ngayDat DESC (truyền qua Pageable).
         */
        @Query(value = """
                        SELECT d FROM DonHang d
                        JOIN FETCH d.khachHang kh
                        WHERE (:trangThai IS NULL OR d.trangThai = :trangThai)
                          AND (:maDonHang  IS NULL OR d.maDonHang  LIKE CONCAT('%', :maDonHang,  '%'))
                          AND (:soDienThoai IS NULL OR kh.soDienThoai LIKE CONCAT('%', :soDienThoai, '%'))
                        """, countQuery = """
                        SELECT COUNT(d) FROM DonHang d
                        JOIN d.khachHang kh
                        WHERE (:trangThai IS NULL OR d.trangThai = :trangThai)
                          AND (:maDonHang  IS NULL OR d.maDonHang  LIKE CONCAT('%', :maDonHang,  '%'))
                          AND (:soDienThoai IS NULL OR kh.soDienThoai LIKE CONCAT('%', :soDienThoai, '%'))
                        """)
        Page<DonHang> timKiemDonHang(
                        @Param("trangThai") String trangThai,
                        @Param("maDonHang") String maDonHang,
                        @Param("soDienThoai") String soDienThoai,
                        Pageable pageable);

        /**
         * Lấy chi tiết 1 đơn hàng kèm eager-load ChiTietDonHang và BienTheSanPham.
         * Dùng để hiển thị màn hình chi tiết đơn hàng cho nhân viên (tránh N+1).
         */
        @Query("""
                        SELECT d FROM DonHang d
                        LEFT JOIN FETCH d.chiTietDonHangs ctdh
                        LEFT JOIN FETCH ctdh.bienTheSanPham bt
                        LEFT JOIN FETCH bt.sanPham
                        LEFT JOIN FETCH d.khachHang
                        WHERE d.id = :donHangId
                        """)
        Optional<DonHang> findByIdWithDetails(@Param("donHangId") Integer donHangId);

        /**
         * Tìm đơn hàng theo mã đơn hàng (duy nhất).
         */
        Optional<DonHang> findByMaDonHang(String maDonHang);
}
