package org.example.primemobile.dto.order;  // ← đổi package

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO chi tiết một dòng sản phẩm trong đơn hàng.
 * <p>
 * Dùng để hiển thị danh sách sản phẩm trong modal xác nhận IMEI
 * và trong màn hình chi tiết đơn hàng của Admin.
 *
 * @see org.example.primemobile.entity.ChiTietDonHang
 * @see org.example.primemobile.entity.BienTheSanPham
 * @see org.example.primemobile.entity.SanPham
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChiTietDonHangDto {

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN CHI TIẾT ĐƠN HÀNG
    // ────────────────────────────────────────────────────────────────────────

    /** ID của dòng chi tiết (chi_tiet_don_hang.id) */
    private Integer id;

    /** Số lượng sản phẩm trong dòng này */
    private Integer soLuong;

    /**
     * Đơn giá bán tại thời điểm đặt hàng (price snapshot).
     * Đây là giá sau khi áp dụng Flash Sale / Giảm trực tiếp,
     * nhưng chưa bao gồm giảm toàn đơn (đã tính riêng ở DonHang.tienGiamGia).
     */
    private BigDecimal donGiaBan;

    /**
     * Giá gốc (giá niêm yết của biến thể) tại thời điểm đặt hàng hoặc hiện tại.
     */
    private BigDecimal giaGoc;

    /** Thành tiền = soLuong × donGiaBan (computed column trong DB) */
    private BigDecimal thanhTien;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN BIẾN THỂ SẢN PHẨM (SKU)
    // ────────────────────────────────────────────────────────────────────────

    /** ID của biến thể sản phẩm (bien_the_san_pham.id) – dùng để lấy IMEI */
    private Integer bienTheSanPhamId;

    /** Mã SKU (ví dụ: "IP15PM-256-NAT") */
    private String maSku;

    /** Tên màu sắc (ví dụ: "Titan Tự Nhiên") */
    private String mauSac;

    /** Dung lượng RAM (GB) */
    private Integer ramGb;

    /** Dung lượng lưu trữ (GB) */
    private Integer luuTruGb;

    // ────────────────────────────────────────────────────────────────────────
    // THÔNG TIN SẢN PHẨM CHA
    // ────────────────────────────────────────────────────────────────────────

    /** Tên sản phẩm (ví dụ: "iPhone 15 Pro Max") – lấy từ SanPham.tenSanPham */
    private String tenSanPham;

    /** Danh sách IMEI đã gán cho chi tiết đơn hàng này */
    private java.util.List<org.example.primemobile.dto.kho.ImeiDto> imeiList;
}