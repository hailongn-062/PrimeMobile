package org.example.primemobile.repository;

import org.example.primemobile.entity.LichSuDiem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LichSuDiemRepository extends JpaRepository<LichSuDiem, Integer> {
}
