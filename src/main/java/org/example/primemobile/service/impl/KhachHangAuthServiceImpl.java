package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.repository.NguoiDungRepository;
import org.example.primemobile.service.IKhachHangAuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Triển khai phân hệ Xác thực Khách Hàng (đăng nhập / đăng ký).
 * <p>
 * Nhất quán với toàn hệ thống (system_rules.md §1):
 * <ul>
 *   <li>KHÔNG dùng Spring Security / JWT.</li>
 *   <li>Mật khẩu so sánh plain-text bằng {@code .equals()} (chế độ demo).</li>
 *   <li>Session quản lý bởi Controller, không phải Service.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KhachHangAuthServiceImpl implements IKhachHangAuthService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ NGHIỆP VỤ
    // -----------------------------------------------------------------------
    private static final String ROLE_KHACH_HANG    = "KhachHang";
    private static final String TRANG_THAI_HOAT_DONG = "hoat_dong";

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------
    private final NguoiDungRepository nguoiDungRepository;
    private final KhachHangRepository khachHangRepository;

    // =======================================================================
    // ĐĂNG NHẬP
    // =======================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Thứ tự kiểm tra Fail-Fast:
     * <ol>
     *   <li>Email tồn tại trong nguoi_dung?</li>
     *   <li>vaiTro = 'KhachHang'?</li>
     *   <li>trangThai = 'hoat_dong'?</li>
     *   <li>Mật khẩu khớp?</li>
     *   <li>Tìm KhachHang liên kết?</li>
     * </ol>
     */
    @Override
    @Transactional
    public SessionKhachHang dangNhap(String email, String matKhau) {
        String emailInput = email == null ? "" : email.trim();
        log.info("[KhachHangAuth] Đăng nhập — email={}", emailInput);

        // Bước 1: Tìm NguoiDung theo email
        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(emailInput)
                .orElseThrow(() -> {
                    log.warn("[KhachHangAuth] Email không tồn tại: {}", emailInput);
                    // Thông báo chung để tránh user enumeration
                    return new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
                });

        // Bước 2: Kiểm tra vai trò phải là KhachHang
        if (!ROLE_KHACH_HANG.equals(nguoiDung.getVaiTro())) {
            log.warn("[KhachHangAuth] Tài khoản không phải KhachHang — email={}, vaiTro={}",
                    emailInput, nguoiDung.getVaiTro());
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
        }

        // Bước 3: Kiểm tra tài khoản không bị khóa
        if (!TRANG_THAI_HOAT_DONG.equals(nguoiDung.getTrangThai())) {
            log.warn("[KhachHangAuth] Tài khoản bị khóa — email={}", emailInput);
            throw new IllegalArgumentException("Tài khoản của bạn đã bị khóa. Vui lòng liên hệ hỗ trợ.");
        }

        // Bước 4: Kiểm tra mật khẩu (plain-text, chế độ demo)
        if (matKhau == null || !matKhau.equals(nguoiDung.getMatKhau())) {
            log.warn("[KhachHangAuth] Mật khẩu không khớp — email={}", emailInput);
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
        }

        // Bước 5: Tìm bản ghi KhachHang liên kết với NguoiDung này
        KhachHang khachHang = khachHangRepository
                .findByNguoiDung_Id(nguoiDung.getId())
                .orElseThrow(() -> {
                    log.error("[KhachHangAuth] Không tìm thấy KhachHang liên kết — nguoiDungId={}",
                            nguoiDung.getId());
                    return new IllegalStateException("Tài khoản khách hàng chưa được thiết lập đầy đủ.");
                });

        // Bước 6: Cập nhật lanDangNhapCuoi
        nguoiDung.setLanDangNhapCuoi(LocalDateTime.now());
        nguoiDungRepository.save(nguoiDung);

        SessionKhachHang session = buildSession(khachHang);
        log.info("[KhachHangAuth] Đăng nhập thành công — khachHangId={}, email={}",
                session.getKhachHangId(), session.getEmail());
        return session;
    }

    // =======================================================================
    // ĐĂNG KÝ
    // =======================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Luồng 2 bước trong 1 transaction:
     * <ol>
     *   <li>Tạo {@code NguoiDung} (tài khoản đăng nhập).</li>
     *   <li>Tạo {@code KhachHang} (hồ sơ khách hàng) liên kết với NguoiDung.</li>
     * </ol>
     * Nếu bất kỳ bước nào fail → rollback cả 2.
     */
    @Override
    @Transactional
    public SessionKhachHang dangKy(String hoTen, String email, String soDienThoai, String matKhau) {
        log.info("[KhachHangAuth] Đăng ký tài khoản mới — email={}, sdt={}", email, soDienThoai);

        // Validate đầu vào
        validateDangKy(hoTen, email, soDienThoai, matKhau);

        // Kiểm tra email chưa tồn tại trong nguoi_dung
        if (nguoiDungRepository.existsByEmail(email.trim())) {
            throw new IllegalArgumentException(
                    "Email \"" + email.trim() + "\" đã được sử dụng. Vui lòng dùng email khác.");
        }

        // Kiểm tra SĐT chưa tồn tại trong khach_hang
        if (khachHangRepository.findBySoDienThoai(soDienThoai.trim()).isPresent()) {
            throw new IllegalArgumentException(
                    "Số điện thoại \"" + soDienThoai.trim() + "\" đã được sử dụng bởi tài khoản khác.");
        }

        // Tạo NguoiDung (tài khoản đăng nhập)
        NguoiDung nguoiDung = new NguoiDung();
        nguoiDung.setEmail(email.trim().toLowerCase());
        nguoiDung.setMatKhau(matKhau);                  // Plain-text, chế độ demo
        nguoiDung.setHoTen(hoTen.trim());
        nguoiDung.setSoDienThoai(soDienThoai.trim());
        nguoiDung.setVaiTro(ROLE_KHACH_HANG);            // Bắt buộc gán cứng
        nguoiDung.setTrangThai(TRANG_THAI_HOAT_DONG);
        nguoiDung.setNgayTao(LocalDateTime.now());
        nguoiDung.setUpdatedAt(LocalDateTime.now());
        NguoiDung savedNguoiDung = nguoiDungRepository.save(nguoiDung);

        // Tạo KhachHang (hồ sơ khách hàng) liên kết với NguoiDung
        KhachHang khachHang = new KhachHang();
        khachHang.setNguoiDung(savedNguoiDung);
        khachHang.setHoTen(hoTen.trim());
        khachHang.setEmail(email.trim().toLowerCase());
        khachHang.setSoDienThoai(soDienThoai.trim());
        khachHang.setNgayTao(LocalDateTime.now());
        khachHang.setUpdatedAt(LocalDateTime.now());
        KhachHang savedKhachHang = khachHangRepository.save(khachHang);

        log.info("[KhachHangAuth] Đăng ký thành công — khachHangId={}, email={}",
                savedKhachHang.getId(), savedKhachHang.getEmail());

        // Tự động đăng nhập sau đăng ký
        return buildSession(savedKhachHang);
    }

    // =======================================================================
    // PRIVATE HELPERS
    // =======================================================================

    /**
     * Validate các trường bắt buộc khi đăng ký.
     */
    private void validateDangKy(String hoTen, String email, String soDienThoai, String matKhau) {
        if (hoTen == null || hoTen.isBlank()) {
            throw new IllegalArgumentException("Họ tên không được để trống.");
        }
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống.");
        }
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email không hợp lệ.");
        }
        if (soDienThoai == null || soDienThoai.isBlank()) {
            throw new IllegalArgumentException("Số điện thoại không được để trống.");
        }
        if (matKhau == null || matKhau.isBlank()) {
            throw new IllegalArgumentException("Mật khẩu không được để trống.");
        }
        if (matKhau.length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
    }

    /**
     * Build SessionKhachHang từ entity KhachHang.
     */
    private SessionKhachHang buildSession(KhachHang khachHang) {
        return new SessionKhachHang(
                khachHang.getId(),
                khachHang.getEmail(),
                khachHang.getHoTen(),
                khachHang.getSoDienThoai()
        );
    }
}
