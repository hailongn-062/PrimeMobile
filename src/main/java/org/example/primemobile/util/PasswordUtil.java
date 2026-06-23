package org.example.primemobile.util;

/**
 * Tiện ích so sánh mật khẩu dạng plain-text — dùng cho môi trường demo/test.
 * <p>
 * Chế độ demo: mật khẩu được lưu nguyên bản trong DB và so sánh trực tiếp,
 * không qua bất kỳ thuật toán băm nào, giúp việc test và trình diễn dự án
 * PrimeMobile trở nên đơn giản hơn.
 * <p>
 * Luồng sử dụng:
 * <pre>
 *   // Khi tạo tài khoản (không băm, lưu thẳng):
 *   nguoiDung.setMatKhau(PasswordUtil.hashPassword("matkhau123"));
 *
 *   // Khi đăng nhập:
 *   boolean ok = PasswordUtil.checkPassword("matkhau123", nguoiDung.getMatKhau());
 * </pre>
 */
public final class PasswordUtil {

    // Lớp tiện ích thuần static — ngăn không cho khởi tạo instance
    private PasswordUtil() {
        throw new UnsupportedOperationException("PasswordUtil là lớp tiện ích, không khởi tạo được.");
    }

    /**
     * Trả về nguyên bản mật khẩu đầu vào (không băm).
     * <p>
     * Phương thức này giữ nguyên chữ ký để tương thích với các nơi gọi đến,
     * đồng thời đảm bảo mật khẩu được lưu DB ở dạng plain-text cho môi trường demo.
     *
     * @param password Mật khẩu plain-text cần "lưu". Không được null.
     * @return Chuỗi mật khẩu nguyên bản, không biến đổi.
     */
    public static String hashPassword(String password) {
        return password;
    }

    /**
     * So sánh mật khẩu plain-text từ form đăng nhập với mật khẩu lưu trong database.
     * <p>
     * So sánh trực tiếp bằng {@link String#equals} — phân biệt chữ hoa/thường.
     *
     * @param rawPassword    Mật khẩu plain-text từ form đăng nhập.
     * @param storedPassword Mật khẩu plain-text lấy từ database.
     * @return {@code true} nếu hai chuỗi giống nhau hoàn toàn, {@code false} nếu không khớp.
     */
    public static boolean checkPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        return rawPassword.equals(storedPassword);
    }
}
