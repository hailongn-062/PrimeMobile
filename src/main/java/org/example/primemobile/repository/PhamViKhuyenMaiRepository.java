package org.example.primemobile.repository;

import org.example.primemobile.entity.PhamViKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PhamViKhuyenMaiRepository extends JpaRepository<PhamViKhuyenMai, Integer> {

    List<PhamViKhuyenMai> findByChuongTrinhKhuyenMaiId(Integer ctkmId);
}
