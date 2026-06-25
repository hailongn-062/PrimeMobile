package org.example.primemobile.repository;

import org.example.primemobile.entity.PhieuNhapKho;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PhieuNhapKhoRepository extends JpaRepository<PhieuNhapKho, Integer> {

    /**
     * Lấy danh sách phiếu nhập kho có phân trang, JOIN FETCH kho và NCC
     * để tránh N+1. Sắp xếp mặc định mới nhất lên đầu qua Pageable.
     */
    @Query(value = """
            SELECT p FROM PhieuNhapKho p
            LEFT JOIN FETCH p.kho
            LEFT JOIN FETCH p.nhaCungCap
            LEFT JOIN FETCH p.nguoiTao
            """,
           countQuery = "SELECT COUNT(p) FROM PhieuNhapKho p")
    Page<PhieuNhapKho> layDanhSachPhanTrang(Pageable pageable);

    /**
     * Lấy chi tiết 1 phiếu nhập kho kèm toàn bộ chi tiết dòng (eager-load).
     * Tránh N+1 khi nhân viên mở màn hình chi tiết phiếu.
     */
    @Query("""
            SELECT p FROM PhieuNhapKho p
            LEFT JOIN FETCH p.kho
            LEFT JOIN FETCH p.nhaCungCap
            LEFT JOIN FETCH p.nguoiTao
            LEFT JOIN FETCH p.chiTietPhieuNhaps ct
            LEFT JOIN FETCH ct.bienTheSanPham bt
            LEFT JOIN FETCH bt.sanPham
            WHERE p.id = :id
            """)
    Optional<PhieuNhapKho> findByIdWithDetails(@Param("id") Integer id);
}
