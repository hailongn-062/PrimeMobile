package org.example.primemobile.repository;

import org.example.primemobile.entity.CauHinhThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CauHinhThanhToanRepository extends JpaRepository<CauHinhThanhToan, Integer> {
}
