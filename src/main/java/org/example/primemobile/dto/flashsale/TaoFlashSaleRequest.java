package org.example.primemobile.dto.flashsale;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO nhận dữ liệu tạo mới Flash Sale từ Admin.
 * <p>
 * Flash Sale = {@link org.example.primemobile.entity.ChuongTrinhKhuyenMai} với
 * {@code loai = 'flash_sale'} + danh sách {@link org.example.primemobile.entity.ChiTietFlashSale}
 * cho từng biến thể sản phẩm.
 *
 * <h3>Request Body mẫu:</h3>
 * <pre>{@code
 * {
 *   "tenCtkm": "Flash Sale Cuối Tuần - iPhone 16",
 *   "moTa": "Giảm sốc chỉ trong 2 giờ",
 *   "ngayBatDau": "2024-06-28T10:00:00",
 *   "ngayKetThuc": "2024-06-28T12:00:00",
 *   "gioFlashBatDau": "2024-06-28T10:00:00",
 *   "gioFlashKetThuc": "2024-06-28T12:00:00",
 *   "soLuongToiDa": 100,
 *   "chiTiets": [
 *     { "bienTheSanPhamId": 5, "giaFlash": 18500000, "soLuongGioiHan": 50 },
 *     { "bienTheSanPhamId": 8, "giaFlash": 22000000, "soLuongGioiHan": 30 }
 *   ]
 * }
 * }</pre>
 */
public class TaoFlashSaleRequest {

    /** Tên chương trình flash sale (bắt buộc). */
    private String tenCtkm;

    /** Mô tả nội dung ưu đãi (tuỳ chọn). */
    private String moTa;

    /** Ngày giờ bắt đầu flash sale (bắt buộc). */
    private LocalDateTime ngayBatDau;

    /** Ngày giờ kết thúc flash sale (bắt buộc). */
    private LocalDateTime ngayKetThuc;

    /** Giờ flash bắt đầu trong ngày (bắt buộc với flash_sale). */
    private LocalDateTime gioFlashBatDau;

    /** Giờ flash kết thúc trong ngày (bắt buộc với flash_sale). */
    private LocalDateTime gioFlashKetThuc;

    /** Tổng số lượng sản phẩm tối đa được áp giá flash. */
    private Integer soLuongToiDa;

    /** Danh sách biến thể tham gia flash sale với giá riêng. */
    private List<ChiTietRequest> chiTiets;

    // -------------------------------------------------------------------------
    // Inner DTO: chi tiết từng biến thể
    // -------------------------------------------------------------------------

    public static class ChiTietRequest {

        /** ID biến thể sản phẩm tham gia flash sale. */
        private Integer bienTheSanPhamId;

        /** Giá flash sale đặc biệt (phải nhỏ hơn giaBan hiện tại). */
        private BigDecimal giaFlash;

        /** Số lượng tối đa được bán với giá flash cho biến thể này. */
        private Integer soLuongGioiHan;

        public Integer getBienTheSanPhamId() { return bienTheSanPhamId; }
        public void setBienTheSanPhamId(Integer v) { this.bienTheSanPhamId = v; }
        public BigDecimal getGiaFlash() { return giaFlash; }
        public void setGiaFlash(BigDecimal v) { this.giaFlash = v; }
        public Integer getSoLuongGioiHan() { return soLuongGioiHan; }
        public void setSoLuongGioiHan(Integer v) { this.soLuongGioiHan = v; }
    }

    // -------------------------------------------------------------------------
    // Getters & Setters
    // -------------------------------------------------------------------------

    public String getTenCtkm() { return tenCtkm; }
    public void setTenCtkm(String v) { this.tenCtkm = v; }
    public String getMoTa() { return moTa; }
    public void setMoTa(String v) { this.moTa = v; }
    public LocalDateTime getNgayBatDau() { return ngayBatDau; }
    public void setNgayBatDau(LocalDateTime v) { this.ngayBatDau = v; }
    public LocalDateTime getNgayKetThuc() { return ngayKetThuc; }
    public void setNgayKetThuc(LocalDateTime v) { this.ngayKetThuc = v; }
    public LocalDateTime getGioFlashBatDau() { return gioFlashBatDau; }
    public void setGioFlashBatDau(LocalDateTime v) { this.gioFlashBatDau = v; }
    public LocalDateTime getGioFlashKetThuc() { return gioFlashKetThuc; }
    public void setGioFlashKetThuc(LocalDateTime v) { this.gioFlashKetThuc = v; }
    public Integer getSoLuongToiDa() { return soLuongToiDa; }
    public void setSoLuongToiDa(Integer v) { this.soLuongToiDa = v; }
    public List<ChiTietRequest> getChiTiets() { return chiTiets; }
    public void setChiTiets(List<ChiTietRequest> v) { this.chiTiets = v; }
}
