package org.example.primemobile.repository;

import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface ChuongTrinhKhuyenMaiRepository extends JpaRepository<ChuongTrinhKhuyenMai, Integer> {

    /**
     * Lấy các chương trình khuyến mãi đang diễn ra (tất cả loại).
     * Dùng cho public API để hiển thị banner, coupon.
     * Loại hợp lệ: 'theo_don_hang' | 'theo_san_pham'
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            WHERE c.loai IN ('theo_don_hang', 'theo_san_pham')
              AND c.trangThai = 'dang_dien_ra'
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
     * Lấy tất cả CTKM loại 'theo_don_hang' đang diễn ra.
     * Dùng cho engine tự động chọn khuyến mãi tốt nhất ở POS (Bán hàng tại quầy).
     * Bao gồm cả chương trình có điều kiện đơn hàng tối thiểu (donHangToiThieu).
     *
     * @return Danh sách CTKM hợp lệ, sắp xếp theo giaTriUuDai DESC để tối ưu stream.
     */
    @Query("""
            SELECT c FROM ChuongTrinhKhuyenMai c
            WHERE c.trangThai = 'dang_dien_ra'
              AND c.loai = 'theo_don_hang'
            ORDER BY c.giaTriUuDai DESC
            """)
    List<ChuongTrinhKhuyenMai> layKhuyenMaiApDungToanDonHang();
}
