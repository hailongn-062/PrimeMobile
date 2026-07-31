package org.example.primemobile.repository;

import org.example.primemobile.entity.DonHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ThongKeRepository extends JpaRepository<DonHang, Integer> {

    interface ThongKeTongQuan {
        BigDecimal getDoanhThu();
        Integer getTongDon();
        Integer getDonHuy();
    }

    interface ThongKeNgay {
        String getNgay();
        BigDecimal getDoanhThu();
        Integer getSoDon();
        BigDecimal getDoanhThuOnline();
        BigDecimal getDoanhThuOffline();
        Integer getDonHuy();
    }

    interface ThongKeKenh {
        String getKenhBan();
        BigDecimal getDoanhThu();
        Integer getSoDon();
    }

    @Query(value = """
        SELECT 
            SUM(CASE WHEN d.trang_thai = 'da_hoan_thanh' THEN d.tong_thanh_toan ELSE 0 END) as doanhThu,
            COUNT(CASE WHEN d.trang_thai != 'don_hang_cho' THEN 1 ELSE NULL END) as tongDon,
            COUNT(CASE WHEN d.trang_thai = 'da_huy' THEN 1 ELSE NULL END) as donHuy
        FROM don_hang d
        WHERE d.ngay_dat >= :start AND d.ngay_dat < :end
    """, nativeQuery = true)
    ThongKeTongQuan getTongQuan(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            CONVERT(VARCHAR(10), d.ngay_dat, 120) as ngay,
            SUM(CASE WHEN d.trang_thai = 'da_hoan_thanh' THEN d.tong_thanh_toan ELSE 0 END) as doanhThu,
            COUNT(CASE WHEN d.trang_thai != 'don_hang_cho' THEN 1 ELSE NULL END) as soDon,
            SUM(CASE WHEN d.kenh_ban = 'online' AND d.trang_thai = 'da_hoan_thanh' THEN d.tong_thanh_toan ELSE 0 END) as doanhThuOnline,
            SUM(CASE WHEN d.kenh_ban != 'online' AND d.trang_thai = 'da_hoan_thanh' THEN d.tong_thanh_toan ELSE 0 END) as doanhThuOffline,
            COUNT(CASE WHEN d.trang_thai = 'da_huy' THEN 1 ELSE NULL END) as donHuy
        FROM don_hang d
        WHERE d.ngay_dat >= :start AND d.ngay_dat < :end
        GROUP BY CONVERT(VARCHAR(10), d.ngay_dat, 120)
        ORDER BY ngay ASC
    """, nativeQuery = true)
    List<ThongKeNgay> getThongKeTheoNgay(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
        SELECT 
            d.kenh_ban as kenhBan,
            SUM(CASE WHEN d.trang_thai = 'da_hoan_thanh' THEN d.tong_thanh_toan ELSE 0 END) as doanhThu,
            COUNT(CASE WHEN d.trang_thai != 'don_hang_cho' THEN 1 ELSE NULL END) as soDon
        FROM don_hang d
        WHERE d.ngay_dat >= :start AND d.ngay_dat < :end
        GROUP BY d.kenh_ban
    """, nativeQuery = true)
    List<ThongKeKenh> getThongKeTheoKenh(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
