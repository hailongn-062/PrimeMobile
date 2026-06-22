package org.example.primemobile.repository;

import org.example.primemobile.entity.ThongSoKyThuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThongSoKyThuatRepository extends JpaRepository<ThongSoKyThuat, Integer> {
}
