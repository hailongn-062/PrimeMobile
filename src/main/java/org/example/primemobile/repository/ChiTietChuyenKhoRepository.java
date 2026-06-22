package org.example.primemobile.repository;

import org.example.primemobile.entity.ChiTietChuyenKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChiTietChuyenKhoRepository extends JpaRepository<ChiTietChuyenKho, Integer> {
}
