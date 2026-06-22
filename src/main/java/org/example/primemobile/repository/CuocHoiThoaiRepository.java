package org.example.primemobile.repository;

import org.example.primemobile.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, Integer> {
}
