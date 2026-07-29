package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.repository.NguoiDungRepository;
import org.example.primemobile.service.INhanVienService;
import org.example.primemobile.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Triển khai nghiệp vụ Quản lý Nhân Viên cho PrimeMobile — Admin only.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md §1):</h2>
 * <ul>
 *   <li>Truy vấn danh sách bắt buộc lọc {@code vai_tro = 'NhanVien'} trên bảng {@code nguoi_dung}.</li>
 *   <li>Khi thêm mới: {@code vaiTro} BẮT BUỘC gán cứng = {@code "NhanVien"}.</li>
 *   <li>Mật khẩu lưu plain-text (chế độ demo) — KHÔNG băm, so sánh trực tiếp {@code .equals()}.</li>
 *   <li>Xóa vật lý KHÔNG được phép — chỉ Khóa/Mở khóa tài khoản.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NhanVienServiceImpl implements INhanVienService {

    // -----------------------------------------------------------------------
    // CONSTANTS
    // -----------------------------------------------------------------------

    /** Vai trò gán cứng cho mọi tài khoản nhân viên — không được thay đổi. */
    private static final String VAI_TRO_NHAN_VIEN = "NhanVien";
    private static final String TRANG_THAI_HOAT_DONG = "hoat_dong";
    private static final String TRANG_THAI_KHOA       = "khoa";

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------

    private final NguoiDungRepository nguoiDungRepository;

    // =======================================================================
    // PUBLIC METHODS
    // =======================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Gọi {@link NguoiDungRepository#findByVaiTroOrderByNgayTaoDesc} với tham số
     * {@code "NhanVien"} để đảm bảo đúng điều kiện vai trò.
     */
    @Override
    @Transactional(readOnly = true)
    public List<NguoiDung> layDanhSachNhanVien() {
        log.info("[NhanVienService] Bắt đầu lấy danh sách nhân viên");
        return nguoiDungRepository.findByVaiTroOrderByNgayTaoDesc(VAI_TRO_NHAN_VIEN);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NguoiDung> timKiemVaLocNhanVien(String tuKhoa, String trangThai) {
        log.info("[NhanVienService] Bắt đầu tìm kiếm và lọc nhân viên: tuKhoa={}, trangThai={}", tuKhoa, trangThai);
        return nguoiDungRepository.timKiemVaLocNhanVien(VAI_TRO_NHAN_VIEN, tuKhoa, trangThai);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public NguoiDung layTheoId(Integer id) {
        log.debug("[NhanVienService] Lấy nhân viên theo id={}", id);

        NguoiDung nv = nguoiDungRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy nhân viên với ID: " + id));

        // Đảm bảo ID này thực sự là tài khoản NhanVien
        if (!VAI_TRO_NHAN_VIEN.equals(nv.getVaiTro())) {
            throw new EntityNotFoundException(
                    "Tài khoản ID=" + id + " không phải là nhân viên (vai_tro=" + nv.getVaiTro() + ").");
        }

        return nv;
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng thực thi:</h3>
     * <ol>
     *   <li>Validate email không trùng lặp.</li>
     *   <li>Validate số điện thoại (nếu có) không trùng lặp.</li>
     *   <li>Gán cứng {@code vaiTro = "NhanVien"} và {@code trangThai = "hoat_dong"}.</li>
     *   <li>Validate mật khẩu không rỗng, lưu plain-text vào DB (chế độ demo).</li>
     *   <li>Persist và trả về entity đã lưu.</li>
     * </ol>
     */
    @Override
    @Transactional
    public NguoiDung themMoi(NguoiDung nhanVien) {
        log.info("[NhanVienService] Thêm mới nhân viên — email={}", nhanVien.getEmail());

        // ------------------------------------------------------------------
        // Bước 1: Validate trùng email
        // ------------------------------------------------------------------
        if (nhanVien.getEmail() == null || nhanVien.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (!ValidationUtils.isValidEmail(nhanVien.getEmail())) {
            throw new IllegalArgumentException(ValidationUtils.EMAIL_INVALID_MSG);
        }
        String emailMoi = nhanVien.getEmail().trim().toLowerCase();
        if (nguoiDungRepository.existsByEmail(emailMoi)) {
            throw new IllegalArgumentException(ValidationUtils.EMAIL_EXISTS_MSG);
        }
        nhanVien.setEmail(emailMoi);

        // ------------------------------------------------------------------
        // Bước 2: Validate trùng số điện thoại (nếu có)
        if (nhanVien.getSoDienThoai() != null && !nhanVien.getSoDienThoai().isBlank()) {
            if (!ValidationUtils.isValidPhoneNumber(nhanVien.getSoDienThoai())) {
                throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
            }
            nguoiDungRepository.findBySoDienThoai(nhanVien.getSoDienThoai())
                    .ifPresent(existing -> {
                        throw new IllegalArgumentException(
                                "Số điện thoại '" + nhanVien.getSoDienThoai()
                                + "' đã được sử dụng bởi tài khoản khác.");
                    });
        }

        // ------------------------------------------------------------------
        // Bước 3: Gán cứng nghiệp vụ — không phụ thuộc vào dữ liệu client
        // ------------------------------------------------------------------
        nhanVien.setVaiTro(VAI_TRO_NHAN_VIEN);           // CỨNG: luôn là NhanVien
        nhanVien.setTrangThai(TRANG_THAI_HOAT_DONG);       // Tài khoản mới luôn hoạt động
        nhanVien.setNgayTao(LocalDateTime.now());
        nhanVien.setUpdatedAt(LocalDateTime.now());

        // ------------------------------------------------------------------
        // Bước 4: Lưu mật khẩu plain-text (chế độ demo — không băm)
        // ------------------------------------------------------------------
        if (nhanVien.getMatKhau() == null || nhanVien.getMatKhau().isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        // Giữ nguyên chuỗi mật khẩu gốc — nhất quán với PasswordUtil.checkPassword()
        // và AuthServiceImpl dùng so sánh .equals() plain-text

        // ------------------------------------------------------------------
        // Bước 5: Persist
        // ------------------------------------------------------------------
        NguoiDung saved = nguoiDungRepository.save(nhanVien);
        log.info("[NhanVienService] Thêm mới nhân viên thành công — id={}, email={}",
                saved.getId(), saved.getEmail());
        return saved;
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Chính sách cập nhật:</h3>
     * Chỉ cập nhật các trường thông tin hồ sơ cơ bản. {@code vaiTro} và {@code matKhau}
     * bị bảo vệ — không cho phép overwrite qua endpoint này.
     */
    @Override
    @Transactional
    public NguoiDung capNhat(Integer id, NguoiDung nhanVien) {
        log.info("[NhanVienService] Cập nhật nhân viên id={}", id);

        NguoiDung existing = layTheoId(id); // Đã validate vaiTro = NhanVien bên trong

        // ------------------------------------------------------------------
        // Validate trùng email với tài khoản KHÁC
        // ------------------------------------------------------------------
        if (nhanVien.getEmail() == null || nhanVien.getEmail().isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (!ValidationUtils.isValidEmail(nhanVien.getEmail())) {
            throw new IllegalArgumentException(ValidationUtils.EMAIL_INVALID_MSG);
        }
        String emailCapNhat = nhanVien.getEmail().trim().toLowerCase();
        if (!existing.getEmail().equals(emailCapNhat)
                && nguoiDungRepository.existsByEmail(emailCapNhat)) {
            throw new IllegalArgumentException(ValidationUtils.EMAIL_EXISTS_MSG);
        }
        nhanVien.setEmail(emailCapNhat);

        // ------------------------------------------------------------------
        // Validate trùng SĐT với tài khoản KHÁC (nếu có)
        // ------------------------------------------------------------------
        if (nhanVien.getSoDienThoai() != null && !nhanVien.getSoDienThoai().isBlank()) {
            if (!ValidationUtils.isValidPhoneNumber(nhanVien.getSoDienThoai())) {
                throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
            }
            nguoiDungRepository.findBySoDienThoai(nhanVien.getSoDienThoai())
                    .ifPresent(other -> {
                        if (!other.getId().equals(id)) {
                            throw new IllegalArgumentException(
                                    "Số điện thoại '" + nhanVien.getSoDienThoai()
                                    + "' đã được sử dụng bởi tài khoản khác.");
                        }
                    });
        }

        // ------------------------------------------------------------------
        // Chỉ cập nhật các trường được phép — bảo vệ vaiTro và matKhau
        // ------------------------------------------------------------------
        existing.setHoTen(nhanVien.getHoTen());
        existing.setEmail(nhanVien.getEmail());
        existing.setSoDienThoai(nhanVien.getSoDienThoai());
        existing.setUpdatedAt(LocalDateTime.now());
        // vaiTro, matKhau, trangThai, ngayTao KHÔNG được cập nhật ở đây

        NguoiDung updated = nguoiDungRepository.save(existing);
        log.info("[NhanVienService] Cập nhật nhân viên thành công — id={}", id);
        return updated;
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Logic Toggle:</h3>
     * <pre>
     *   hoat_dong → khoa
     *   khoa      → hoat_dong
     * </pre>
     */
    @Override
    @Transactional
    public NguoiDung doiTrangThai(Integer id) {
        log.info("[NhanVienService] Đổi trạng thái nhân viên id={}", id);

        NguoiDung nv = layTheoId(id); // Đã validate vaiTro = NhanVien

        // Toggle trạng thái
        if (TRANG_THAI_HOAT_DONG.equals(nv.getTrangThai())) {
            nv.setTrangThai(TRANG_THAI_KHOA);
            log.info("[NhanVienService] Khóa tài khoản nhân viên id={}", id);
        } else {
            nv.setTrangThai(TRANG_THAI_HOAT_DONG);
            log.info("[NhanVienService] Mở khóa tài khoản nhân viên id={}", id);
        }
        nv.setUpdatedAt(LocalDateTime.now());

        return nguoiDungRepository.save(nv);
    }
}
