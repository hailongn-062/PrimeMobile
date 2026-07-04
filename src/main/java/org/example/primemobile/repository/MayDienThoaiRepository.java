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

    /**
     * Đếm số bản ghi máy điện thoại có trạng thái 'trong_kho'
     * của một SKU cụ thể.
     * <p>
     * Lưu ý: Hàm này đếm TOÀN BỘ IMEI 'trong_kho' của SKU đó trong hệ thống
     * (không phân biệt kho). Service layer sẽ so sánh với ton_kho của kho cụ thể.
     * Dùng cho hàm tinhSoLuongImeiCanThem (system_rules.md §3.3).
     */
    @Query("""
            SELECT COUNT(m) FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.tinhTrang         = 'trong_kho'
            """)
    long countTrongKhoByBienThe(@Param("bienTheSanPhamId") Integer bienTheSanPhamId);

    /**
     * Đếm số bản ghi máy điện thoại có trạng thái 'trong_kho'
     * của một SKU cụ thể tại một kho cụ thể.
     * <p>
     * Khác với {@link #countTrongKhoByBienThe(Integer)}, hàm này đếm IMEI
     * theo từng kho riêng biệt, phục vụ cho việc quản lý IMEI độc lập
     * giữa các kho (VD: Kho Tổng và Kho Online có thể có IMEI riêng
     * cho cùng một SKU mà không ảnh hưởng lẫn nhau).
     * <p>
     * Dùng trong {@link org.example.primemobile.service.impl.MayDienThoaiServiceImpl#tinhSoLuongImeiCanThem(Integer, Integer)}
     * để tính số IMEI còn thiếu tại một kho cụ thể.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU)
     * @param khoId ID kho cần đếm
     * @return Số lượng IMEI đang trong kho của SKU đó
     */
    @Query("""
            SELECT COUNT(m) FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.tinhTrang = 'trong_kho'
              AND m.kho.id = :khoId
            """)
    long countTrongKhoByBienTheAndKho(
            @Param("bienTheSanPhamId") Integer bienTheSanPhamId,
            @Param("khoId") Integer khoId
    );

    /**
     * Kiểm tra imei1 đã tồn tại trong database chưa.
     */
    boolean existsByImei1(String imei1);

    /**
     * Kiểm tra imei2 đã tồn tại trong database chưa.
     */
    boolean existsByImei2(String imei2);

    /**
     * Kiểm tra serial đã tồn tại trong database chưa.
     */
    boolean existsBySerial(String serial);

    /**
     * Tìm MayDienThoai theo imei1 (chính xác).
     * Dùng trong luồng thanh toán POS để mark IMEI là 'da_ban'.
     */
    Optional<MayDienThoai> findByImei1(String imei1);

    /**
     * Lấy danh sách máy còn trong kho của 1 biến thể SKU.
     * Dùng để kiểm tra IMEI nhân viên nhập có thực sự 'trong_kho' không.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheId
              AND m.tinhTrang = 'trong_kho'
            """)
    List<MayDienThoai> findTrongKhoByBienTheId(@Param("bienTheId") Integer bienTheId);

    /**
     * Lấy danh sách máy còn trong kho của 1 biến thể SKU theo kho cụ thể.
     * Dùng để lọc IMEI theo kho (ví dụ chỉ lấy IMEI ở Kho Online).
     *
     * @param bienTheId ID biến thể sản phẩm.
     * @param khoId     ID kho cần lọc.
     * @return Danh sách IMEI đang 'trong_kho' tại kho đó.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheId
              AND m.tinhTrang = 'trong_kho'
              AND m.kho.id = :khoId
            """)
    List<MayDienThoai> findTrongKhoByBienTheIdAndKhoId(@Param("bienTheId") Integer bienTheId,
                                                       @Param("khoId") Integer khoId);

    /**
     * Lấy danh sách IMEI của một biến thể SKU theo trạng thái cụ thể.
     * Dùng cho màn hình POS để hiển thị danh sách IMEI có sẵn cho nhân viên chọn.
     * <p>
     * <b>Lưu ý:</b> Method này yêu cầu {@code tinhTrang} phải khác {@code null}.
     * Nếu muốn lấy tất cả IMEI (không lọc trạng thái), sử dụng
     * {@link #findByBienTheSanPhamId(Integer)}.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
     * @param tinhTrang        Trạng thái IMEI cần lọc (ví dụ: 'trong_kho',
     *                         'da_ban', ...).
     *                         <b>Không được null.</b>
     * @return Danh sách {@link MayDienThoai} có trạng thái tương ứng.
     */
    List<MayDienThoai> findByBienTheSanPhamIdAndTinhTrang(Integer bienTheSanPhamId, String tinhTrang);

    /**
     * Lấy danh sách IMEI của một biến thể SKU theo trạng thái và kho cụ thể.
     * Dùng khi xác nhận đơn online để chỉ chọn IMEI ở Kho Online.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
     * @param tinhTrang        Trạng thái IMEI cần lọc (ví dụ: 'trong_kho').
     * @param khoId            ID kho cần lọc.
     * @return Danh sách {@link MayDienThoai} có trạng thái và kho tương ứng.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.tinhTrang = :tinhTrang
              AND m.kho.id = :khoId
            """)
    List<MayDienThoai> findByBienTheSanPhamIdAndTinhTrangAndKhoId(
            @Param("bienTheSanPhamId") Integer bienTheSanPhamId,
            @Param("tinhTrang") String tinhTrang,
            @Param("khoId") Integer khoId);

    /**
     * Lấy tất cả IMEI của một biến thể (không lọc theo trạng thái).
     * Dùng cho màn hình POS khi cần hiển thị toàn bộ IMEI của sản phẩm.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
     * @return Danh sách tất cả {@link MayDienThoai} thuộc biến thể đó.
     */
    List<MayDienThoai> findByBienTheSanPhamId(Integer bienTheSanPhamId);

    /**
     * Lấy danh sách IMEI của một biến thể theo kho (không lọc trạng thái).
     * Dùng khi không cần lọc theo tinhTrang.
     *
     * @param bienTheSanPhamId ID biến thể sản phẩm.
     * @param khoId            ID kho cần lọc.
     * @return Danh sách {@link MayDienThoai} thuộc biến thể và kho đó.
     */
    @Query("""
            SELECT m FROM MayDienThoai m
            WHERE m.bienTheSanPham.id = :bienTheSanPhamId
              AND m.kho.id = :khoId
            """)
    List<MayDienThoai> findByBienTheSanPhamIdAndKhoId(
            @Param("bienTheSanPhamId") Integer bienTheSanPhamId,
            @Param("khoId") Integer khoId);

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
}