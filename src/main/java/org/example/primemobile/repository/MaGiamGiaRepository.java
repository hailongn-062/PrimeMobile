package org.example.primemobile.repository;

import org.example.primemobile.entity.MaGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MaGiamGiaRepository extends JpaRepository<MaGiamGia, Integer> {

    /** Tìm mã giảm giá theo maCode (duy nhất), eager-load chương trình KM cha. */
    @Query("""
            SELECT m FROM MaGiamGia m
            JOIN FETCH m.chuongTrinhKhuyenMai ctkm
            WHERE m.maCode = :maCode
            """)
    Optional<MaGiamGia> findByMaCodeWithCtkm(@Param("maCode") String maCode);

    /** Kiểm tra maCode đã tồn tại chưa (dùng khi sinh mã hàng loạt). */
    boolean existsByMaCode(String maCode);

    /** Lấy danh sách mã giảm giá theo ID chương trình KM. */
    java.util.List<MaGiamGia> findByChuongTrinhKhuyenMaiId(Integer ctkmId);
}
