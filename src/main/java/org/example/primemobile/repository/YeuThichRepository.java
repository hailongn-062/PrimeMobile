package org.example.primemobile.repository;

import org.example.primemobile.entity.YeuThich;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface YeuThichRepository extends JpaRepository<YeuThich, Integer> {
}
