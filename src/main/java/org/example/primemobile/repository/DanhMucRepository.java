package org.example.primemobile.repository;

import org.example.primemobile.entity.DanhMuc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DanhMucRepository extends JpaRepository<DanhMuc, Integer> {

    /** Tìm danh mục theo tên chính xác (phân biệt hoa thường theo collation DB). */
    Optional<DanhMuc> findByTenDanhMuc(String tenDanhMuc);

    /** Kiểm tra tồn tại theo tên, không phân biệt hoa thường — dùng để validate trùng lặp. */
    boolean existsByTenDanhMucIgnoreCase(String tenDanhMuc);

    /** Kiểm tra tồn tại theo tên, loại trừ 1 ID — dùng khi cập nhật để bỏ qua chính nó. */
    boolean existsByTenDanhMucIgnoreCaseAndIdNot(String tenDanhMuc, Integer id);

    /** Kiểm tra tồn tại theo slug — bảo vệ unique constraint bảng DB. */
    boolean existsBySlug(String slug);

    /** Kiểm tra tồn tại theo slug, loại trừ 1 ID — dùng khi cập nhật. */
    boolean existsBySlugAndIdNot(String slug, Integer id);
}
