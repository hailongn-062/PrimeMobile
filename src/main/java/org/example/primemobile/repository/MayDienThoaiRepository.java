package org.example.primemobile.repository;

import org.example.primemobile.entity.MayDienThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MayDienThoaiRepository extends JpaRepository<MayDienThoai, Integer> {

    /**
     * Đếm số bản ghi máy điện thoại có trạng thái 'trong_kho'
     * của một SKU cụ thể.
     * <p>
     * Lưu ý: Bảng may_dien_thoai không có cột kho_id (xem schema SQL).
     * IMEI gắn với biến thể SKU, không gắn trực tiếp với kho.
     * Hàm này đếm TOÀN BỘ IMEI 'trong_kho' của SKU đó trong hệ thống.
     * Service layer sẽ lấy số liệu này so sánh với ton_kho của kho cụ thể.
     * Dùng cho hàm tinhSoLuongImeiCanThem (system_rules.md §3.3).
     */
    @Query("""
            SELECT COUNT(m) FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.tinhTrang         = 'trong_kho'
            """)
    long countTrongKhoByBienThe(@Param("bienTheSanPhamId") Integer bienTheSanPhamId);

    /** Kiểm tra imei1 đã tồn tại trong database chưa. */
    boolean existsByImei1(String imei1);

    /** Kiểm tra imei2 đã tồn tại trong database chưa. */
    boolean existsByImei2(String imei2);

    /** Kiểm tra serial đã tồn tại trong database chưa. */
    boolean existsBySerial(String serial);
}


