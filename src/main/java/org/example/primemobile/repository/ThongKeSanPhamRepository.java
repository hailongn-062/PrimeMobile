package org.example.primemobile.repository;

import org.example.primemobile.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThongKeSanPhamRepository extends JpaRepository<SanPham, Integer> {

    @Query(value = "SELECT COUNT(id) FROM san_pham WHERE trang_thai = 'dang_ban'", nativeQuery = true)
    int countActiveProducts();

    @Query(value = """
        SELECT COALESCE(SUM(ct.so_luong), 0)
        FROM chi_tiet_don_hang ct
        JOIN don_hang d ON ct.don_hang_id = d.id
        WHERE d.trang_thai = 'da_hoan_thanh' 
          AND d.ngay_dat >= :start AND d.ngay_dat < :end
    """, nativeQuery = true)
    int countProductsSold(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT COUNT(DISTINCT sp.id)
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban' AND tk.so_luong > 0 AND tk.so_luong < :threshold
    """, nativeQuery = true)
    int countLowStockProducts(@Param("threshold") int threshold);

    @Query(value = """
        SELECT COUNT(DISTINCT sp.id)
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban' AND tk.so_luong > 0
          AND NOT EXISTS (
              SELECT 1 FROM chi_tiet_don_hang ct
              JOIN don_hang d ON ct.don_hang_id = d.id
              WHERE ct.bien_the_san_pham_id = bt.id
                AND d.ngay_dat >= :dateThreshold
          )
    """, nativeQuery = true)
    int countOldStockProducts(@Param("dateThreshold") LocalDateTime dateThreshold);

    interface TopSanPhamProjection {
        Integer getSanPhamId();
        String getTenSanPham();
        String getHinhAnh();
        Integer getSoLuongBan();
        BigDecimal getDoanhThu();
    }

    @Query(value = """
        SELECT 
            sp.id as sanPhamId,
            sp.ten_san_pham as tenSanPham,
            (SELECT TOP 1 ha.duong_dan 
             FROM hinh_anh_san_pham ha 
             JOIN bien_the_san_pham bt2 ON ha.bien_the_san_pham_id = bt2.id 
             WHERE bt2.san_pham_id = sp.id AND ha.la_anh_chinh = 1 
             ORDER BY ha.id ASC) as hinhAnh,
            SUM(ct.so_luong) as soLuongBan,
            SUM(ct.so_luong * ct.don_gia_ban) as doanhThu
        FROM chi_tiet_don_hang ct
        JOIN don_hang d ON ct.don_hang_id = d.id
        JOIN bien_the_san_pham bt ON ct.bien_the_san_pham_id = bt.id
        JOIN san_pham sp ON bt.san_pham_id = sp.id
        WHERE d.trang_thai = 'da_hoan_thanh' 
          AND d.ngay_dat >= :start AND d.ngay_dat < :end
        GROUP BY sp.id, sp.ten_san_pham
        ORDER BY 
            CASE WHEN :sortBy = 'doanhThu' THEN SUM(ct.so_luong * ct.don_gia_ban) END DESC,
            CASE WHEN :sortBy = 'soLuong' THEN SUM(ct.so_luong) END DESC
    """, countQuery = """
        SELECT COUNT(DISTINCT sp.id)
        FROM chi_tiet_don_hang ct
        JOIN don_hang d ON ct.don_hang_id = d.id
        JOIN bien_the_san_pham bt ON ct.bien_the_san_pham_id = bt.id
        JOIN san_pham sp ON bt.san_pham_id = sp.id
        WHERE d.trang_thai = 'da_hoan_thanh' 
          AND d.ngay_dat >= :start AND d.ngay_dat < :end
    """, nativeQuery = true)
    Page<TopSanPhamProjection> getTopSellingProducts(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("sortBy") String sortBy, Pageable pageable);

    interface DanhMucProjection {
        String getTenDanhMuc();
        Integer getSoLuongBan();
        BigDecimal getDoanhThu();
    }

    @Query(value = """
        SELECT 
            dm.ten_danh_muc as tenDanhMuc,
            SUM(ct.so_luong) as soLuongBan,
            SUM(ct.so_luong * ct.don_gia_ban) as doanhThu
        FROM chi_tiet_don_hang ct
        JOIN don_hang d ON ct.don_hang_id = d.id
        JOIN bien_the_san_pham bt ON ct.bien_the_san_pham_id = bt.id
        JOIN san_pham sp ON bt.san_pham_id = sp.id
        JOIN danh_muc dm ON sp.danh_muc_id = dm.id
        WHERE d.trang_thai = 'da_hoan_thanh' 
          AND d.ngay_dat >= :start AND d.ngay_dat < :end
        GROUP BY dm.id, dm.ten_danh_muc
        ORDER BY doanhThu DESC
    """, nativeQuery = true)
    List<DanhMucProjection> getCategoryDistribution(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    interface BienTheProjection {
        Integer getBienTheId();
        String getTenBienThe();
        Integer getSoLuongBan();
        BigDecimal getDoanhThu();
        Integer getTonKho();
    }

    @Query(value = """
        SELECT 
            bt.id as bienTheId,
            ms.ten_mau + ' - ' + CAST(bt.luu_tru_gb AS VARCHAR) + 'GB' as tenBienThe,
            COALESCE(SUM(CASE WHEN d.trang_thai = 'da_hoan_thanh' AND d.ngay_dat >= :start AND d.ngay_dat < :end THEN ct.so_luong ELSE 0 END), 0) as soLuongBan,
            COALESCE(SUM(CASE WHEN d.trang_thai = 'da_hoan_thanh' AND d.ngay_dat >= :start AND d.ngay_dat < :end THEN ct.so_luong * ct.don_gia_ban ELSE 0 END), 0) as doanhThu,
            COALESCE(tk.so_luong, 0) as tonKho
        FROM bien_the_san_pham bt
        LEFT JOIN mau_sac ms ON bt.mau_sac_id = ms.id
        LEFT JOIN chi_tiet_don_hang ct ON ct.bien_the_san_pham_id = bt.id
        LEFT JOIN don_hang d ON ct.don_hang_id = d.id
        LEFT JOIN ton_kho tk ON tk.bien_the_san_pham_id = bt.id
        WHERE bt.san_pham_id = :sanPhamId
        GROUP BY bt.id, ms.ten_mau, bt.luu_tru_gb, tk.so_luong
        ORDER BY soLuongBan DESC
    """, nativeQuery = true)
    List<BienTheProjection> getVariantStats(@Param("sanPhamId") Integer sanPhamId, @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    interface TonKhoWarningProjection {
        Integer getSanPhamId();
        String getTenSanPham();
        Integer getTonKho();
        Integer getSoLuongBan30Ngay();
        LocalDateTime getNgayBanCuoi();
    }

    @Query(value = """
        SELECT 
            sp.id as sanPhamId,
            sp.ten_san_pham as tenSanPham,
            SUM(tk.so_luong) as tonKho,
            (SELECT COALESCE(SUM(ct.so_luong), 0)
             FROM chi_tiet_don_hang ct
             JOIN don_hang d ON ct.don_hang_id = d.id
             JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
             WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh' 
               AND d.ngay_dat >= :last30Days) as soLuongBan30Ngay,
            (SELECT MAX(d.ngay_dat)
             FROM chi_tiet_don_hang ct
             JOIN don_hang d ON ct.don_hang_id = d.id
             JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
             WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') as ngayBanCuoi
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban'
        GROUP BY sp.id, sp.ten_san_pham
        HAVING SUM(tk.so_luong) > 0 AND SUM(tk.so_luong) < :threshold
        ORDER BY SUM(tk.so_luong) ASC
    """, countQuery = """
        SELECT COUNT(DISTINCT sp.id)
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban'
        GROUP BY sp.id, sp.ten_san_pham
        HAVING SUM(tk.so_luong) > 0 AND SUM(tk.so_luong) < :threshold
    """, nativeQuery = true)
    Page<TonKhoWarningProjection> getLowStockWarnings(@Param("threshold") int threshold, @Param("last30Days") LocalDateTime last30Days, Pageable pageable);

    @Query(value = """
        SELECT 
            sp.id as sanPhamId,
            sp.ten_san_pham as tenSanPham,
            SUM(tk.so_luong) as tonKho,
            (SELECT COALESCE(SUM(ct.so_luong), 0)
             FROM chi_tiet_don_hang ct
             JOIN don_hang d ON ct.don_hang_id = d.id
             JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
             WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh' 
               AND d.ngay_dat >= :last30Days) as soLuongBan30Ngay,
            (SELECT MAX(d.ngay_dat)
             FROM chi_tiet_don_hang ct
             JOIN don_hang d ON ct.don_hang_id = d.id
             JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
             WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') as ngayBanCuoi
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban'
        GROUP BY sp.id, sp.ten_san_pham
        HAVING SUM(tk.so_luong) > 0 
           AND (SELECT MAX(d.ngay_dat)
                FROM chi_tiet_don_hang ct
                JOIN don_hang d ON ct.don_hang_id = d.id
                JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
                WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') < :dateThreshold
           OR (SELECT MAX(d.ngay_dat)
                FROM chi_tiet_don_hang ct
                JOIN don_hang d ON ct.don_hang_id = d.id
                JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
                WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') IS NULL
        ORDER BY ngayBanCuoi ASC
    """, countQuery = """
        SELECT COUNT(DISTINCT sp.id)
        FROM san_pham sp
        JOIN bien_the_san_pham bt ON sp.id = bt.san_pham_id
        JOIN ton_kho tk ON bt.id = tk.bien_the_san_pham_id
        WHERE sp.trang_thai = 'dang_ban'
        GROUP BY sp.id, sp.ten_san_pham
        HAVING SUM(tk.so_luong) > 0 
           AND (SELECT MAX(d.ngay_dat)
                FROM chi_tiet_don_hang ct
                JOIN don_hang d ON ct.don_hang_id = d.id
                JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
                WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') < :dateThreshold
           OR (SELECT MAX(d.ngay_dat)
                FROM chi_tiet_don_hang ct
                JOIN don_hang d ON ct.don_hang_id = d.id
                JOIN bien_the_san_pham bt2 ON ct.bien_the_san_pham_id = bt2.id
                WHERE bt2.san_pham_id = sp.id AND d.trang_thai = 'da_hoan_thanh') IS NULL
    """, nativeQuery = true)
    Page<TonKhoWarningProjection> getOldStockWarnings(@Param("dateThreshold") LocalDateTime dateThreshold, @Param("last30Days") LocalDateTime last30Days, Pageable pageable);

    @Query(value = """
        SELECT TOP 1 ctpn.don_gia_nhap
        FROM chi_tiet_phieu_nhap ctpn
        JOIN bien_the_san_pham bt ON ctpn.bien_the_san_pham_id = bt.id
        WHERE bt.san_pham_id = :sanPhamId
        ORDER BY ctpn.id DESC
    """, nativeQuery = true)
    BigDecimal getLatestImportPriceBySanPhamId(@Param("sanPhamId") Integer sanPhamId);

    @Query("SELECT new org.example.primemobile.dto.common.DropdownDTO(s.id, s.tenSanPham) FROM SanPham s WHERE s.trangThai = 'dang_ban' ORDER BY s.tenSanPham ASC")
    List<org.example.primemobile.dto.common.DropdownDTO> getAllSanPhamDropdown();
}
