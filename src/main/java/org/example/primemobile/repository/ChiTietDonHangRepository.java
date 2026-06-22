package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietDonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChiTietDonHangRepository extends JpaRepository<ChiTietDonHang, Integer> {

    /**
     * Lấy danh sách chi tiết của 1 đơn hàng (simple version).
     * Dùng để tính lại tổng tiền hoặc duyệt khi thanh toán.
     */
    List<ChiTietDonHang> findByDonHangId(Integer donHangId);

    /**
     * Lấy chi tiết đơn hàng, eager-load BienTheSanPham để tránh N+1 query
     * khi xử lý thanh toán và trừ kho hàng loạt.
     */
    @Query("""
            SELECT c FROM ChiTietDonHang c
            JOIN FETCH c.bienTheSanPham
            WHERE c.donHang.id = :donHangId
            """)
    List<ChiTietDonHang> findByDonHangIdWithDetails(@Param("donHangId") Integer donHangId);

    /**
     * Kiểm tra một SKU cụ thể có trong đơn hàng chưa.
     * Dùng để quyết định "thêm mới" hay "ạng dồn số lượng" khi gọi themSanPhamVaoDon.
     */
    Optional<ChiTietDonHang> findByDonHangIdAndBienTheSanPhamId(Integer donHangId,
                                                                  Integer bienTheSanPhamId);
}
