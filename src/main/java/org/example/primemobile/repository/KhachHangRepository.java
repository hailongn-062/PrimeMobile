package org.example.primemobile.repository;

import org.example.primemobile.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
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
}