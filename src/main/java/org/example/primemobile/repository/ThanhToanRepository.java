package org.example.primemobile.repository;

import org.example.primemobile.entity.ThanhToan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ThanhToanRepository extends JpaRepository<ThanhToan, Integer> {

    Optional<ThanhToan> findByVnpTransactionNo(String vnpTransactionNo);

    List<ThanhToan> findByDonHangId(Integer donHangId);

    Optional<ThanhToan> findByDonHangIdAndTrangThai(Integer donHangId, String trangThai);
}
