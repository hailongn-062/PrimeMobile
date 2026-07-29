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

    Optional<NguoiDung> findByEmailOrSoDienThoai(String email, String soDienThoai);

    java.util.List<NguoiDung> findByVaiTroOrderByNgayTaoDesc(String vaiTro);

    boolean existsByEmail(String email);

    Optional<NguoiDung> findBySoDienThoai(String soDienThoai);

    @org.springframework.data.jpa.repository.Query("SELECT n FROM NguoiDung n WHERE n.vaiTro = :vaiTro " +
            "AND (:trangThai IS NULL OR n.trangThai = :trangThai) " +
            "AND (:tuKhoa IS NULL OR " +
            "LOWER(n.hoTen) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
            "LOWER(n.email) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) OR " +
            "LOWER(n.soDienThoai) LIKE LOWER(CONCAT('%', :tuKhoa, '%')) " +
            ") " +
            "ORDER BY n.ngayTao DESC")
    java.util.List<NguoiDung> timKiemVaLocNhanVien(
            @org.springframework.data.repository.query.Param("vaiTro") String vaiTro,
            @org.springframework.data.repository.query.Param("tuKhoa") String tuKhoa,
            @org.springframework.data.repository.query.Param("trangThai") String trangThai
    );
}
