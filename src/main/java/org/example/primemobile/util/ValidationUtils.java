package org.example.primemobile.util;

public class ValidationUtils {

    /**
     * Regex validate số điện thoại Việt Nam:
     * - Bắt đầu bằng: 03, 05, 07, 08, hoặc 09
     * - Độ dài: đúng 10 chữ số
     * - Chỉ chứa các số 0-9
     */
    public static final String PHONE_REGEX = "^(03|05|07|08|09)\\d{8}$";
    
    /**
     * Thông báo lỗi mặc định khi số điện thoại không hợp lệ.
     */
    public static final String PHONE_INVALID_MSG = "Số điện thoại không hợp lệ.";

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._\\-+]+@[a-zA-Z0-9.\\-]+\\.[a-zA-Z]{2,}$";
    public static final String EMAIL_INVALID_MSG = "Email không hợp lệ.";
    public static final String EMAIL_EXISTS_MSG = "Email đã được sử dụng.";

    /**
     * Kiểm tra số điện thoại có hợp lệ theo chuẩn Việt Nam hay không.
     * @param phone Chuỗi số điện thoại
     * @return true nếu hợp lệ, false nếu không hợp lệ hoặc null/trống
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return false;
        }
        return phone.trim().matches(PHONE_REGEX);
    }

    /**
     * Kiểm tra email có hợp lệ hay không.
     * @param email Chuỗi email
     * @return true nếu hợp lệ, false nếu không hợp lệ hoặc rỗng, hoặc chứa khoảng trắng
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        if (email.length() > 254) {
            return false;
        }
        // Không chứa khoảng trắng
        if (email.contains(" ")) {
            return false;
        }
        return email.trim().matches(EMAIL_REGEX);
    }
}
