package org.example.primemobile.repository;

import org.example.primemobile.entity.YeuCauBaoHanh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YeuCauBaoHanhRepository extends JpaRepository<YeuCauBaoHanh, Integer> {
}
