package org.example.primemobile.repository;

import org.example.primemobile.entity.TrungTamBaoHanh;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrungTamBaoHanhRepository extends JpaRepository<TrungTamBaoHanh, Integer> {

    @Query("""
            SELECT t FROM TrungTamBaoHanh t
            WHERE (:trangThai IS NULL OR t.trangThai = :trangThai)
              AND (:tuKhoa IS NULL OR t.tenTrungTam LIKE CONCAT('%', :tuKhoa, '%')
                                   OR t.soDienThoai LIKE CONCAT('%', :tuKhoa, '%')
                                   OR t.nguoiLienHe LIKE CONCAT('%', :tuKhoa, '%'))
            """)
    Page<TrungTamBaoHanh> timKiem(
            @Param("trangThai") String trangThai,
            @Param("tuKhoa")    String tuKhoa,
            Pageable pageable
    );

    /**
     * Tìm trung tâm bảo hành theo tên chính xác.
     *
     * @param tenTrungTam Tên trung tâm bảo hành.
     * @return Optional chứa TrungTamBaoHanh nếu tìm thấy.
     */
    Optional<TrungTamBaoHanh> findByTenTrungTam(String tenTrungTam);

    /**
     * Tìm các trung tâm bảo hành theo trạng thái.
     *
     * @param trangThai Trạng thái (ví dụ: 'hoat_dong' hoặc 'ngung_hoat_dong').
     * @return Danh sách các trung tâm bảo hành có trạng thái tương ứng.
     */
    List<TrungTamBaoHanh> findByTrangThai(String trangThai);

    /**
     * Kiểm tra tồn tại trung tâm bảo hành theo tên (không phân biệt hoa thường).
     *
     * @param tenTrungTam Tên trung tâm bảo hành cần kiểm tra.
     * @return true nếu tồn tại, false nếu không.
     */
    boolean existsByTenTrungTamIgnoreCase(String tenTrungTam);

    /**
     * Kiểm tra tồn tại theo tên, loại trừ một ID nhất định (dùng khi cập nhật).
     *
     * @param tenTrungTam Tên trung tâm bảo hành.
     * @param id          ID cần loại trừ.
     * @return true nếu tồn tại trung tâm khác có tên đó.
     */
    boolean existsByTenTrungTamIgnoreCaseAndIdNot(String tenTrungTam, Integer id);
}