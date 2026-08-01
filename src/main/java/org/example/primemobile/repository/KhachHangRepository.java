package org.example.primemobile.repository;

import org.example.primemobile.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

       /**
        * Tìm khách hàng theo số điện thoại.
        * Dùng để lấy tài khoản khách lẻ mặc định (so_dien_thoai = '0000000000')
        * khi bán hàng offline cho khách vãng lai (system_rules.md §2.1).
        */
       Optional<KhachHang> findBySoDienThoai(String soDienThoai);

       /** Tìm khách hàng theo email (không phân biệt hoa thường). */
       Optional<KhachHang> findByEmailIgnoreCase(String email);

       /**
        * Tìm kiếm khách hàng theo từ khóa — áp dụng cho cả SĐT lẫn email.
        * Dùng cho màn hình tra cứu khách hàng tại Admin Dashboard.
        * So sánh LIKE chứa từ khóa ở cả 2 trường.
        * 
        * <p>
        * <b>Lưu ý:</b> Đây là Native Query để xử lý an toàn với tham số NULL
        * (tránh lỗi SQLGrammarException khi CONCAT với NULL trong JPQL).
        * </p>
        *
        * @param tuKhoa Từ khóa tìm kiếm (có thể null).
        * @return Danh sách khách hàng phù hợp.
        */
       @Query(value = "SELECT * FROM khach_hang WHERE " +
                     "(:tuKhoa IS NULL OR " +
                     "(so_dien_thoai LIKE CONCAT('%', :tuKhoa, '%') OR " +
                     "LOWER(email) LIKE LOWER(CONCAT('%', :tuKhoa, '%'))))", nativeQuery = true)
       List<KhachHang> timKiemTheoSdtHoacEmail(@Param("tuKhoa") String tuKhoa);

       /**
        * Tìm bản ghi KhachHang liên kết với NguoiDung theo nguoiDung.id.
        * Dùng trong {@code KhachHangAuthServiceImpl.dangNhap()} để resolve
        * từ NguoiDung → KhachHang sau khi xác thực thành công.
        */
       Optional<KhachHang> findByNguoiDung_Id(Integer nguoiDungId);

       /**
        * Lấy danh sách khách hàng kèm thống kê tổng hợp dùng cho trang quản lý Admin.
        * <p>
        * Dùng Native Query để JOIN an toàn với nguoi_dung (LEFT JOIN) và don_hang
        * (LEFT JOIN + aggregate). Hỗ trợ tìm kiếm theo từ khóa và lọc theo trạng thái.
        *
        * @param tuKhoa    Từ khóa tìm kiếm (ho_ten / so_dien_thoai / email). Null = không lọc.
        * @param trangThai Trạng thái tài khoản ("hoat_dong"/"khoa"/"vang_lai"). Null = tất cả.
        * @return Danh sách {@link KhachHangRowDto} tổng hợp.
        */
       @Query(value = """
               SELECT
                   kh.id              AS id,
                   kh.ho_ten          AS hoTen,
                   kh.email           AS email,
                   kh.so_dien_thoai   AS soDienThoai,
                   nd.id              AS nguoiDungId,
                   nd.ngay_tao        AS ngayThamGia,
                   nd.trang_thai      AS trangThai,
                   ISNULL(SUM(CASE WHEN dh.trang_thai = 'da_hoan_thanh' THEN dh.tong_thanh_toan ELSE 0 END), 0) AS tongChiTieu
               FROM khach_hang kh
               LEFT JOIN nguoi_dung nd ON nd.id = kh.nguoi_dung_id AND nd.vai_tro = 'KhachHang'
               LEFT JOIN don_hang dh  ON dh.khach_hang_id = kh.id
               WHERE
                   (:tuKhoa IS NULL OR
                    kh.ho_ten       LIKE CONCAT('%', :tuKhoa, '%') OR
                    kh.so_dien_thoai LIKE CONCAT('%', :tuKhoa, '%') OR
                    LOWER(kh.email) LIKE LOWER(CONCAT('%', :tuKhoa, '%')))
               AND (
                   :trangThai IS NULL OR
                   (:trangThai = 'vang_lai' AND kh.nguoi_dung_id IS NULL) OR
                   (nd.trang_thai = :trangThai)
               )
               GROUP BY kh.id, kh.ho_ten, kh.email, kh.so_dien_thoai,
                        nd.id, nd.ngay_tao, nd.trang_thai
               ORDER BY kh.id DESC
               """,
               nativeQuery = true)
       List<Object[]> layDanhSachTongHop(@Param("tuKhoa") String tuKhoa,
                                         @Param("trangThai") String trangThai);

       /**
        * Cập nhật trạng thái tài khoản nguoi_dung liên kết với khách hàng.
        * Dùng cho chức năng khóa / mở tài khoản.
        *
        * @param nguoiDungId ID bản ghi nguoi_dung cần cập nhật.
        * @param trangThai   Trạng thái mới ("hoat_dong" hoặc "khoa").
        */
       @Modifying
       @Query(value = "UPDATE nguoi_dung SET trang_thai = :trangThai, updated_at = GETDATE() WHERE id = :nguoiDungId",
               nativeQuery = true)
       void capNhatTrangThaiTaiKhoan(@Param("nguoiDungId") Integer nguoiDungId,
                                      @Param("trangThai") String trangThai);

       /**
        * Lấy [nguoi_dung_id, trang_thai] của tài khoản gắn với khách hàng.
        * Trả về null nếu khách chưa có tài khoản (nguoi_dung_id IS NULL).
        *
        * @param khachHangId ID khách hàng (bảng khach_hang).
        * @return Object[2]: [nguoiDungId (Integer), trangThai (String)] hoặc null.
        */
       @Query(value = """
               SELECT nd.id, nd.trang_thai
               FROM khach_hang kh
               LEFT JOIN nguoi_dung nd ON nd.id = kh.nguoi_dung_id
               WHERE kh.id = :khachHangId
               """, nativeQuery = true)
       List<Object[]> layTrangThaiTaiKhoan(@Param("khachHangId") Integer khachHangId);
}