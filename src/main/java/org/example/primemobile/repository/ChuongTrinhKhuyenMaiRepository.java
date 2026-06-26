package org.example.primemobile.repository;

import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChuongTrinhKhuyenMaiRepository extends JpaRepository<ChuongTrinhKhuyenMai, Integer> {

    /**
     * Lấy danh sách chương trình Flash Sale có phân trang.
     * Lọc {@code loai = 'flash_sale'}, JOIN FETCH chi_tiet_flash_sale để tránh N+1.
     */
    @Query(value = """
            SELECT DISTINCT c FROM ChuongTrinhKhuyenMai c
            LEFT JOIN FETCH c.chiTietFlashSales
            WHERE c.loai = 'flash_sale'
            ORDER BY c.ngayBatDau DESC
            """,
           countQuery = "SELECT COUNT(c) FROM ChuongTrinhKhuyenMai c WHERE c.loai = 'flash_sale'")
    Page<ChuongTrinhKhuyenMai> layDanhSachFlashSale(Pageable pageable);

    /**
     * Lấy tất cả Flash Sale đang diễn ra ({@code trangThai = 'dang_dien_ra'}).
     * Dùng cho public catalog để hiển thị badge giảm giá flash.
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            LEFT JOIN FETCH c.chiTietFlashSales
            WHERE c.loai = 'flash_sale' AND c.trangThai = 'dang_dien_ra'
            """)
    List<ChuongTrinhKhuyenMai> layFlashSaleDangDienRa();

    /**
     * Lấy chi tiết 1 chương trình Flash Sale kèm toàn bộ dòng chi tiết biến thể.
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            LEFT JOIN FETCH c.chiTietFlashSales ct
            LEFT JOIN FETCH ct.bienTheSanPham bt
            LEFT JOIN FETCH bt.sanPham
            WHERE c.id = :id AND c.loai = 'flash_sale'
            """)
    Optional<ChuongTrinhKhuyenMai> findFlashSaleByIdWithDetails(@Param("id") Integer id);

    /**
     * Lấy các chương trình khuyến mãi (không phải flash_sale) đang diễn ra.
     * Dùng cho public API để hiển thị banner, coupon.
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            WHERE c.loai != 'flash_sale' AND c.trangThai = 'dang_dien_ra'
            """)
    List<ChuongTrinhKhuyenMai> layKhuyenMaiDangDienRa();
}
