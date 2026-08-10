package org.example.primemobile.repository;

import org.example.primemobile.entity.MauSac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Integer> {
    
    // Tìm màu sắc theo tên (tìm kiếm gần đúng)
    Page<MauSac> findByTenMauContainingIgnoreCase(String tenMau, Pageable pageable);

    // Tìm chính xác tên màu (case-insensitive) - phục vụ validate
    Optional<MauSac> findByTenMauIgnoreCase(String tenMau);
}
