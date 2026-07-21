package org.example.primemobile.repository;

import org.example.primemobile.entity.CuocHoiThoai;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuocHoiThoaiRepository extends JpaRepository<CuocHoiThoai, Integer> {
    List<CuocHoiThoai> findByKhachHangIdOrderByUpdatedAtDesc(Integer khachHangId);
    Optional<CuocHoiThoai> findFirstBySessionIdOrderByUpdatedAtDesc(String sessionId);
}
