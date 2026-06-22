package org.example.primemobile.dto.kho;

/**
 * DTO chứa thông tin định danh của một chiếc điện thoại vật lý.
 * <p>
 * Sử dụng trong luồng nhập IMEI thủ công / quét mã của nhân viên
 * (system_rules.md §3.3 – Cơ chế kiểm soát chặt chẽ mã IMEI).
 * <p>
 * Ràng buộc duy nhất (unique) cho imei1, imei2, serial được kiểm tra
 * tại Service Layer trước khi lưu vào database.
 *
 * <ul>
 *   <li>{@code imei1}  – IMEI khe SIM 1, bắt buộc (chuẩn GSMA: 15 chữ số).</li>
 *   <li>{@code imei2}  – IMEI khe SIM 2, tùy chọn (NULL với máy 1 SIM).</li>
 *   <li>{@code serial} – Số serial của máy, tùy chọn.</li>
 * </ul>
 */
public class ThemImeiRequest {

    /** IMEI khe SIM 1 – bắt buộc, duy nhất toàn hệ thống. */
    private String imei1;

    /** IMEI khe SIM 2 – tùy chọn (null với máy 1 SIM). */
    private String imei2;

    /** Số serial của máy – tùy chọn. */
    private String serial;

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    public ThemImeiRequest() {
    }

    public ThemImeiRequest(String imei1, String imei2, String serial) {
        this.imei1  = imei1;
        this.imei2  = imei2;
        this.serial = serial;
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getImei1() {
        return imei1;
    }

    public void setImei1(String imei1) {
        this.imei1 = imei1;
    }

    public String getImei2() {
        return imei2;
    }

    public void setImei2(String imei2) {
        this.imei2 = imei2;
    }

    public String getSerial() {
        return serial;
    }

    public void setSerial(String serial) {
        this.serial = serial;
    }
}
