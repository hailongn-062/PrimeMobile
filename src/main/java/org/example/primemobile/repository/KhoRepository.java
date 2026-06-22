package org.example.primemobile.repository;

import org.example.primemobile.entity.Kho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhoRepository extends JpaRepository<Kho, Integer> {

    /**
     * Tìm kho theo loại ("kho_tong" hoặc "kho_online").
     * Dùng để tự động tìm Kho Tổng mà không cần hardcode ID.
     * Ràng buộc UNIQUE trên cột loai đảm bảo trả về đúng 1 bản ghi.
     */
    Optional<Kho> findByLoai(String loai);
}
