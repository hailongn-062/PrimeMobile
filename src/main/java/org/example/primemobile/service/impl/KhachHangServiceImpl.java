package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.service.IKhachHangService;
import org.example.primemobile.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Triển khai phân hệ Quản lý Khách hàng — dành cho Admin tra cứu và cập nhật.
 * <p>
 * Service này chỉ cập nhật thông tin hồ sơ cơ bản của khách hàng.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KhachHangServiceImpl implements IKhachHangService {

    private final KhachHangRepository khachHangRepository;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Nếu từ khóa rỗng hoặc null, trả về toàn bộ danh sách khách hàng
     * (tránh lỗi JPQL khi truyền null vào CONCAT).
     */
    @Override
    @Transactional(readOnly = true)
    public List<KhachHang> timKiem(String tuKhoa) {
        // Nếu không có từ khóa, trả về toàn bộ danh sách (không dùng query có vấn đề)
        if (tuKhoa == null || tuKhoa.isBlank()) {
            return khachHangRepository.findAll();
        }
        String keyword = tuKhoa.trim();
        return khachHangRepository.timKiemTheoSdtHoacEmail(keyword);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public KhachHang layTheoId(Integer id) {
        return khachHangRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy khách hàng có ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     * <li>Load entity hiện tại.</li>
     * <li>Validate SĐT mới không trùng với khách khác.</li>
     * <li>Validate email mới không trùng với khách khác (nếu có).</li>
     * <li>Ghi đè các trường được phép cập nhật.</li>
     * </ol>
     */
    @Override
    @Transactional
    public KhachHang capNhat(Integer id, KhachHang khachHangMoi) {
        KhachHang existing = layTheoId(id);

        // Validate SĐT mới không trùng với khách khác
        if (khachHangMoi.getSoDienThoai() != null && !khachHangMoi.getSoDienThoai().isBlank()) {
            if (!ValidationUtils.isValidPhoneNumber(khachHangMoi.getSoDienThoai())) {
                throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
            }
            String sdtMoi = khachHangMoi.getSoDienThoai().trim();
            khachHangRepository.findBySoDienThoai(sdtMoi)
                    .ifPresent(other -> {
                        if (!other.getId().equals(id)) {
                            throw new IllegalArgumentException(
                                    "Số điện thoại \"" + sdtMoi + "\" đã được sử dụng bởi khách hàng khác.");
                        }
                    });
            existing.setSoDienThoai(sdtMoi);
        }

        // Validate email mới không trùng với khách khác (nếu có giá trị)
        if (khachHangMoi.getEmail() != null && !khachHangMoi.getEmail().isBlank()) {
            if (!ValidationUtils.isValidEmail(khachHangMoi.getEmail())) {
                throw new IllegalArgumentException(ValidationUtils.EMAIL_INVALID_MSG);
            }
            String emailMoi = khachHangMoi.getEmail().trim().toLowerCase();
            khachHangRepository.findByEmailIgnoreCase(emailMoi)
                    .ifPresent(other -> {
                        if (!other.getId().equals(id)) {
                            throw new IllegalArgumentException(ValidationUtils.EMAIL_EXISTS_MSG);
                        }
                    });
            existing.setEmail(emailMoi);
        } else {
            existing.setEmail(null); // Cho phép xóa email
        }

        // Cập nhật các trường thông tin cơ bản
        if (khachHangMoi.getHoTen() != null && !khachHangMoi.getHoTen().isBlank()) {
            existing.setHoTen(khachHangMoi.getHoTen().trim());
        }
        existing.setGioiTinh(khachHangMoi.getGioiTinh());
        existing.setNgaySinh(khachHangMoi.getNgaySinh());
        existing.setUpdatedAt(LocalDateTime.now());

        KhachHang updated = khachHangRepository.save(existing);
        log.info("[KhachHang] Đã cập nhật thông tin — id={}, hoTen={}", updated.getId(), updated.getHoTen());
        return updated;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     * <li>Validate {@code hoTen} và {@code soDienThoai} bắt buộc.</li>
     * <li>Kiểm tra SĐT đã tồn tại → trả về khách cũ (tránh duplicate).</li>
     * <li>Tạo mới {@code KhachHang} với {@code nguoiDung = null} (guest).</li>
     * </ol>
     */
    @Override
    @Transactional
    public KhachHang taoKhachVangLai(KhachHang khachHangMoi) {
        // Validate bắt buộc
        if (khachHangMoi.getHoTen() == null || khachHangMoi.getHoTen().isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống.");
        }
        if (khachHangMoi.getSoDienThoai() == null || khachHangMoi.getSoDienThoai().isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (!ValidationUtils.isValidPhoneNumber(khachHangMoi.getSoDienThoai())) {
            throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
        }

        String sdt = khachHangMoi.getSoDienThoai().trim();
        String hoTen = khachHangMoi.getHoTen().trim();

        // Kiểm tra SĐT đã tồn tại → trả về khách cũ (tránh tạo trùng)
        var existing = khachHangRepository.findBySoDienThoai(sdt);
        if (existing.isPresent()) {
            log.info("[KhachHang] SĐT '{}' đã tồn tại → trả về khách hàng cũ id={}",
                    sdt, existing.get().getId());
            return existing.get();
        }

        String email = null;
        if (khachHangMoi.getEmail() != null && !khachHangMoi.getEmail().isBlank()) {
            if (!ValidationUtils.isValidEmail(khachHangMoi.getEmail())) {
                throw new IllegalArgumentException(ValidationUtils.EMAIL_INVALID_MSG);
            }
            email = khachHangMoi.getEmail().trim().toLowerCase();
            if (khachHangRepository.findByEmailIgnoreCase(email).isPresent()) {
                throw new IllegalArgumentException(ValidationUtils.EMAIL_EXISTS_MSG);
            }
        }

        // Tạo mới khách vãng lai (nguoiDung = null → không có tài khoản)
        KhachHang khachMoi = KhachHang.builder()
                .hoTen(hoTen)
                .soDienThoai(sdt)
                .email(email)
                .gioiTinh(khachHangMoi.getGioiTinh())
                .nguoiDung(null) // Guest — không liên kết tài khoản
                .ngayTao(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        khachMoi = khachHangRepository.save(khachMoi);
        log.info("[KhachHang] Đã tạo khách vãng lai — id={}, hoTen='{}', sdt='{}'",
                khachMoi.getId(), khachMoi.getHoTen(), khachMoi.getSoDienThoai());
        return khachMoi;
    }
}