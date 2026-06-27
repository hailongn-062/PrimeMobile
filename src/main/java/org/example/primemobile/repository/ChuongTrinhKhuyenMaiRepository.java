package org.example.primemobile.repository;

import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

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

    /** Lấy tất cả chương trình, sắp xếp mới nhất lên đầu. */
    List<ChuongTrinhKhuyenMai> findAllByOrderByIdDesc();

    /**
     * Kích hoạt các chương trình khuyến mãi đã đến ngày bắt đầu.
     * Cập nhật {@code trangThai = 'dang_dien_ra'} cho những bản ghi
     * có {@code trangThai = 'chua_bat_dau'} VÀ {@code ngayBatDau <= CURRENT_TIMESTAMP}.
     *
     * @return số dòng được cập nhật
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChuongTrinhKhuyenMai c
            SET c.trangThai = 'dang_dien_ra'
            WHERE c.trangThai = 'chua_bat_dau'
              AND c.ngayBatDau <= CURRENT_TIMESTAMP
            """)
    int kichHoatKhuyenMai();

    /**
     * Kết thúc các chương trình khuyến mãi đã quá ngày kết thúc.
     * Cập nhật {@code trangThai = 'da_ket_thuc'} cho những bản ghi
     * có {@code trangThai = 'dang_dien_ra'} VÀ {@code ngayKetThuc <= CURRENT_TIMESTAMP}.
     *
     * @return số dòng được cập nhật
     */
    @Modifying
    @Transactional
    @Query("""
            UPDATE ChuongTrinhKhuyenMai c
            SET c.trangThai = 'da_ket_thuc'
            WHERE c.trangThai = 'dang_dien_ra'
              AND c.ngayKetThuc <= CURRENT_TIMESTAMP
            """)
    int ketThucKhuyenMai();

    /**
     * Lấy tất cả CTKM đang diễn ra, áp dụng cho TOÀN BỘ ĐƠN HÀNG.
     * <p>
     * Bao gồm 2 loại: {@code "phan_tram"} (giảm % không điều kiện)
     * và {@code "don_hang_toi_thieu"} (giảm % khi đạt ngưỡng tối thiểu).
     * Dùng cho engine tự động chọn khuyến mãi tốt nhất ở POS (Bán hàng tại quầy).
     *
     * @return Danh sách CTKM hợp lệ, sắp xếp theo giaTriUuDai DESC để tối ưu stream.
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            WHERE c.trangThai = 'dang_dien_ra'
              AND c.loai IN ('phan_tram', 'don_hang_toi_thieu')
            ORDER BY c.giaTriUuDai DESC
            """)
    List<ChuongTrinhKhuyenMai> layKhuyenMaiApDungToanDonHang();
}
