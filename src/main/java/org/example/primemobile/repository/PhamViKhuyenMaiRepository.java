package org.example.primemobile.repository;

import org.example.primemobile.entity.PhamViKhuyenMai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PhamViKhuyenMaiRepository extends JpaRepository<PhamViKhuyenMai, Integer> {
}
