package org.example.primemobile.repository;

import org.example.primemobile.entity.HangSanXuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HangSanXuatRepository extends JpaRepository<HangSanXuat, Integer> {

    /** Tìm hãng theo tên chính xác. */
    Optional<HangSanXuat> findByTenHang(String tenHang);

    /** Kiểm tra tồn tại theo tên, không phân biệt hoa thường — dùng để validate trùng lặp. */
    boolean existsByTenHangIgnoreCase(String tenHang);

    /** Kiểm tra tồn tại theo tên, loại trừ 1 ID — dùng khi cập nhật để bỏ qua chính nó. */
    boolean existsByTenHangIgnoreCaseAndIdNot(String tenHang, Integer id);
}
