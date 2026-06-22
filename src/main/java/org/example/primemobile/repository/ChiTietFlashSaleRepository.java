package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietFlashSale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietFlashSaleRepository extends JpaRepository<ChiTietFlashSale, Integer> {
}
