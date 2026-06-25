package org.example.primemobile.repository;

import org.example.primemobile.entity.NguoiDung;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NguoiDungRepository extends JpaRepository<NguoiDung, Integer> {

    /**
     * Tra cứu người dùng theo email để phục vụ luồng đăng nhập Admin/NhanVien.
     * Dùng trong {@code AuthServiceImpl.dangNhapAdmin()}.
     */
    Optional<NguoiDung> findByEmail(String email);

    java.util.List<NguoiDung> findByVaiTroOrderByNgayTaoDesc(String vaiTro);

    boolean existsByEmail(String email);

    Optional<NguoiDung> findBySoDienThoai(String soDienThoai);
}
