package org.example.primemobile.repository;

import org.example.primemobile.entity.HoiDapSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HoiDapSanPhamRepository extends JpaRepository<HoiDapSanPham, Integer> {
}
