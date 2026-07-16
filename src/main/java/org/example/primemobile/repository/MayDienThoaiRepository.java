package org.example.primemobile.repository;

import org.example.primemobile.entity.MayDienThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MayDienThoaiRepository extends JpaRepository<MayDienThoai, Integer> {

    List<MayDienThoai> findByNguoiGiuIdAndDonHangIsNull(Integer nguoiGiuId);
    
    List<MayDienThoai> findByTinhTrangAndThoiGianGiuBeforeAndDonHangIsNull(String tinhTrang, java.time.LocalDateTime time);

    /**
     * Đếm số bản ghi máy điện thoại có trạng thái 'trong_kho'
     * của một SKU cụ thể (toàn hệ thống, không phân biệt kho).
     * Dùng cho hàm kiểm tra số lượng IMEI còn trong kho (system_rules.md §3.3).
     */
    @Query("""
            SELECT COUNT(m) FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.tinhTrang         = 'trong_kho'
            """)
    long countTrongKhoByBienThe(@Param("bienTheSanPhamId") Integer bienTheSanPhamId);

    /**
     * Kiểm tra imei1 đã tồn tại trong database chưa.
     */
    boolean existsByImei1(String imei1);

    /**
     * Kiểm tra imei2 đã tồn tại trong database chưa.
     * Dùng tại Service Layer vì DB dùng Filtered Unique Index (không enforce qua JPA).
     */
    boolean existsByImei2(String imei2);

    /**
     * Tìm MayDienThoai theo imei1 (chính xác).
     * Dùng trong luồng thanh toán POS để mark IMEI là 'da_ban'.
     */
    Optional<MayDienThoai> findByImei1(String imei1);

    /**
     * Lấy danh sách máy còn trong kho của 1 biến thể SKU (toàn hệ thống).
     * Dùng để kiểm tra IMEI nhân viên nhập có thực sự 'trong_kho' không.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheId
              AND m.tinhTrang = 'trong_kho'
            """)
    List<MayDienThoai> findTrongKhoByBienTheId(@Param("bienTheId") Integer bienTheId);

    /**
     * Lấy danh sách IMEI của một biến thể SKU theo trạng thái cụ thể.
     * Dùng cho màn hình POS để hiển thị danh sách IMEI có sẵn cho nhân viên chọn.
     * <p>
     * <b>Lưu ý:</b> Method này yêu cầu {@code tinhTrang} phải khác {@code null}.
     * Nếu muốn lấy tất cả IMEI (không lọc trạng thái), sử dụng
     * {@link #findByBienTheSanPhamId(Integer)}.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
     * @param tinhTrang        Trạng thái IMEI cần lọc (ví dụ: 'trong_kho', 'da_ban').
     *                         <b>Không được null.</b>
     * @return Danh sách {@link MayDienThoai} có trạng thái tương ứng.
     */
    List<MayDienThoai> findByBienTheSanPhamIdAndTinhTrang(Integer bienTheSanPhamId, String tinhTrang);

    /**
     * Lấy tất cả IMEI của một biến thể (không lọc theo trạng thái).
     * Dùng cho màn hình POS khi cần hiển thị toàn bộ IMEI của sản phẩm.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
     * @return Danh sách tất cả {@link MayDienThoai} thuộc biến thể đó.
     */
    List<MayDienThoai> findByBienTheSanPhamId(Integer bienTheSanPhamId);

    /**
     * Lấy danh sách IMEI đã được gán cho một đơn hàng cụ thể với trạng thái xác định.
     * <p>
     * Phương thức này được sử dụng để hiển thị danh sách IMEI trong trang chi tiết đơn hàng,
     * giúp nhân viên kiểm tra và đối soát khi giao hàng.
     * <p>
     * Thông thường, chỉ lấy IMEI có {@code tinhTrang = 'da_ban'} vì khi xác nhận đơn,
     * IMEI đã được chuyển sang trạng thái 'da_ban'.
     *
     * @param donHangId ID đơn hàng cần lấy danh sách IMEI.
     * @param tinhTrang Trạng thái IMEI cần lọc (ví dụ: 'da_ban', 'trong_kho').
     * @return Danh sách {@link MayDienThoai} thuộc đơn hàng đó với trạng thái tương ứng.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.donHang.id = :donHangId
              AND m.tinhTrang = :tinhTrang
            """)
    List<MayDienThoai> findByDonHangIdAndTinhTrang(
            @Param("donHangId") Integer donHangId,
            @Param("tinhTrang") String tinhTrang
    );

    @Query("""
            SELECT m FROM MayDienThoai m 
            WHERE m.donHang.id = :donHangId
            """)
    List<MayDienThoai> findByDonHangId(@Param("donHangId") Integer donHangId);
}