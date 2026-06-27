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
   * Lấy tất cả IMEI của một biến thể (không lọc theo trạng thái).
   * Dùng cho màn hình POS khi cần hiển thị toàn bộ IMEI của sản phẩm.
   *
   * @param bienTheSanPhamId ID biến thể sản phẩm (SKU) cần lấy.
   * @return Danh sách tất cả {@link MayDienThoai} thuộc biến thể đó.
   */
  List<MayDienThoai> findByBienTheSanPhamId(Integer bienTheSanPhamId);
}