package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.DangNhapAdminRequest;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.repository.NguoiDungRepository;
import org.example.primemobile.service.IAuthService;
import org.example.primemobile.util.PasswordUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Triển khai nghiệp vụ Xác thực cho Admin và NhanVien của PrimeMobile.
 *
 * <h2>Nguyên tắc bảo mật áp dụng (system_rules.md §1):</h2>
 * <ul>
 *   <li>KHÔNG dùng Spring Security / JWT.</li>
 *   <li>Phiên đăng nhập được quản lý bằng {@code HttpSession} do Controller xử lý.</li>
 *   <li>Service chỉ xác thực và trả về {@link SessionUser} — không tự set session.</li>
 *   <li>Mật khẩu so sánh qua {@link PasswordUtil#checkPassword(String, String)} (SHA-256).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ NGHIỆP VỤ
    // -----------------------------------------------------------------------

    /** Giá trị vaiTro hợp lệ cho Admin Dashboard. */
    private static final String ROLE_ADMIN      = "Admin";
    private static final String ROLE_NHAN_VIEN  = "NhanVien";

    /** Trạng thái tài khoản đang hoạt động. */
    private static final String TRANG_THAI_HOAT_DONG = "hoat_dong";

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------

    private final NguoiDungRepository nguoiDungRepository;

    // =======================================================================
    // PUBLIC METHODS
    // =======================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Thứ tự kiểm tra (Fail-Fast từng bước):</h3>
     * <ol>
     *   <li>Email tồn tại trong database?</li>
     *   <li>Tài khoản đang {@code hoat_dong} (không bị khóa)?</li>
     *   <li>Vai trò có phải {@code Admin} hoặc {@code NhanVien}?</li>
     *   <li>Mật khẩu có khớp với hash trong DB?</li>
     * </ol>
     * Sau khi xác thực thành công, cập nhật {@code lanDangNhapCuoi} vào DB.
     */
    @Override
    @Transactional
    public SessionUser dangNhapAdmin(DangNhapAdminRequest request) {

        log.info("[AuthService] Bắt đầu xác thực đăng nhập Admin — email={}",
                request.getEmail());

        // ------------------------------------------------------------------
        // Bước 1: Tìm người dùng theo email
        // ------------------------------------------------------------------
        String emailInput = request.getEmail() == null ? "" : request.getEmail().trim();

        NguoiDung nguoiDung = nguoiDungRepository.findByEmail(emailInput)
                .orElseThrow(() -> {
                    log.warn("[AuthService] Email không tồn tại: {}", emailInput);
                    // Trả về thông báo chung — không tiết lộ "email không tồn tại"
                    // để tránh user enumeration attack
                    return new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
                });

        // ------------------------------------------------------------------
        // Bước 2: Kiểm tra tài khoản không bị khóa
        // ------------------------------------------------------------------
        if (!TRANG_THAI_HOAT_DONG.equals(nguoiDung.getTrangThai())) {
            log.warn("[AuthService] Tài khoản bị khóa — email={}", emailInput);
            throw new IllegalArgumentException(
                    "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.");
        }

        // ------------------------------------------------------------------
        // Bước 3: Kiểm tra vai trò — Chỉ Admin và NhanVien được vào Dashboard
        // ------------------------------------------------------------------
        String vaiTro = nguoiDung.getVaiTro();
        if (!ROLE_ADMIN.equals(vaiTro) && !ROLE_NHAN_VIEN.equals(vaiTro)) {
            log.warn("[AuthService] Tài khoản không có quyền Admin/NhanVien — email={}, vaiTro={}",
                    emailInput, vaiTro);
            throw new IllegalArgumentException(
                    "Tài khoản không có quyền truy cập vào khu vực quản trị.");
        }

        // ------------------------------------------------------------------
        // Bước 4: Xác minh mật khẩu bằng SHA-256
        // ------------------------------------------------------------------
        if (!PasswordUtil.checkPassword(request.getMatKhau(), nguoiDung.getMatKhau())) {
            log.warn("[AuthService] Mật khẩu không khớp — email={}", emailInput);
            throw new IllegalArgumentException("Email hoặc mật khẩu không chính xác.");
        }

        // ------------------------------------------------------------------
        // Bước 5: Cập nhật thời điểm đăng nhập cuối
        // ------------------------------------------------------------------
        nguoiDung.setLanDangNhapCuoi(LocalDateTime.now());
        nguoiDungRepository.save(nguoiDung);

        // ------------------------------------------------------------------
        // Bước 6: Build và trả về SessionUser để Controller lưu vào HttpSession
        // ------------------------------------------------------------------
        SessionUser sessionUser = new SessionUser(
                nguoiDung.getId(),
                nguoiDung.getEmail(),
                nguoiDung.getHoTen(),
                nguoiDung.getVaiTro()
        );

        log.info("[AuthService] Xác thực thành công — id={}, email={}, role={}",
                sessionUser.getId(), sessionUser.getEmail(), sessionUser.getRole());

        return sessionUser;
    }
}
