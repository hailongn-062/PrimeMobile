package org.example.primemobile.repository;

import org.example.primemobile.entity.NhaCungCap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NhaCungCapRepository extends JpaRepository<NhaCungCap, Integer> {

    /** Lấy danh sách NCC có phân trang, hỗ trợ lọc theo trangThai và tên. */
    @Query("""
            SELECT n FROM NhaCungCap n
            WHERE (:trangThai IS NULL OR n.trangThai = :trangThai)
              AND (:tuKhoa IS NULL OR n.tenNcc LIKE CONCAT('%', :tuKhoa, '%')
                                  OR n.maNcc  LIKE CONCAT('%', :tuKhoa, '%'))
            """)
    Page<NhaCungCap> timKiem(
            @Param("trangThai") String trangThai,
            @Param("tuKhoa")    String tuKhoa,
            Pageable pageable
    );

    /** Lấy danh sách NCC đang hợp tác — dùng để đổ vào ComboBox tạo Phiếu Nhập. */
    List<NhaCungCap> findByTrangThaiOrderByTenNccAsc(String trangThai);

    /** Kiểm tra maNcc đã tồn tại chưa. */
    boolean existsByMaNcc(String maNcc);
}
