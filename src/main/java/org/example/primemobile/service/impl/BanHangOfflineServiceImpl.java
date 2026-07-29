package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IBanHangOfflineService;
import org.example.primemobile.service.IBaoHanhService;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.dto.request.PosThanhToanRequest;
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
 * <li>Đơn offline luôn gắn kênh bán {@code "tai_quay"}.</li>
 * <li>Khách vãng lai (không để lại thông tin) → Gán vào tài khoản mặc định
 * {@code so_dien_thoai = '0000000000'}.</li>
 * <li>Hàng bán từ <b>Kho Tổng</b> ({@code loai = 'kho_tong'}) — không từ Kho
 * Online.</li>
 * <li><b>Stock Rule (§3.1):</b> Sau khi trừ bán, tồn kho Kho Tổng KHÔNG
 * được âm. Không áp dụng mức dự trữ tối thiểu.</li>
 * <li>Khi hoàn tất thanh toán tại quầy:
 * Trạng thái đơn → {@code "da_hoan_thanh"}, thanh toán → {@code "da_thanh_toan"},
 * tồn kho bị trừ ngay lập tức.</li>
 * </ul>
 *
 * <h2>Chiến lược quản lý Transaction:</h2>
 * <ul>
 * <li>{@code taoDonHangMoi} — {@code @Transactional}: Tạo DonHang cần commit
 * ngay.</li>
 * <li>{@code themSanPhamVaoDon} — {@code @Transactional}: Validate + upsert
 * ChiTietDonHang + cập nhật tongTienHang trong 1 transaction nguyên tử.</li>
 * <li>{@code thanhToanDonHang} — {@code @Transactional}: Cập nhật đơn + tạo
 * ThanhToan + trừ kho TOÀN BỘ trong 1 transaction — rollback nếu bất kỳ bước
 * nào lỗi.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BanHangOfflineServiceImpl implements IBanHangOfflineService {

        // -----------------------------------------------------------------------
        // HẰNG SỐ NGHIỆP VỤ
        // -----------------------------------------------------------------------

        /** Số lượng tồn kho tối thiểu được phép bán = 0 (không áp dụng Safety Stock — §3.1).
         * Bị chặn khi tồn kho sau khi trừ < 0 (tức âm kho). */

        /** SĐT của tài khoản khách lẻ mặc định (system_rules.md §2.1). */
        /**
         * Trạng thái đơn hàng đang nháp, chờ xác nhận (khớp với CHECK constraint DB).
         */
        private static final String TRANG_THAI_CHO_THANH_TOAN = "cho_thanh_toan";

        /** Pattern sinh mã đơn hàng: DH-YYYYMM-<millis 6 chữ số cuối>. */
        private static final DateTimeFormatter MA_DON_DATE_FMT = DateTimeFormatter.ofPattern("yyMMdd");

        // -----------------------------------------------------------------------
        // DEPENDENCIES
        // -----------------------------------------------------------------------

        private final DonHangRepository donHangRepository;
        private final ChiTietDonHangRepository chiTietDonHangRepository;
        private final ThanhToanRepository thanhToanRepository;
        private final KhachHangRepository khachHangRepository;
        private final NguoiDungRepository nguoiDungRepository;
        private final KhoRepository khoRepository;
        private final TonKhoRepository tonKhoRepository;
        private final PhuongThucThanhToanRepository phuongThucThanhToanRepository;
        private final MayDienThoaiRepository mayDienThoaiRepository;
        private final ChuongTrinhKhuyenMaiRepository ctkmRepository;

        // Service được inject để lấy thông tin biến thể SKU
        private final IBienTheSanPhamService bienTheSanPhamService;

        // Service tính giá sau khuyến mãi động
        private final IKhuyenMaiService khuyenMaiService;

        // Service tự động tạo phiếu bảo hành sau khi bán
        private final IBaoHanhService baoHanhService;

        // =======================================================================
        // PUBLIC METHODS
        // =======================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Luồng chi tiết:</h3>
         * <ol>
         * <li>Validate nhân viên tồn tại và đang hoạt động.</li>
         * <li>Lấy thông tin khách hàng để gắn vào đơn hàng.</li>
         * <li>Sinh mã đơn hàng và lưu.</li>
         * </ol>
         */
        @Override
        @Transactional
        public DonHang taoDonHangMoi(Integer nhanVienId, Integer khachHangId) {

                log.info("[BanHangOffline] Tạo đơn hàng mới — nhanVienId={}, khachHangId={}", nhanVienId, khachHangId);

                // ------------------------------------------------------------------
                // Bước 1: Validate nhân viên
                // ------------------------------------------------------------------
                NguoiDung nhanVien = nguoiDungRepository.findById(nhanVienId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy nhân viên có ID: " + nhanVienId));

                // ------------------------------------------------------------------
                // Bước 2: Lấy thông tin khách hàng
                // ------------------------------------------------------------------
                if (khachHangId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn hoặc thêm khách hàng để tạo đơn hàng. (Bắt buộc phục vụ bảo hành)");
                }
                KhachHang khachHang = khachHangRepository.findById(khachHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy khách hàng có ID: " + khachHangId));

                // ------------------------------------------------------------------
                // Bước 3: Build và lưu DonHang
                // ------------------------------------------------------------------
                String maDonHang = sinhMaDonHang();

                DonHang donHang = DonHang.builder()
                                .maDonHang(maDonHang)
                                .khachHang(khachHang)
                                .nguoiXuLy(nhanVien)
                                .kenhBan("tai_quay") // §2.1: Bán hàng offline
                                .ngayDat(LocalDateTime.now())
                                .tongTienHang(BigDecimal.ZERO) // Sẽ cập nhật khi thêm sản phẩm
                                .tienGiamGia(BigDecimal.ZERO)
                                .phiShip(BigDecimal.ZERO) // Bán tại quầy: không có phí ship
                                .trangThai(TRANG_THAI_CHO_THANH_TOAN) // Trạng thái nháp nội bộ
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
         * <li>Validate đơn hàng tồn tại và đang ở {@code "cho_xac_nhan"}.</li>
         * <li>Lấy thông tin SKU (giá bán hiện tại) qua
         * {@link IBienTheSanPhamService}.</li>
         * <li><b>Safety Stock Check tại Kho Tổng:</b>
         * Tồn kho sau khi trừ {@code soLuong} phải {@code >= 5}.</li>
         * <li><b>Upsert ChiTietDonHang:</b>
         * Nếu SKU đã có trong đơn → cộng dồn số lượng.
         * Nếu chưa có → tạo mới với price snapshot tại thời điểm thêm.</li>
         * <li>Tính lại {@code tong_tien_hang} = SUM(donGia × soLuong) toàn đơn.</li>
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
                // Bước 3: Lấy thông tin biến thể SKU và tính giá sau khuyến mãi động
                // ------------------------------------------------------------------
                BienTheSanPham bienThe = bienTheSanPhamService.getBienTheSanPham(bienTheSanPhamId);

                // Lấy giá bán hiện tại: ưu tiên khuyến mãi động
                BigDecimal tongTienHienTai = donHang.getTongTienHang();
                BigDecimal donGia = khuyenMaiService.tinhGiaSauKhuyenMai(bienTheSanPhamId, tongTienHienTai);
                if (donGia == null) {
                        // Fallback sang giá gốc nếu không có khuyến mãi
                        donGia = bienThe.getGiaBan();
                }

                // ------------------------------------------------------------------
                // Bước 4: SAFETY STOCK CHECK tại Kho Tổng (system_rules.md §3.1)
                // ------------------------------------------------------------------
                Kho khoTong = timKhoTong();

                TonKho tonKho = tonKhoRepository
                                .findByKhoAndBienTheSanPham(khoTong, bienThe)
                                .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                "Biến thể [%s] chưa có trong Kho Tổng. " +
                                                                "Vui lòng nhập hàng trước khi bán.",
                                                bienThe.getMaSku())));

                // Tính tổng số lượng sẽ bán (bao gồm cả số đã có trong đơn nếu upsert)
                int soLuongDaTrongDon = chiTietDonHangRepository
                                .findByDonHangIdAndBienTheSanPhamId(donHangId, bienTheSanPhamId)
                                .map(ChiTietDonHang::getSoLuong)
                                .orElse(0);

                // Tổng số lượng "chiếm dụng" trong kho sau thao tác này
                int tongSoLuongSauKhiThem = soLuongDaTrongDon + soLuong;
                int tonKhoSauKhiTru = tonKho.getSoLuong() - tongSoLuongSauKhiThem;

                // ================================================================
                // ⚠️ CHECK TỒN KHO
                // ================================================================
                if (tonKhoSauKhiTru < 0) {
                        throw new IllegalArgumentException(String.format(
                                        "Không đủ tồn kho. " +
                                                        "Sản phẩm [%s] tại Kho Tổng: " +
                                                        "Tồn kho = %d, Cần bán (bao gồm đã có trong đơn) = %d.",
                                        bienThe.getMaSku(),
                                        tonKho.getSoLuong(), tongSoLuongSauKhiThem));
                }
                // ================================================================

                // ------------------------------------------------------------------
                // Bước 5: UPSERT ChiTietDonHang (thêm mới hoặc cộng dồn số lượng)
                // ------------------------------------------------------------------
                ChiTietDonHang chiTiet;
                Optional<ChiTietDonHang> existingOpt = chiTietDonHangRepository
                                .findByDonHangIdAndBienTheSanPhamId(donHangId, bienTheSanPhamId);

                if (existingOpt.isPresent()) {
                        // SKU đã có → cộng dồn số lượng
                        chiTiet = existingOpt.get();
                        log.debug("[BanHangOffline] Cộng dồn SKU [{}]: {} + {} = {}",
                                        bienThe.getMaSku(), chiTiet.getSoLuong(), soLuong,
                                        chiTiet.getSoLuong() + soLuong);
                        chiTiet.setSoLuong(chiTiet.getSoLuong() + soLuong);
                } else {
                        // SKU chưa có → tạo mới với price snapshot
                        log.debug("[BanHangOffline] Thêm mới SKU [{}] vào đơn", bienThe.getMaSku());
                        chiTiet = ChiTietDonHang.builder()
                                        .donHang(donHang)
                                        .bienTheSanPham(bienThe)
                                        .soLuong(soLuong)
                                        .donGiaBan(donGia) // Price snapshot tại thời điểm thêm
                                        .build();
                }

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
         * <li>Validate đơn hàng đang ở {@code "cho_xac_nhan"} và có ít nhất 1 sản
         * phẩm.</li>
         * <li>Validate phương thức thanh toán tồn tại.</li>
         * <li>Cập nhật trạng thái đơn hàng: {@code "da_hoan_thanh"} + {@code "da_thanh_toan"}
         * (Bán tại quầy = giao hàng tức thì theo system_rules.md §2.1).</li>
         * <li>Tạo bản ghi {@link ThanhToan} với {@code trang_thai = "thanh_cong"}.</li>
         * <li>Duyệt từng {@link ChiTietDonHang} → trừ tồn kho Kho Tổng thực tế.
         * Kiểm tra chống âm kho lần cuối trước khi trừ.</li>
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

                if (!TRANG_THAI_CHO_THANH_TOAN.equals(donHang.getTrangThai()) && !"don_hang_cho".equals(donHang.getTrangThai())) {
                        throw new IllegalStateException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái '%s', không thể thanh toán.",
                                        donHang.getMaDonHang(), donHang.getTrangThai()));
                }

                if (donHang.getKhachHang() == null) {
                    throw new IllegalArgumentException("Vui lòng chọn khách hàng trước khi thanh toán. (Bắt buộc phục vụ bảo hành)");
                }

                // ------------------------------------------------------------------
                // Bước 2: Lấy danh sách chi tiết đơn — eager-load để tránh N+1 query
                // ------------------------------------------------------------------
                List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

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
                                                "Không tìm thấy phương thức thanh toán có ID: "
                                                                + phuongThucThanhToanId));

                // ------------------------------------------------------------------
                // Bước 4: TRỪ KHO THỰC TẾ
                // ------------------------------------------------------------------
                Kho khoTong = timKhoTong();

                // EXECUTE: Dùng Pessimistic Lock khi trừ kho thực tế (§3.1 chống race condition)
                LocalDateTime now = LocalDateTime.now();
                for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPhamForUpdate(khoTong, chiTiet.getBienTheSanPham())
                                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                        "Không tìm thấy tồn kho cho [%s] tại Kho Tổng.",
                                                        chiTiet.getBienTheSanPham().getMaSku())));

                        int soLuongMoi = tonKho.getSoLuong() - chiTiet.getSoLuong();

                        // ⚠️ GUARD chống âm kho — lớp bảo vệ thứ 2 (lớp 1 đã kiểm tra Safety Stock phía trên)
                        if (soLuongMoi < 0) {
                                throw new IllegalStateException(String.format(
                                                "[BanHangOffline] Tồn kho không được âm sau khi trừ. "
                                                + "SKU [%s]: tồn=%d, bán=%d, mới=%d.",
                                                chiTiet.getBienTheSanPham().getMaSku(),
                                                tonKho.getSoLuong(), chiTiet.getSoLuong(), soLuongMoi));
                        }

                        tonKho.setSoLuong(soLuongMoi);
                        tonKho.setUpdatedAt(now);
                        tonKhoRepository.save(tonKho);

                        log.debug("[BanHangOffline] Trừ kho [{}]: -{} → còn {}",
                                        chiTiet.getBienTheSanPham().getMaSku(),
                                        chiTiet.getSoLuong(), tonKho.getSoLuong());
                }

                // ------------------------------------------------------------------
                // Bước 5: Cập nhật trạng thái đơn hàng
                // Bán tại quầy = giao hàng tức thì, đơn hàng hoàn thành (system_rules.md §2.1)
                // ------------------------------------------------------------------
                donHang.setTrangThai("da_hoan_thanh");
                donHang.setTrangThaiThanhToan("da_thanh_toan");
                donHang.setNgayGiaoThucTe(now);
                donHang.setUpdatedAt(now);
                donHangRepository.save(donHang);
                
                // Chốt toàn bộ máy điện thoại của đơn này thành da_ban
                List<MayDienThoai> imeiList = mayDienThoaiRepository.findByDonHangId(donHang.getId());
                for (MayDienThoai may : imeiList) {
                        may.setTinhTrang("da_ban");
                        may.setNguoiGiu(null);
                        may.setThoiGianGiu(null);
                        mayDienThoaiRepository.save(may);
                }
                donHang.setUpdatedAt(now);
                donHangRepository.save(donHang);

                // Tự động tạo Phiếu bảo hành cho các IMEI trong đơn
                baoHanhService.taoPhieuBaoHanhChoDonHang(donHang);
                log.info("[BanHangOffline] Đã tạo phiếu BH cho đơn [{}].", donHang.getMaDonHang());

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
                                .soTienThucTe(soTien) // Tại quầy: tiền nhận = tiền hóa đơn
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
         * <li>Validate đơn hàng tồn tại và đang ở trạng thái có thể sửa
         * ({@code "cho_xac_nhan"}).</li>
         * <li>Gán khách hàng vào đơn hàng và lưu.</li>
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
                // Bước 2: Chốt khách hàng
                // ------------------------------------------------------------------
                if (khachHangId == null) {
                    throw new IllegalArgumentException("Vui lòng chọn hoặc thêm khách hàng. (Bắt buộc phục vụ bảo hành)");
                }
                
                KhachHang khachHang = khachHangRepository.findById(khachHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy khách hàng có ID: " + khachHangId));

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
         * Lấy kho duy nhất trong hệ thống (ID = 1).
         * Throw rõ ràng nếu chưa có dữ liệu khởi tạo kho trong DB.
         */
        private Kho timKhoTong() {
                return khoRepository.findById(1)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy kho ID=1 trong hệ thống. " +
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
         * Sinh mã đơn hàng tự động theo format:
         * {@code DH-YYYYMMDD-<6 chữ số cuối millis>}.
         * <p>
         * Ví dụ: {@code DH-20240615-334521}.
         */
        private String sinhMaDonHang() {
                String datePart = LocalDateTime.now().format(MA_DON_DATE_FMT);
                String randPart = String.format("%04d", System.currentTimeMillis() % 10_000L);
                return "DH" + datePart + randPart;
        }

        // =======================================================================
        // ĐƠN HÀNG CHỜ (MODULE 7)
        // =======================================================================

        @Override
        @Transactional
        public DonHang luuDonHangCho(Integer donHangId, Integer nhanVienId, org.example.primemobile.dto.request.PosThanhToanRequest payload) {
                log.info("[BanHangOffline] Lưu đơn hàng chờ — donHangId={}, nhanVienId={}", donHangId, nhanVienId);

                DonHang donHang;
                if (donHangId == null) {
                        // Tạo đơn mới
                        NguoiDung nhanVien = nguoiDungRepository.findById(nhanVienId)
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên"));

                        if (payload.getKhachHangId() == null) {
                            throw new IllegalArgumentException("Vui lòng chọn hoặc thêm khách hàng để tạo đơn hàng. (Bắt buộc phục vụ bảo hành)");
                        }
                        KhachHang khachHang = khachHangRepository.findById(payload.getKhachHangId())
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Khách hàng"));

                        BigDecimal tienGiamGia = BigDecimal.ZERO;
                        ChuongTrinhKhuyenMai ctkm = null;

                        if (payload.getCtkmId() != null) {
                                KhuyenMaiResult kmResult = khuyenMaiService.apDungCtkmTheoId(payload.getCtkmId(), payload.getTongTien());
                                tienGiamGia = kmResult.getTienGiam();
                                ctkm = ctkmRepository.findById(payload.getCtkmId()).orElse(null);
                        } else {
                                KhuyenMaiResult kmResult = khuyenMaiService.tinhKhuyenMaiChoDonHang(payload.getTongTien());
                                tienGiamGia = kmResult.getTienGiam();
                                if (kmResult.getCtkmId() != null) {
                                        ctkm = ctkmRepository.findById(kmResult.getCtkmId()).orElse(null);
                                }
                        }

                        donHang = DonHang.builder()
                                        .maDonHang(sinhMaDonHang())
                                        .khachHang(khachHang)
                                        .nguoiXuLy(nhanVien)
                                        .kenhBan("tai_quay")
                                        .ngayDat(LocalDateTime.now())
                                        .tongTienHang(payload.getTongTien())
                                        .tienGiamGia(tienGiamGia)
                                        .phiShip(BigDecimal.ZERO)
                                        .chuongTrinhKhuyenMai(ctkm)
                                        .trangThai("don_hang_cho")
                                        .trangThaiThanhToan("chua_thanh_toan")
                                        .updatedAt(LocalDateTime.now())
                                        .build();
                        
                        donHang = donHangRepository.save(donHang);
                } else {
                        // Cập nhật đơn hiện tại
                        donHang = donHangRepository.findById(donHangId)
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng id=" + donHangId));
                        
                        if (!"cho_xac_nhan".equals(donHang.getTrangThai()) && !"don_hang_cho".equals(donHang.getTrangThai())) {
                                throw new IllegalStateException("Đơn hàng không ở trạng thái hợp lệ để lưu chờ");
                        }

                        if (payload.getKhachHangId() == null) {
                            throw new IllegalArgumentException("Vui lòng chọn hoặc thêm khách hàng để tạo đơn hàng. (Bắt buộc phục vụ bảo hành)");
                        }
                        KhachHang khachHang = khachHangRepository.findById(payload.getKhachHangId())
                                        .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Khách hàng"));
                        
                        BigDecimal tienGiamGia = BigDecimal.ZERO;
                        ChuongTrinhKhuyenMai ctkm = null;

                        if (payload.getCtkmId() != null) {
                                KhuyenMaiResult kmResult = khuyenMaiService.apDungCtkmTheoId(payload.getCtkmId(), payload.getTongTien());
                                tienGiamGia = kmResult.getTienGiam();
                                ctkm = ctkmRepository.findById(payload.getCtkmId()).orElse(null);
                        } else {
                                KhuyenMaiResult kmResult = khuyenMaiService.tinhKhuyenMaiChoDonHang(payload.getTongTien());
                                tienGiamGia = kmResult.getTienGiam();
                                if (kmResult.getCtkmId() != null) {
                                        ctkm = ctkmRepository.findById(kmResult.getCtkmId()).orElse(null);
                                }
                        }

                        donHang.setKhachHang(khachHang);
                        donHang.setTongTienHang(payload.getTongTien());
                        donHang.setTienGiamGia(tienGiamGia);
                        donHang.setChuongTrinhKhuyenMai(ctkm);
                        donHang.setTrangThai("don_hang_cho");
                        donHang.setTrangThaiThanhToan("chua_thanh_toan");
                        donHang.setUpdatedAt(LocalDateTime.now());
                        
                        // Clear chi tiết cũ (upsert mới theo payload)
                        chiTietDonHangRepository.deleteAll(donHang.getChiTietDonHangs());
                        donHang.getChiTietDonHangs().clear();
                        
                        // Giải phóng IMEI cũ nếu có (hoàn trả về trong_kho)
                        List<MayDienThoai> oldImeis = mayDienThoaiRepository.findByDonHangId(donHangId);
                        for (MayDienThoai may : oldImeis) {
                                may.setTinhTrang("trong_kho");
                                may.setNguoiGiu(null);
                                may.setThoiGianGiu(null);
                                may.setDonHang(null);
                                mayDienThoaiRepository.save(may);
                        }
                        
                        donHang = donHangRepository.save(donHang);
                }

                // Lưu chi tiết và khóa IMEI
                if (payload.getChiTiets() != null) {
                        for (org.example.primemobile.dto.request.PosThanhToanRequest.ChiTietPosRequest ct : payload.getChiTiets()) {
                                BienTheSanPham bienThe = bienTheSanPhamService.getBienTheSanPham(ct.getBienTheId());
                                ChiTietDonHang chiTiet = ChiTietDonHang.builder()
                                                .donHang(donHang)
                                                .bienTheSanPham(bienThe)
                                                .soLuong(ct.getSoLuong())
                                                .donGiaBan(ct.getDonGia() != null ? ct.getDonGia() : bienThe.getGiaBan())
                                                .build();
                                chiTietDonHangRepository.save(chiTiet);
                                donHang.getChiTietDonHangs().add(chiTiet);

                                // Khóa IMEI (chuyển sang da_ban ngay lập tức)
                                if (ct.getImeis() != null) {
                                        for (String imei : ct.getImeis()) {
                                                MayDienThoai may = mayDienThoaiRepository.findByImei1(imei.trim())
                                                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy IMEI: " + imei));
                                                boolean hopLe = "trong_kho".equals(may.getTinhTrang()) || 
                                                                ("dang_giu".equals(may.getTinhTrang()) && may.getNguoiGiu() != null && may.getNguoiGiu().getId().equals(nhanVienId));
                                                if (!hopLe) {
                                                        throw new IllegalArgumentException("IMEI [" + imei + "] không ở trạng thái hợp lệ để bán (có thể đang bị người khác giữ)");
                                                }
                                                may.setTinhTrang("dang_giu");
                                                // (Vẫn giữ người giữ là nhân viên hiện tại nhưng có donHang != null)
                                                may.setDonHang(donHang);
                                                mayDienThoaiRepository.save(may);
                                        }
                                }
                        }
                }

                return donHang;
        }

        @Override
        @Transactional(readOnly = true)
        public List<DonHang> layDanhSachDonHangCho() {
                // Lấy đơn của kênh bán tại quầy và có trạng thái don_hang_cho, sắp xếp mới nhất
                return donHangRepository.findByKenhBanAndTrangThaiOrderByNgayDatDesc("tai_quay", "don_hang_cho");
        }

        @Override
        @Transactional
        public DonHang huyDonHangCho(Integer donHangId, String lyDoHuy) {
                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng id=" + donHangId));
                
                if (!"don_hang_cho".equals(donHang.getTrangThai())) {
                        throw new IllegalStateException("Đơn hàng không ở trạng thái chờ");
                }
                
                // Hoàn trả IMEI về trong_kho
                List<MayDienThoai> imeiList = mayDienThoaiRepository.findByDonHangId(donHangId);
                for (MayDienThoai may : imeiList) {
                        may.setTinhTrang("trong_kho");
                        may.setNguoiGiu(null);
                        may.setThoiGianGiu(null);
                        may.setDonHang(null); // Giải phóng khỏi đơn
                        mayDienThoaiRepository.save(may);
                }
                
                // Cập nhật trạng thái đơn
                donHang.setTrangThai("da_huy");
                donHang.setTrangThaiThanhToan("chua_thanh_toan");
                donHang.setGhiChu(lyDoHuy);
                donHang.setUpdatedAt(LocalDateTime.now());
                
                return donHangRepository.save(donHang);
        }

        @Override
        @Transactional
        public DonHang tiepTucDonHangCho(Integer donHangId) {
                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy đơn hàng id=" + donHangId));
                
                if (!"don_hang_cho".equals(donHang.getTrangThai())) {
                        throw new IllegalStateException("Đơn hàng không ở trạng thái chờ");
                }
                
                // Kiểm tra lại CTKM (còn hiệu lực không)
                if (donHang.getChuongTrinhKhuyenMai() != null) {
                        ChuongTrinhKhuyenMai ctkm = donHang.getChuongTrinhKhuyenMai();
                        LocalDateTime now = LocalDateTime.now();
                        if (!"dang_dien_ra".equals(ctkm.getTrangThai()) ||
                                now.isBefore(ctkm.getNgayBatDau()) ||
                                now.isAfter(ctkm.getNgayKetThuc())) {
                                // CTKM không còn hiệu lực → bỏ khuyến mãi
                                donHang.setChuongTrinhKhuyenMai(null);
                                donHang.setTienGiamGia(BigDecimal.ZERO);
                        }
                }
                
                // Xoá việc đổi trạng thái sang cho_xac_nhan. Đơn hàng tiếp tục vẫn là đơn hàng chờ cho đến khi thanh toán xong.
                return donHang;
        }

        // =======================================================================
        // CART RESERVATION (MODULE POS)
        // =======================================================================

        @Override
        @Transactional
        public void giuImei(String imei, Integer nhanVienId) {
                MayDienThoai may = mayDienThoaiRepository.findByImei1(imei.trim())
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy IMEI: " + imei));
                
                if ("dang_giu".equals(may.getTinhTrang()) && may.getNguoiGiu() != null && may.getNguoiGiu().getId().equals(nhanVienId)) {
                        // Đã giữ bởi chính nhân viên này rồi
                        return;
                }

                if (!"trong_kho".equals(may.getTinhTrang())) {
                        throw new IllegalStateException("IMEI đang không ở trạng thái trong_kho. Trạng thái hiện tại: " + may.getTinhTrang());
                }

                NguoiDung nhanVien = nguoiDungRepository.findById(nhanVienId)
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhân viên"));

                may.setTinhTrang("dang_giu");
                may.setNguoiGiu(nhanVien);
                may.setThoiGianGiu(LocalDateTime.now());
                mayDienThoaiRepository.save(may);
        }

        @Override
        @Transactional
        public void nhaImei(String imei, Integer nhanVienId) {
                MayDienThoai may = mayDienThoaiRepository.findByImei1(imei.trim())
                                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy IMEI: " + imei));
                
                // Chỉ nhả nếu đang giữ và đúng nhân viên giữ (tránh nhả nhầm của người khác)
                if ("dang_giu".equals(may.getTinhTrang()) && may.getNguoiGiu() != null && may.getNguoiGiu().getId().equals(nhanVienId)) {
                        may.setTinhTrang("trong_kho");
                        may.setNguoiGiu(null);
                        may.setThoiGianGiu(null);
                        mayDienThoaiRepository.save(may);
                }
        }

        @Override
        @Transactional
        public void nhaTatCaImeiCuaNhanVien(Integer nhanVienId) {
                List<MayDienThoai> dsMay = mayDienThoaiRepository.findByNguoiGiuIdAndDonHangIsNull(nhanVienId);
                for (MayDienThoai may : dsMay) {
                        may.setTinhTrang("trong_kho");
                        may.setNguoiGiu(null);
                        may.setThoiGianGiu(null);
                        mayDienThoaiRepository.save(may);
                }
        }
}