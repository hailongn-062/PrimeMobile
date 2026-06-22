package org.example.primemobile.repository;

import org.example.primemobile.entity.MayDienThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MayDienThoaiRepository extends JpaRepository<MayDienThoai, Integer> {
}
