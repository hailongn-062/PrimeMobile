package org.example.primemobile.repository;

import org.example.primemobile.entity.PhieuChuyenKho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhieuChuyenKhoRepository extends JpaRepository<PhieuChuyenKho, Integer> {

    /**
     * Lấy danh sách phiếu chuyển kho có phân trang, JOIN FETCH kho nguồn,
     * kho đích và người tạo để tránh N+1 query.
     */
    @Query(value = """
            SELECT p FROM PhieuChuyenKho p
            LEFT JOIN FETCH p.khoNguon
            LEFT JOIN FETCH p.khoDich
            LEFT JOIN FETCH p.nguoiTao
            """,
           countQuery = "SELECT COUNT(p) FROM PhieuChuyenKho p")
    Page<PhieuChuyenKho> layDanhSachPhanTrang(Pageable pageable);

    /**
     * Lấy chi tiết 1 phiếu chuyển kho kèm toàn bộ dòng chi tiết (eager-load).
     * Tránh N+1 khi nhân viên mở màn hình chi tiết phiếu.
     */
    @Query("""
            SELECT p FROM PhieuChuyenKho p
            LEFT JOIN FETCH p.khoNguon
            LEFT JOIN FETCH p.khoDich
            LEFT JOIN FETCH p.nguoiTao
            LEFT JOIN FETCH p.chiTietChuyenKhos ct
            LEFT JOIN FETCH ct.bienTheSanPham bt
            LEFT JOIN FETCH bt.sanPham
            WHERE p.id = :id
            """)
    Optional<PhieuChuyenKho> findByIdWithDetails(@Param("id") Integer id);
}
