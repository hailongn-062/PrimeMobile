package org.example.primemobile.repository;

import org.example.primemobile.entity.HangSanXuat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HangSanXuatRepository extends JpaRepository<HangSanXuat, Integer> {
}
