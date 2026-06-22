package org.example.primemobile.repository;

import org.example.primemobile.entity.KhachHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KhachHangRepository extends JpaRepository<KhachHang, Integer> {

    /**
     * Tìm khách hàng theo số điện thoại.
     * Dùng để lấy tài khoản khách lẻ mặc định (so_dien_thoai = '0000000000')
     * khi bán hàng offline cho khách vãng lai (system_rules.md §2.1).
     */
    Optional<KhachHang> findBySoDienThoai(String soDienThoai);
}

