package org.example.primemobile.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entity mapping bảng don_hang (Module 7: Đơn hàng).
 * <p>
 * Trung tâm của toàn bộ luồng bán hàng. Mỗi đơn hàng có 2 kênh:
 *  - "online"   : Khách đặt qua website, xử lý bởi luồng online.
 *  - "tai_quay" : Nhân viên tạo tại quầy (bán hàng offline).
 * <p>
 * ⚠️ COMPUTED COLUMNS (AS PERSISTED):
 *  Cột {@code tong_thanh_toan} = tong_tien_hang - tien_giam_gia + phi_ship
 *  BẮT BUỘC dùng {@code @Column(insertable = false, updatable = false)}.
 * <p>
 * Snapshot địa chỉ giao hàng:
 *  Địa chỉ giao được snapshot ngay lúc đặt hàng (hoTenNguoiNhan, sdtNguoiNhan,
 *  diaChiGiaCuThe, phuongXaGiao, quanHuyenGiao, tinhThanhGiao).
 *  Điều này đảm bảo đơn hàng không bị ảnh hưởng dù khách sau đó thay đổi địa chỉ.
 * <p>
 * FK tạm giữ Integer (sẽ refactor khi tạo Entity tương ứng):
 * ✅ Toàn bộ FK đã được refactor hoàn tất — không còn thuộc tính Integer tạm nào.
 * <p>
 * Trạng thái đơn hàng (CHECK chk_dh_trang_thai):
 *  "cho_xac_nhan" | "da_xac_nhan" | "dang_giao" | "da_giao" | "da_huy"
 * <p>
 * Trạng thái thanh toán (CHECK chk_dh_trang_thai_tt):
 *  "chua_thanh_toan" | "dang_cho_qr" | "da_thanh_toan" | "that_bai" | "hoan_tien"
 * <p>
 * Luật hủy đơn (system_rules.md §2.2):
 *  Khách hàng chỉ được hủy khi trang_thai = 'cho_xac_nhan'.
 *  Nút Hủy phải bị Disable khi đơn đã chuyển sang 'da_xac_nhan' hoặc 'dang_giao'.
 * <p>
 * Quan hệ:
 *  - N:1 với {@link KhachHang}          (FK khach_hang_id)
 *  - N:1 với {@link NguoiDung}          (FK nguoi_xu_ly_id, nullable)
 *  - N:1 với {@link DiaChiKhachHang}    (FK dia_chi_giao_id, ON DELETE SET NULL)
 *  - 1-N với {@link ChiTietDonHang}     (mappedBy donHang, CASCADE ALL)
 *  - 1-N với {@link ThanhToan}          (mappedBy donHang)
 */
@Entity
@Table(
        name = "don_hang",
        indexes = {
                @Index(name = "idx_dh_kh",   columnList = "khach_hang_id, ngay_dat"),
                @Index(name = "idx_dh_tts",  columnList = "trang_thai, ngay_dat"),
                @Index(name = "idx_dh_ngay", columnList = "ngay_dat"),
                @Index(name = "idx_dh_tttt", columnList = "trang_thai_thanh_toan, thoi_gian_het_han_tt")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_dh_ma", columnNames = "ma_don_hang")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"chiTietDonHangs", "thanhToans"})
@EqualsAndHashCode(exclude = {"chiTietDonHangs", "thanhToans"})
public class DonHang {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Mã đơn hàng – duy nhất, hiển thị cho khách (ví dụ: "DH2024001"). */
    @Column(name = "ma_don_hang", nullable = false, length = 50, unique = true)
    private String maDonHang;

    /**
     * Khách hàng đặt đơn.
     * NOT NULL – mọi đơn hàng đều phải gắn với 1 khách hàng
     * (kể cả khách lẻ vãng lai sẽ được gán vào tài khoản khách lẻ mặc định).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "khach_hang_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_dh_kh")
    )
    private KhachHang khachHang;

    /**
     * Nhân viên / Admin xử lý đơn này.
     * NULL khi đơn online chưa có nhân viên nhận xử lý.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "nguoi_xu_ly_id",
            foreignKey = @ForeignKey(name = "fk_dh_nd")
    )
    private NguoiDung nguoiXuLy;

    /**
     * Cuộc hội thoại Chatbot AI dẫn đến đơn hàng này (nếu có).
     * Nullable — đa số đơn hàng không xuất phát từ chatbot.
     * ON DELETE SET NULL — xóa hội thoại không ảnh hưởng đơn hàng.
     * FK fk_dh_cht tương ứng với ALTER TABLE trong SQL (Module 11).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "cuoc_hoi_thoai_id",
            foreignKey = @ForeignKey(name = "fk_dh_cht")
    )
    private CuocHoiThoai cuocHoiThoai;

    /**
     * Kênh bán hàng (DEFAULT 'online').
     * Giá trị hợp lệ: "online" | "tai_quay"
     */
    @Column(name = "kenh_ban", nullable = false, length = 10)
    @Builder.Default
    private String kenhBan = "online";

    /** Ngày giờ khách đặt hàng (DEFAULT GETDATE()). */
    @Column(name = "ngay_dat", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime ngayDat = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // SNAPSHOT ĐỊA CHỈ GIAO HÀNG
    // Lưu cả FK lẫn các trường text để đảm bảo tính bất biến của lịch sử đơn hàng
    // -------------------------------------------------------------------------

    /**
     * FK tới địa chỉ giao hàng gốc của khách (ON DELETE SET NULL).
     * NULL sau khi khách xóa địa chỉ, nhưng các trường text snapshot vẫn còn.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "dia_chi_giao_id",
            foreignKey = @ForeignKey(name = "fk_dh_dc")
    )
    private DiaChiKhachHang diaChiGiao;

    /** Snapshot: tên người nhận tại thời điểm đặt hàng. */
    @Column(name = "ho_ten_nguoi_nhan", length = 100)
    private String hoTenNguoiNhan;

    /** Snapshot: SĐT người nhận tại thời điểm đặt hàng. */
    @Column(name = "sdt_nguoi_nhan", length = 20)
    private String sdtNguoiNhan;

    /** Snapshot: email người nhận tại thời điểm đặt hàng. */
    @Column(name = "email_nguoi_nhan", length = 100)
    private String emailNguoiNhan;

    /** Snapshot: số nhà, tên đường tại thời điểm đặt hàng. */
    @Column(name = "dia_chi_giao_cu_the", length = 255)
    private String diaChiGiaCuThe;

    /** Snapshot: tên phường/xã tại thời điểm đặt hàng. */
    @Column(name = "phuong_xa_giao", length = 100)
    private String phuongXaGiao;

    /** Snapshot: tên quận/huyện tại thời điểm đặt hàng. */
    @Column(name = "quan_huyen_giao", length = 100)
    private String quanHuyenGiao;

    /** Snapshot: tên tỉnh/thành phố tại thời điểm đặt hàng. */
    @Column(name = "tinh_thanh_giao", length = 100)
    private String tinhThanhGiao;

    // -------------------------------------------------------------------------
    // TÀI CHÍNH
    // -------------------------------------------------------------------------

    /** Tổng tiền hàng (chưa giảm giá, chưa cộng phí ship). */
    @Column(name = "tong_tien_hang", nullable = false, precision = 15, scale = 2)
    private BigDecimal tongTienHang;

    /** Số tiền được giảm (từ mã giảm giá hoặc hạng thành viên). DEFAULT 0. */
    @Column(name = "tien_giam_gia", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal tienGiamGia = BigDecimal.ZERO;

    /**
     * Phí vận chuyển lấy từ GHN API (DEFAULT 0).
     * Fallback về 0 nếu GHN API lỗi (system_rules.md §6).
     */
    @Column(name = "phi_ship", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal phiShip = BigDecimal.ZERO;

    /**
     * ⚠️ CỘT TÍNH TOÁN TỰ ĐỘNG (Computed Column - PERSISTED):
     * SQL Server tự tính: tong_tien_hang - tien_giam_gia + phi_ship
     * BẮT BUỘC insertable = false, updatable = false
     * để Hibernate không cố ghi vào cột này gây lỗi "Cannot update a computed column".
     */
    @Column(name = "tong_thanh_toan", insertable = false, updatable = false, precision = 15, scale = 2)
    private BigDecimal tongThanhToan;

    /**
     * Mã giảm giá được áp dụng cho đơn hàng này.
     * Nullable — đơn hàng không bắt buộc phải có mã giảm giá.
     * ON DELETE SET NULL (DB level) — xóa mã giảm giá không ảnh hưởng lịch sử đơn.
     * FK fk_dh_mgg tương ứng với ALTER TABLE trong SQL (Module 9).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "ma_giam_gia_id",
            foreignKey = @ForeignKey(name = "fk_dh_mgg")
    )
    private MaGiamGia maGiamGia;

    // -------------------------------------------------------------------------
    // VẬN CHUYỂN
    // -------------------------------------------------------------------------

    /**
     * Ngày giao hàng dự kiến (lấy từ GHN API /leadtime).
     * Chỉ lưu ngày, không lưu giờ.
     */
    @Column(name = "ngay_giao_du_kien")
    private LocalDate ngayGiaoDuKien;

    /** Ngày giờ giao hàng thực tế (điền khi cập nhật trạng thái 'da_giao'). */
    @Column(name = "ngay_giao_thuc_te")
    private LocalDateTime ngayGiaoThucTe;

    // -------------------------------------------------------------------------
    // TRẠNG THÁI ĐƠN HÀNG
    // -------------------------------------------------------------------------

    /**
     * Trạng thái xử lý đơn hàng (DEFAULT 'cho_xac_nhan').
     * Giá trị hợp lệ: "cho_xac_nhan" | "da_xac_nhan" | "dang_giao" | "da_giao" | "da_huy"
     * ⚠️ Khách chỉ được hủy khi = "cho_xac_nhan" (system_rules.md §2.2).
     */
    @Column(name = "trang_thai", nullable = false, length = 20)
    @Builder.Default
    private String trangThai = "cho_xac_nhan";

    /**
     * Trạng thái thanh toán (DEFAULT 'chua_thanh_toan').
     * Giá trị hợp lệ:
     *  "chua_thanh_toan" | "dang_cho_qr" | "da_thanh_toan" | "that_bai" | "hoan_tien"
     */
    @Column(name = "trang_thai_thanh_toan", nullable = false, length = 20)
    @Builder.Default
    private String trangThaiThanhToan = "chua_thanh_toan";

    /** Thời điểm hết hạn chờ thanh toán QR (NULL với COD). */
    @Column(name = "thoi_gian_het_han_tt")
    private LocalDateTime thoiGianHetHanTt;

    /** Ghi chú của khách hoặc nhân viên về đơn hàng. */
    @Column(name = "ghi_chu", columnDefinition = "NVARCHAR(MAX)")
    private String ghiChu;

    /** Thời điểm cập nhật đơn hàng gần nhất. */
    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 DonHang → nhiều ChiTietDonHang (ON DELETE CASCADE)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChiTietDonHang> chiTietDonHangs = new ArrayList<>();

    // -------------------------------------------------------------------------
    // Quan hệ 1-N: 1 DonHang → nhiều ThanhToan (lịch sử thanh toán)
    // -------------------------------------------------------------------------
    @OneToMany(mappedBy = "donHang", fetch = FetchType.LAZY)
    @Builder.Default
    private List<ThanhToan> thanhToans = new ArrayList<>();
}
