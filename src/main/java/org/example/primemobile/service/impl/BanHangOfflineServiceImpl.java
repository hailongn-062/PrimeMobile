package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IBanHangOfflineService;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Triển khai phân hệ Bán hàng Offline (tại quầy) cho PrimeMobile.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md §2.1):</h2>
 * <ul>
 *   <li>Đơn offline luôn gắn kênh bán {@code "tai_quay"}.</li>
 *   <li>Khách vãng lai (không để lại thông tin) → Gán vào tài khoản mặc định
 *       {@code so_dien_thoai = '0000000000'}.</li>
 *   <li>Hàng bán từ <b>Kho Tổng</b> ({@code loai = 'kho_tong'}) — không từ Kho Online.</li>
 *   <li><b>Safety Stock Rule (§3.1):</b> Sau khi trừ bán, tồn kho Kho Tổng KHÔNG được
 *       xuống dưới {@value #TON_KHO_TOI_THIEU} đơn vị / SKU.</li>
 *   <li>Khi hoàn tất thanh toán tại quầy:
 *       Trạng thái đơn → {@code "da_giao"}, thanh toán → {@code "da_thanh_toan"},
 *       tồn kho bị trừ ngay lập tức.</li>
 * </ul>
 *
 * <h2>Chiến lược quản lý Transaction:</h2>
 * <ul>
 *   <li>{@code taoDonHangMoi} — {@code @Transactional}: Tạo DonHang cần commit ngay.</li>
 *   <li>{@code themSanPhamVaoDon} — {@code @Transactional}: Validate + upsert ChiTietDonHang +
 *       cập nhật tongTienHang trong 1 transaction nguyên tử.</li>
 *   <li>{@code thanhToanDonHang} — {@code @Transactional}: Cập nhật đơn + tạo ThanhToan +
 *       trừ kho TOÀN BỘ trong 1 transaction — rollback nếu bất kỳ bước nào lỗi.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BanHangOfflineServiceImpl implements IBanHangOfflineService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ NGHIỆP VỤ
    // -----------------------------------------------------------------------

    /** Mức tồn kho tối thiểu bắt buộc (system_rules.md §3.1). */
    private static final int TON_KHO_TOI_THIEU = 5;

    /** SĐT của tài khoản khách lẻ mặc định (system_rules.md §2.1). */
    private static final String SDT_KHACH_LE_MAC_DINH = "0000000000";

    /** Loại kho phục vụ bán hàng offline. */
    private static final String LOAI_KHO_TONG = "kho_tong";

    /** Trạng thái đơn hàng đang nháp, chờ xác nhận (khớp với CHECK constraint DB). */
    private static final String TRANG_THAI_CHO_THANH_TOAN = "cho_xac_nhan";

    /** Pattern sinh mã đơn hàng: DH-YYYYMM-<millis 6 chữ số cuối>. */
    private static final DateTimeFormatter MA_DON_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------

    private final DonHangRepository            donHangRepository;
    private final ChiTietDonHangRepository     chiTietDonHangRepository;
    private final ThanhToanRepository          thanhToanRepository;
    private final KhachHangRepository          khachHangRepository;
    private final NguoiDungRepository          nguoiDungRepository;
    private final KhoRepository                khoRepository;
    private final TonKhoRepository             tonKhoRepository;
    private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    // Service được inject để lấy thông tin biến thể SKU
    private final IBienTheSanPhamService       bienTheSanPhamService;

    // =======================================================================
    // PUBLIC METHODS
    // =======================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng chi tiết:</h3>
     * <ol>
     *   <li>Validate nhân viên tồn tại và đang hoạt động.</li>
     *   <li>Lấy khách lẻ mặc định (sdt = '0000000000') để gắn vào đơn hàng.
     *       Controller có thể cập nhật khách hàng thực sau.</li>
     *   <li>Sinh mã đơn hàng và lưu.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DonHang taoDonHangMoi(Integer nhanVienId) {

        log.info("[BanHangOffline] Tạo đơn hàng mới — nhanVienId={}", nhanVienId);

        // ------------------------------------------------------------------
        // Bước 1: Validate nhân viên
        // ------------------------------------------------------------------
        NguoiDung nhanVien = nguoiDungRepository.findById(nhanVienId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy nhân viên có ID: " + nhanVienId));

        // ------------------------------------------------------------------
        // Bước 2: Lấy tài khoản khách lẻ mặc định (system_rules.md §2.1)
        // ------------------------------------------------------------------
        KhachHang khachLe = khachHangRepository.findBySoDienThoai(SDT_KHACH_LE_MAC_DINH)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy tài khoản khách lẻ mặc định (sdt='" +
                        SDT_KHACH_LE_MAC_DINH + "'). Vui lòng kiểm tra dữ liệu khởi tạo."));

        // ------------------------------------------------------------------
        // Bước 3: Build và lưu DonHang
        // ------------------------------------------------------------------
        String maDonHang = sinhMaDonHang();

        DonHang donHang = DonHang.builder()
                .maDonHang(maDonHang)
                .khachHang(khachLe)
                .nguoiXuLy(nhanVien)
                .kenhBan("tai_quay")                    // §2.1: Bán hàng offline
                .ngayDat(LocalDateTime.now())
                .tongTienHang(BigDecimal.ZERO)          // Sẽ cập nhật khi thêm sản phẩm
                .tienGiamGia(BigDecimal.ZERO)
                .phiShip(BigDecimal.ZERO)               // Bán tại quầy: không có phí ship
                .trangThai(TRANG_THAI_CHO_THANH_TOAN)   // Trạng thái nháp nội bộ
                .trangThaiThanhToan("chua_thanh_toan")
                .updatedAt(LocalDateTime.now())
                .build();

        donHang = donHangRepository.save(donHang);

        log.info("[BanHangOffline] Đã tạo đơn hàng — maDonHang={}, id={}",
                maDonHang, donHang.getId());
        return donHang;
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng chi tiết:</h3>
     * <ol>
     *   <li>Validate đơn hàng tồn tại và đang ở {@code "cho_xac_nhan"}.</li>
     *   <li>Lấy thông tin SKU (giá bán hiện tại) qua {@link IBienTheSanPhamService}.</li>
     *   <li><b>Safety Stock Check tại Kho Tổng:</b>
     *       Tồn kho sau khi trừ {@code soLuong} phải {@code >= 5}.</li>
     *   <li><b>Upsert ChiTietDonHang:</b>
     *       Nếu SKU đã có trong đơn → cộng dồn số lượng.
     *       Nếu chưa có → tạo mới với price snapshot tại thời điểm thêm.</li>
     *   <li>Tính lại {@code tong_tien_hang} = SUM(donGia × soLuong) toàn đơn.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DonHang themSanPhamVaoDon(Integer donHangId,
                                     Integer bienTheSanPhamId,
                                     int soLuong) {

        log.info("[BanHangOffline] Thêm sản phẩm — donHangId={}, bienTheId={}, soLuong={}",
                donHangId, bienTheSanPhamId, soLuong);

        // ------------------------------------------------------------------
        // Bước 1: Validate soLuong
        // ------------------------------------------------------------------
        if (soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }

        // ------------------------------------------------------------------
        // Bước 2: Validate đơn hàng tồn tại và đang ở trạng thái nháp
        // ------------------------------------------------------------------
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng có ID: " + donHangId));

        if (!TRANG_THAI_CHO_THANH_TOAN.equals(donHang.getTrangThai())) {
            throw new IllegalStateException(String.format(
                    "Đơn hàng [%s] đang ở trạng thái '%s', không thể thêm sản phẩm. " +
                    "Chỉ được thêm khi đơn đang ở trạng thái '%s'.",
                    donHang.getMaDonHang(), donHang.getTrangThai(), TRANG_THAI_CHO_THANH_TOAN));
        }

        // ------------------------------------------------------------------
        // Bước 3: Lấy thông tin biến thể SKU (ném EntityNotFoundException nếu không có)
        // ------------------------------------------------------------------
        BienTheSanPham bienThe = bienTheSanPhamService.getBienTheSanPham(bienTheSanPhamId);

        // Lấy giá bán hiện tại: ưu tiên giá khuyến mãi nếu đang áp dụng
        BigDecimal donGia = (bienThe.getGiaKhuyenMai() != null)
                ? bienThe.getGiaKhuyenMai()
                : bienThe.getGiaBan();

        // ------------------------------------------------------------------
        // Bước 4: SAFETY STOCK CHECK tại Kho Tổng (system_rules.md §3.1)
        // ------------------------------------------------------------------
        Kho khoTong = timKhoTong();

        TonKho tonKho = tonKhoRepository
                .findByKhoAndBienTheSanPham(khoTong, bienThe)
                .orElseThrow(() -> new EntityNotFoundException(String.format(
                        "Biến thể [%s] chưa có trong Kho Tổng. " +
                        "Vui lòng nhập hàng trước khi bán.", bienThe.getMaSku())));

        // Tính tổng số lượng sẽ bán (bao gồm cả số đã có trong đơn nếu upsert)
        int soLuongDaTrongDon = chiTietDonHangRepository
                .findByDonHangIdAndBienTheSanPhamId(donHangId, bienTheSanPhamId)
                .map(ChiTietDonHang::getSoLuong)
                .orElse(0);

        // Tổng số lượng "chiếm dụng" trong kho sau thao tác này
        int tongSoLuongSauKhiThem = soLuongDaTrongDon + soLuong;
        int tonKhoSauKhiTru = tonKho.getSoLuong() - tongSoLuongSauKhiThem;

        // ================================================================
        // ⚠️ RÀNG BUỘC SỐNG CÒN — SAFETY STOCK RULE (system_rules.md §3.1)
        // ================================================================
        if (tonKhoSauKhiTru < TON_KHO_TOI_THIEU) {
            throw new IllegalArgumentException(String.format(
                    "Vi phạm quy tắc tồn kho. Số lượng còn lại tối thiểu phải là %d. " +
                    "Sản phẩm [%s] tại Kho Tổng: " +
                    "Tồn kho = %d, Đã có trong đơn = %d, Muốn thêm = %d, " +
                    "Còn lại sau khi bán = %d (< %d).",
                    TON_KHO_TOI_THIEU,
                    bienThe.getMaSku(),
                    tonKho.getSoLuong(), soLuongDaTrongDon, soLuong,
                    tonKhoSauKhiTru, TON_KHO_TOI_THIEU));
        }
        // ================================================================

        // ------------------------------------------------------------------
        // Bước 5: UPSERT ChiTietDonHang (thêm mới hoặc cộng dồn số lượng)
        // ------------------------------------------------------------------
        ChiTietDonHang chiTiet = chiTietDonHangRepository
                .findByDonHangIdAndBienTheSanPhamId(donHangId, bienTheSanPhamId)
                .map(existing -> {
                    // SKU đã có → cộng dồn số lượng
                    log.debug("[BanHangOffline] Cộng dồn SKU [{}]: {} + {} = {}",
                            bienThe.getMaSku(), existing.getSoLuong(), soLuong,
                            existing.getSoLuong() + soLuong);
                    existing.setSoLuong(existing.getSoLuong() + soLuong);
                    return existing;
                })
                .orElseGet(() -> {
                    // SKU chưa có → tạo mới với price snapshot
                    log.debug("[BanHangOffline] Thêm mới SKU [{}] vào đơn", bienThe.getMaSku());
                    return ChiTietDonHang.builder()
                            .donHang(donHang)
                            .bienTheSanPham(bienThe)
                            .soLuong(soLuong)
                            .donGiaBan(donGia)    // Price snapshot tại thời điểm thêm
                            .build();
                });

        chiTietDonHangRepository.save(chiTiet);

        // ------------------------------------------------------------------
        // Bước 6: Tính lại tổng tiền hàng (tong_tien_hang) cho toàn đơn
        // ------------------------------------------------------------------
        BigDecimal tongTienHang = tinhLaiTongTien(donHangId);
        donHang.setTongTienHang(tongTienHang);
        donHang.setUpdatedAt(LocalDateTime.now());
        donHangRepository.save(donHang);

        log.info("[BanHangOffline] Đã thêm sản phẩm [{}] x{} vào đơn [{}]. Tổng tiền = {}",
                bienThe.getMaSku(), soLuong, donHang.getMaDonHang(), tongTienHang);
        return donHang;
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng chi tiết:</h3>
     * <ol>
     *   <li>Validate đơn hàng đang ở {@code "cho_xac_nhan"} và có ít nhất 1 sản phẩm.</li>
     *   <li>Validate phương thức thanh toán tồn tại.</li>
     *   <li>Cập nhật trạng thái đơn hàng: {@code "da_giao"} + {@code "da_thanh_toan"}
     *       (Bán tại quầy = giao hàng tức thì theo system_rules.md §2.1).</li>
     *   <li>Tạo bản ghi {@link ThanhToan} với {@code trang_thai = "thanh_cong"}.</li>
     *   <li>Duyệt từng {@link ChiTietDonHang} → trừ tồn kho Kho Tổng thực tế.
     *       Kiểm tra Safety Stock lần cuối trước khi trừ.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DonHang thanhToanDonHang(Integer donHangId, Integer phuongThucThanhToanId) {

        log.info("[BanHangOffline] Thanh toán đơn hàng — donHangId={}, ptttId={}",
                donHangId, phuongThucThanhToanId);

        // ------------------------------------------------------------------
        // Bước 1: Validate đơn hàng
        // ------------------------------------------------------------------
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng có ID: " + donHangId));

        if (!TRANG_THAI_CHO_THANH_TOAN.equals(donHang.getTrangThai())) {
            throw new IllegalStateException(String.format(
                    "Đơn hàng [%s] đang ở trạng thái '%s', không thể thanh toán.",
                    donHang.getMaDonHang(), donHang.getTrangThai()));
        }

        // ------------------------------------------------------------------
        // Bước 2: Lấy danh sách chi tiết đơn — eager-load để tránh N+1 query
        // ------------------------------------------------------------------
        List<ChiTietDonHang> danhSachChiTiet =
                chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

        if (danhSachChiTiet.isEmpty()) {
            throw new IllegalStateException(
                    "Đơn hàng [" + donHang.getMaDonHang() + "] chưa có sản phẩm nào. " +
                    "Vui lòng thêm sản phẩm trước khi thanh toán.");
        }

        // ------------------------------------------------------------------
        // Bước 3: Validate phương thức thanh toán
        // ------------------------------------------------------------------
        PhuongThucThanhToan pttt = phuongThucThanhToanRepository
                .findById(phuongThucThanhToanId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phương thức thanh toán có ID: " + phuongThucThanhToanId));

        // ------------------------------------------------------------------
        // Bước 4: TRỪ KHO THỰC TẾ — Chiến lược Fail-Fast (Pre-validate ALL trước)
        // Kiểm tra toàn bộ trước khi trừ bất kỳ dòng nào → Rollback hoàn toàn nếu lỗi
        // ------------------------------------------------------------------
        Kho khoTong = timKhoTong();

        // PRE-VALIDATE: Kiểm tra tất cả SKU trước khi trừ kho
        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
            BienTheSanPham bienThe = chiTiet.getBienTheSanPham();
            TonKho tonKho = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoTong, bienThe)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(
                            "Không tìm thấy tồn kho cho [%s] tại Kho Tổng.",
                            bienThe.getMaSku())));

            int tonKhoSauTru = tonKho.getSoLuong() - chiTiet.getSoLuong();
            if (tonKhoSauTru < TON_KHO_TOI_THIEU) {
                throw new IllegalArgumentException(String.format(
                        "Vi phạm quy tắc tồn kho khi hoàn tất thanh toán. " +
                        "Sản phẩm [%s]: Tồn kho = %d, Bán = %d, Còn lại = %d (< %d).",
                        bienThe.getMaSku(), tonKho.getSoLuong(),
                        chiTiet.getSoLuong(), tonKhoSauTru, TON_KHO_TOI_THIEU));
            }
        }

        // EXECUTE: Tất cả đã pass → Trừ kho từng dòng
        LocalDateTime now = LocalDateTime.now();
        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
            TonKho tonKho = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoTong, chiTiet.getBienTheSanPham())
                    .orElseThrow(); // Đã kiểm tra ở trên, không thể null

            tonKho.setSoLuong(tonKho.getSoLuong() - chiTiet.getSoLuong());
            tonKho.setUpdatedAt(now);
            tonKhoRepository.save(tonKho);

            log.debug("[BanHangOffline] Trừ kho [{}]: -{} → còn {}",
                    chiTiet.getBienTheSanPham().getMaSku(),
                    chiTiet.getSoLuong(), tonKho.getSoLuong());
        }

        // ------------------------------------------------------------------
        // Bước 5: Cập nhật trạng thái đơn hàng
        // Bán tại quầy = giao hàng tức thì (system_rules.md §2.1)
        // ------------------------------------------------------------------
        donHang.setTrangThai("da_giao");
        donHang.setTrangThaiThanhToan("da_thanh_toan");
        donHang.setNgayGiaoThucTe(now);
        donHang.setUpdatedAt(now);
        donHangRepository.save(donHang);

        // ------------------------------------------------------------------
        // Bước 6: Lưu lịch sử thanh toán
        // Dùng tongThanhToan (computed column) làm so_tien chính xác
        // Nếu tongThanhToan chưa được DB tính (session mới), dùng tongTienHang
        // ------------------------------------------------------------------
        BigDecimal soTien = donHang.getTongThanhToan() != null
                ? donHang.getTongThanhToan()
                : donHang.getTongTienHang();

        ThanhToan thanhToan = ThanhToan.builder()
                .donHang(donHang)
                .phuongThucThanhToan(pttt)
                .soTien(soTien)
                .soTienThucTe(soTien)               // Tại quầy: tiền nhận = tiền hóa đơn
                .trangThai("thanh_cong")
                .thoiGianTao(now)
                .thoiGianThanhCong(now)
                .build();

        thanhToanRepository.save(thanhToan);

        log.info("[BanHangOffline] Hoàn tất thanh toán — maDonHang={}, soTien={}, pttt={}",
                donHang.getMaDonHang(), soTien, pttt.getTenPttt());
        return donHang;
    }

    // =======================================================================
    // PRIVATE HELPER METHODS
    // =======================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng chi tiết:</h3>
     * <ol>
     *   <li>Validate đơn hàng tồn tại và đang ở trạng thái có thể sửa ({@code "cho_xac_nhan"}).</li>
     *   <li>Xác định khách hàng theo thứ tự ưu tiên:
     *       <ol type="a">
     *         <li>Nếu {@code khachHangId != null} → Query DB.
     *             Nếu tìm thấy → dùng khách đó.</li>
     *         <li>Nếu {@code khachHangId == null} HOẶC không tìm thấy trong DB →
     *             Tự động lấy tài khoản khách lẻ mặc định (sdt = '0000000000').</li>
     *       </ol>
     *   </li>
     *   <li>Gán {@code khachHang} vào đơn hàng và lưu.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DonHang capNhatKhachHangChoDon(Integer donHangId, Integer khachHangId) {

        log.info("[BanHangOffline] Cập nhật khách hàng — donHangId={}, khachHangId={}",
                donHangId, khachHangId);

        // ------------------------------------------------------------------
        // Bước 1: Validate đơn hàng tồn tại
        // ------------------------------------------------------------------
        DonHang donHang = donHangRepository.findById(donHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy đơn hàng có ID: " + donHangId));

        if (!TRANG_THAI_CHO_THANH_TOAN.equals(donHang.getTrangThai())) {
            throw new IllegalStateException(String.format(
                    "Đơn hàng [%s] đang ở trạng thái '%s', không thể thay đổi khách hàng.",
                    donHang.getMaDonHang(), donHang.getTrangThai()));
        }

        // ------------------------------------------------------------------
        // Bước 2: Chốt khách hàng — Optional-based resolution
        // ------------------------------------------------------------------
        KhachHang khachHang;

        Optional<KhachHang> khachOptional = Optional.ofNullable(khachHangId)
                .flatMap(khachHangRepository::findById);

        if (khachOptional.isPresent()) {
            // Trường hợp 1: Khách có tài khoản hợp lệ
            khachHang = khachOptional.get();
            log.info("[BanHangOffline] Gán khách hàng id={} ('{}') vào đơn [{}].",
                    khachHang.getId(), khachHang.getHoTen(), donHang.getMaDonHang());
        } else {
            // Trường hợp 2: khachHangId null hoặc không tìm thấy → Khách lẻ mặc định
            if (khachHangId != null) {
                // ID được cung cấp nhưng không tồn tại trong DB → cảnh báo, fallback
                log.warn("[BanHangOffline] Không tìm thấy khách hàng id={}. " +
                         "Tự động gán vào tài khoản khách lẻ mặc định.", khachHangId);
            } else {
                log.info("[BanHangOffline] khachHangId = null. Gán vào tài khoản khách lẻ mặc định.");
            }
            khachHang = khachHangRepository.findBySoDienThoai(SDT_KHACH_LE_MAC_DINH)
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy tài khoản khách lẻ mặc định (sdt='" +
                            SDT_KHACH_LE_MAC_DINH + "'). Vui lòng kiểm tra dữ liệu khởi tạo DB."));
        }

        // ------------------------------------------------------------------
        // Bước 3: Gán và lưu
        // ------------------------------------------------------------------
        donHang.setKhachHang(khachHang);
        donHang.setUpdatedAt(LocalDateTime.now());
        donHang = donHangRepository.save(donHang);

        log.info("[BanHangOffline] Đã cập nhật khách hàng cho đơn [{}] → khachHangId={}.",
                donHang.getMaDonHang(), khachHang.getId());
        return donHang;
    }

    /**
     * Tìm Kho Tổng từ database theo loai = 'kho_tong'.
     * Throw rõ ràng nếu chưa có dữ liệu khởi tạo kho trong DB.
     */
    private Kho timKhoTong() {
        return khoRepository.findByLoai(LOAI_KHO_TONG)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy Kho Tổng trong hệ thống. " +
                        "Vui lòng kiểm tra dữ liệu khởi tạo bảng kho."));
    }

    /**
     * Tính lại tổng tiền hàng của đơn hàng bằng cách sum toàn bộ chi tiết.
     * <p>
     * Dùng Java Stream + BigDecimal.add để đảm bảo độ chính xác tài chính.
     * KHÔNG dùng double/float vì sai số dấu phẩy động.
     *
     * @param donHangId ID đơn hàng cần tính lại.
     * @return Tổng tiền hàng (BigDecimal, >= 0).
     */
    private BigDecimal tinhLaiTongTien(Integer donHangId) {
        return chiTietDonHangRepository
                .findByDonHangId(donHangId)
                .stream()
                .map(ct -> ct.getDonGiaBan().multiply(BigDecimal.valueOf(ct.getSoLuong())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Sinh mã đơn hàng tự động theo format: {@code DH-YYYYMMDD-<6 chữ số cuối millis>}.
     * <p>
     * Ví dụ: {@code DH-20240615-334521}.
     */
    private String sinhMaDonHang() {
        String datePart = LocalDateTime.now().format(MA_DON_DATE_FMT);
        String randPart = String.format("%06d", System.currentTimeMillis() % 1_000_000L);
        return "DH-" + datePart + "-" + randPart;
    }
}
