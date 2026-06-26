package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietFlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChiTietFlashSaleRepository extends JpaRepository<ChiTietFlashSale, Integer> {

    List<ChiTietFlashSale> findByChuongTrinhKhuyenMaiId(Integer ctkmId);
}
