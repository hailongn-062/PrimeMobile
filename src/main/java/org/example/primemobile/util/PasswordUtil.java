package org.example.primemobile.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Tiện ích băm mật khẩu bằng thuật toán SHA-256 thuần Java
 * ({@link java.security.MessageDigest}).
 * <p>
 * Theo system_rules.md §1: TUYỆT ĐỐI KHÔNG dùng Spring Security/JWT.
 * Lớp này thay thế BCryptPasswordEncoder bằng SHA-256 tự triển khai.
 * <p>
 * <b>Lưu ý:</b> SHA-256 không có salt ngẫu nhiên như BCrypt nên kém an toàn hơn
 * với rainbow-table attack trong môi trường production. Tuy nhiên, đây là
 * lựa chọn phù hợp cho môi trường demo đồ án theo yêu cầu của dự án.
 * <p>
 * Luồng sử dụng:
 * <pre>
 *   // Khi tạo tài khoản:
 *   String hashed = PasswordUtil.hashPassword("matkhau123");
 *   nguoiDung.setMatKhau(hashed);
 *
 *   // Khi đăng nhập:
 *   boolean ok = PasswordUtil.checkPassword("matkhau123", nguoiDung.getMatKhau());
 * </pre>
 */
public final class PasswordUtil {

    /** Tên thuật toán băm — SHA-256 từ java.security. */
    private static final String ALGORITHM = "SHA-256";

    // Lớp tiện ích thuần static — ngăn không cho khởi tạo instance
    private PasswordUtil() {
        throw new UnsupportedOperationException("PasswordUtil là lớp tiện ích, không khởi tạo được.");
    }

    /**
     * Băm một chuỗi mật khẩu plain-text thành chuỗi hex SHA-256.
     * <p>
     * Mã hóa chuỗi sang UTF-8 trước khi băm để đảm bảo nhất quán
     * trên mọi nền tảng (Windows/Linux/macOS).
     *
     * @param plainPassword Mật khẩu plain-text cần băm. Không được null.
     * @return Chuỗi hex SHA-256 64 ký tự (chữ thường), ví dụ:
     *         {@code "a665a45920422f9d417e4867efdc4fb8a04a1f3fff1fa07e998e86f7f7a27ae3"}.
     * @throws IllegalStateException nếu JVM không hỗ trợ SHA-256 (không bao giờ xảy ra trên Java 8+).
     */
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null) {
            throw new IllegalArgumentException("Mật khẩu không được null.");
        }
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashBytes = digest.digest(plainPassword.getBytes(StandardCharsets.UTF_8));
            // Java 17+: HexFormat.of() thay thế vòng lặp String.format thủ công
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // Không bao giờ xảy ra — SHA-256 là thuật toán bắt buộc trong Java SE
            throw new IllegalStateException("SHA-256 không khả dụng trên JVM hiện tại.", e);
        }
    }

    /**
     * Xác minh mật khẩu plain-text có khớp với hash đã lưu trong database không.
     * <p>
     * So sánh theo cách: băm {@code plainPassword} rồi so sánh với {@code hashedPassword}.
     * Dùng {@link String#equalsIgnoreCase} để bảo vệ trường hợp hash được lưu
     * với cách viết hoa khác nhau (dù {@link #hashPassword} luôn trả về chữ thường).
     *
     * @param plainPassword    Mật khẩu plain-text từ form đăng nhập.
     * @param hashedPassword   Mật khẩu đã băm lấy từ database.
     * @return {@code true} nếu mật khẩu khớp, {@code false} nếu không khớp.
     */
    public static boolean checkPassword(String plainPassword, String hashedPassword) {
        if (plainPassword == null || hashedPassword == null) {
            return false;
        }
        String hashedInput = hashPassword(plainPassword);
        return hashedInput.equalsIgnoreCase(hashedPassword);
    }
}
