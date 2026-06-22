package org.example.primemobile.repository;

import org.example.primemobile.entity.PhieuChuyenKho;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhieuChuyenKhoRepository extends JpaRepository<PhieuChuyenKho, Integer> {
}
