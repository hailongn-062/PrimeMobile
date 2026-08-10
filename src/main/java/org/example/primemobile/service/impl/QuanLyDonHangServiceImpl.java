package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IQuanLyDonHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Triển khai phân hệ Quản lý Đơn Hàng dành cho Nhân viên / Admin.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md):</h2>
 * <ul>
 * <li><b>§2.2.5 – Trừ kho khi xác nhận:</b> kho_tong bị trừ đúng tại bước
 * {@code xacNhanDonHang},
 * không phải lúc khách đặt hàng.</li>
 * <li><b>§3.1 – Stock Rule:</b> Sau khi trừ, tồn kho {@code kho_tong}
 * KHÔNG ĐƯỢC âm. Không áp dụng mức dự trữ tối thiểu (Safety Stock).
 * Pessimistic Write Lock được dùng tại bước EXECUTE để chống race condition.</li>
 * <li><b>§2.2.7 – Hoàn kho khi hủy:</b> Nếu đơn đang ở {@code da_xac_nhan} hoặc
 * {@code dang_giao}
 * (kho đã bị trừ), BẮT BUỘC cộng hoàn lại vào kho_tong khi hủy.</li>
 * <li><b>§7.4 – Tạm hoãn tích điểm:</b> Không viết code cộng điểm / cộng
 * tong_chi_tieu
 * khi đơn chuyển sang {@code da_hoan_thanh}.</li>
 * </ul>
 *
 * <h2>Chiến lược Transaction:</h2>
 * Mọi hàm thay đổi dữ liệu đều annotated {@code @Transactional}.
 * Fail-Fast pattern: validate toàn bộ TRƯỚC khi thực thi bất kỳ lệnh ghi nào.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuanLyDonHangServiceImpl implements IQuanLyDonHangService {

        // ───────────────────────────────────────────────────────────────────────
        // HẰNG SỐ NGHIỆP VỤ
        // ───────────────────────────────────────────────────────────────────────

        /** Số lượng tồn kho tối thiểu được phép bán = 0 (không áp dụng Safety Stock — §3.1).
         * Bị chặn khi tồn kho sau khi trừ < 0 (tức âm kho). */

        /** Các trạng thái đơn hàng mà kho đã bị trừ — cần hoàn kho khi hủy. */
        private static final Set<String> TRANG_THAI_DA_TRU_KHO = Set.of("da_xac_nhan", "dang_giao");

        // ───────────────────────────────────────────────────────────────────────
        // DEPENDENCIES
        // ───────────────────────────────────────────────────────────────────────

        private final DonHangRepository donHangRepository;
        private final ChiTietDonHangRepository chiTietDonHangRepository;
        private final KhoRepository khoRepository;
        private final TonKhoRepository tonKhoRepository;
        private final NguoiDungRepository nguoiDungRepository;
        private final MayDienThoaiRepository mayDienThoaiRepository; // ✅ Thêm để kiểm tra IMEI

        // =========================================================================
        // 1. XEM DANH SÁCH & CHI TIẾT
        // =========================================================================

        /**
         * {@inheritDoc}
         * <p>
         * Read-only transaction để tối ưu hiệu năng.
         */
        @Override
        @Transactional(readOnly = true)
        public Page<DonHang> layDanhSachDonHang(String trangThai, String maDonHang,
                        String soDienThoai, Pageable pageable) {
                // Chuẩn hoá: chuỗi rỗng → null để query JPQL xử lý đúng điều kiện IS NULL
                String tt = (trangThai != null && !trangThai.isBlank()) ? trangThai.trim() : null;
                String ma = (maDonHang != null && !maDonHang.isBlank()) ? maDonHang.trim() : null;
                String sdt = (soDienThoai != null && !soDienThoai.isBlank()) ? soDienThoai.trim() : null;

                log.debug("[QuanLyDonHang] Tìm kiếm — trangThai={}, maDonHang={}, soDienThoai={}, page={}",
                                tt, ma, sdt, pageable.getPageNumber());

                return donHangRepository.timKiemDonHang(tt, ma, sdt, pageable);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @Transactional(readOnly = true)
        public DonHang layChiTietDonHang(Integer donHangId) {
                return donHangRepository.findByIdWithDetails(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));
        }

        // =========================================================================
        // 2. XÁC NHẬN ĐƠN HÀNG (BƯỚC QUAN TRỌNG NHẤT — TRỪ KHO)
        // =========================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Luồng chi tiết:</h3>
         * <ol>
         * <li>Validate đơn hàng ở trạng thái {@code "cho_xac_nhan"}.</li>
         * <li>Load ChiTietDonHang với FETCH JOIN (tránh N+1).</li>
         * <li><b>Fail-Fast — PRE-VALIDATE toàn bộ:</b>
         * Với mỗi SKU: kiểm tra tồn kho kho_tong &ge; (soLuong + Safety Stock).
         * Ném lỗi ngay nếu bất kỳ SKU nào vi phạm — chưa trừ dòng nào.</li>
         * <li><b>EXECUTE:</b> Tất cả pass → trừ kho từng dòng.</li>
         * <li>Cập nhật trạng thái đơn → {@code "da_xac_nhan"}, ghi nhân viên xử
         * lý.</li>
         * </ol>
         */
        @Override
        @Transactional
        public DonHang xacNhanDonHang(Integer donHangId, Integer nhanVienId) {

                log.info("[QuanLyDonHang] ▶ Xác nhận đơn — donHangId={}, nhanVienId={}",
                                donHangId, nhanVienId);

                // --- Validate đơn hàng ---
                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                if (!"cho_xac_nhan".equals(donHang.getTrangThai())) {
                        throw new IllegalArgumentException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái '%s', không thể xác nhận. " +
                                                        "Chỉ được xác nhận khi trạng thái là 'cho_xac_nhan'.",
                                        donHang.getMaDonHang(), donHang.getTrangThai()));
                }

                if ("dang_chuyen_huong".equals(donHang.getTrangThaiThanhToan()) || "that_bai".equals(donHang.getTrangThaiThanhToan())) {
                        throw new IllegalStateException("Không thể xác nhận đơn hàng đang thanh toán VNPay chưa thành công hoặc thất bại.");
                }

                // --- Lấy danh sách chi tiết đơn (eager-load) ---
                List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

                if (danhSachChiTiet.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Đơn hàng [" + donHang.getMaDonHang() + "] không có sản phẩm nào. " +
                                                        "Không thể xác nhận đơn rỗng.");
                }

                // --- Lấy kho t?ng ---
                Kho khoTong = layKhoTongHoacNemLoi();

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC FAIL-FAST: PRE-VALIDATE TOÀN BỘ trước khi trừ bất kỳ dòng nào
                // ═══════════════════════════════════════════════════════════════════
                for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                        BienTheSanPham bienThe = chiTiet.getBienTheSanPham();

                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPham(khoTong, bienThe)
                                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                        "Không tìm thấy tồn kho cho sản phẩm [%s] tại kho t?ng. " +
                                                                        "Vui lòng kiểm tra dữ liệu kho.",
                                                        bienThe.getMaSku())));

                        int tonKhoSauKhiTru = tonKho.getSoLuong() - chiTiet.getSoLuong();

                        // ⚠️ QUY TẮc TỒN KHO §3.1: Sau khi trừ KHÔNG được âm
                        if (tonKhoSauKhiTru < 0) {
                                throw new IllegalArgumentException(String.format(
                                                "Không đủ tồn kho khi xác nhận đơn [%s]. " +
                                                                "Sản phẩm [%s] tại kho tổng: Tồn kho = %d, Bán = %d.",
                                                donHang.getMaDonHang(), bienThe.getMaSku(),
                                                tonKho.getSoLuong(), chiTiet.getSoLuong()));
                        }
                }

                // ═══════════════════════════════════════════════════════════════════
                // EXECUTE: Dùng Pessimistic Lock khi trừ kho thực tế (§3.1 chống race condition)
                // ═══════════════════════════════════════════════════════════════════
                LocalDateTime now = LocalDateTime.now();
                for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPhamForUpdate(khoTong, chiTiet.getBienTheSanPham())
                                        .orElseThrow(); // Đã validate ở trên, không thể null

                        int soLuongTruoc = tonKho.getSoLuong();
                        int soLuongMoi = soLuongTruoc - chiTiet.getSoLuong();

                        // ⚠️ GUARD chống âm kho — lớp bảo vệ cuối cùng sau Pessimistic Lock
                        if (soLuongMoi < 0) {
                                throw new IllegalStateException(String.format(
                                                "[QuanLyDonHang] Tồn kho không được âm sau khi trừ. "
                                                + "SKU [%s]: tồn=%d, bán=%d, mới=%d.",
                                                chiTiet.getBienTheSanPham().getMaSku(),
                                                soLuongTruoc, chiTiet.getSoLuong(), soLuongMoi));
                        }

                        tonKho.setSoLuong(soLuongMoi);
                        tonKho.setUpdatedAt(now);
                        tonKhoRepository.save(tonKho);

                        log.debug("[QuanLyDonHang] Trừ kho — sku=[{}]: {} - {} = {}",
                                        chiTiet.getBienTheSanPham().getMaSku(),
                                        soLuongTruoc, chiTiet.getSoLuong(), tonKho.getSoLuong());
                }

                // --- Cập nhật trạng thái đơn hàng ---
                donHang.setTrangThai("da_xac_nhan");
                donHang.setUpdatedAt(now);

                // Ghi nhân viên xử lý (nếu có)
                if (nhanVienId != null) {
                        nguoiDungRepository.findById(nhanVienId).ifPresent(donHang::setNguoiXuLy);
                }

                donHangRepository.save(donHang);

                log.info("[QuanLyDonHang] ✅ Xác nhận thành công — maDonHang={}, trangThai=da_xac_nhan, đã trừ {} SKU",
                                donHang.getMaDonHang(), danhSachChiTiet.size());
                return donHang;
        }

        // =========================================================================
        // 2b. XÁC NHẬN ĐƠN HÀNG VỚI IMEI
        // =========================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Luồng chi tiết:</h3>
         * <ol>
         * <li>Validate đơn hàng ở trạng thái {@code "cho_xac_nhan"}.</li>
         * <li>Load ChiTietDonHang với FETCH JOIN (tránh N+1).</li>
         * <li>Lấy kho t?ng.</li>
         * <li><b>Fail-Fast — PRE-VALIDATE toàn bộ:</b>
         * <ul>
         * <li>Với mỗi {@link ImeiSelection}: kiểm tra chi tiết đơn tồn tại, số lượng
         * IMEI khớp.</li>
         * <li>Với mỗi IMEI: kiểm tra tồn tại, tinhTrang='trong_kho', thuộc đúng biến
         * thể.</li>
         * <li>Kiểm tra tồn kho kho_tong đủ (Safety Stock).</li>
         * </ul>
         * Ném lỗi ngay nếu bất kỳ vi phạm nào — chưa thay đổi dữ liệu.
         * </li>
         * <li><b>EXECUTE:</b>
         * <ul>
         * <li>Cập nhật IMEI: tinhTrang='da_ban', gán donHang.</li>
         * <li>Trừ tồn kho từng dòng.</li>
         * <li>Cập nhật trạng thái đơn → {@code "da_xac_nhan"}, ghi nhân viên xử
         * lý.</li>
         * </ul>
         * </li>
         * </ol>
         */
        @Override
        @Transactional
        public DonHang xacNhanDonHangVoiImei(Integer donHangId, Integer nhanVienId,
                        List<ImeiSelection> imeiSelections) {

                log.info("[QuanLyDonHang] ▶ Xác nhận đơn với IMEI — donHangId={}, nhanVienId={}, {} selection",
                                donHangId, nhanVienId, imeiSelections == null ? 0 : imeiSelections.size());

                // --- Validate đơn hàng ---
                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                if (!"cho_xac_nhan".equals(donHang.getTrangThai())) {
                        throw new IllegalArgumentException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái '%s', không thể xác nhận. " +
                                                        "Chỉ được xác nhận khi trạng thái là 'cho_xac_nhan'.",
                                        donHang.getMaDonHang(), donHang.getTrangThai()));
                }

                if ("dang_chuyen_huong".equals(donHang.getTrangThaiThanhToan()) || "that_bai".equals(donHang.getTrangThaiThanhToan())) {
                        throw new IllegalStateException("Không thể xác nhận đơn hàng đang thanh toán VNPay chưa thành công hoặc thất bại.");
                }

                // --- Lấy danh sách chi tiết đơn (eager-load) ---
                List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

                if (danhSachChiTiet.isEmpty()) {
                        throw new IllegalArgumentException(
                                        "Đơn hàng [" + donHang.getMaDonHang() + "] không có sản phẩm nào. " +
                                                        "Không thể xác nhận đơn rỗng.");
                }

                // --- Lấy kho t?ng ---
                Kho khoTong = layKhoTongHoacNemLoi();

                // ═══════════════════════════════════════════════════════════════════
                // BƯỚC FAIL-FAST: PRE-VALIDATE TOÀN BỘ trước khi thay đổi dữ liệu
                // ═══════════════════════════════════════════════════════════════════

                // Map chiTietId -> ChiTietDonHang để kiểm tra nhanh
                java.util.Map<Integer, ChiTietDonHang> chiTietMap = new java.util.HashMap<>();
                for (ChiTietDonHang ct : danhSachChiTiet) {
                        chiTietMap.put(ct.getId(), ct);
                }

                // Danh sách các IMEI cần cập nhật (sau khi validate) — lưu để execute
                java.util.List<MayDienThoai> imeiToUpdate = new java.util.ArrayList<>();

                for (ImeiSelection sel : imeiSelections) {
                        ChiTietDonHang chiTiet = chiTietMap.get(sel.getChiTietDonHangId());
                        if (chiTiet == null) {
                                throw new IllegalArgumentException(
                                                "Chi tiết đơn hàng ID " + sel.getChiTietDonHangId()
                                                                + " không thuộc đơn hàng này.");
                        }

                        List<String> imeiList = sel.getImeiList();
                        if (imeiList == null || imeiList.isEmpty()) {
                                throw new IllegalArgumentException(
                                                "Chưa có IMEI cho sản phẩm: " + chiTiet.getBienTheSanPham().getMaSku());
                        }

                        if (imeiList.size() != chiTiet.getSoLuong()) {
                                throw new IllegalArgumentException(String.format(
                                                "Số lượng IMEI (%d) không khớp với số lượng sản phẩm (%d) cho SKU [%s].",
                                                imeiList.size(), chiTiet.getSoLuong(),
                                                chiTiet.getBienTheSanPham().getMaSku()));
                        }

                        // Kiểm tra từng IMEI
                        for (String imei : imeiList) {
                                String imeiClean = imei.trim();
                                MayDienThoai may = mayDienThoaiRepository.findByImei1(imeiClean)
                                                .orElseThrow(() -> new EntityNotFoundException(
                                                                "Không tìm thấy IMEI: " + imeiClean));

                                // Kiểm tra IMEI thuộc đúng biến thể
                                if (!may.getBienTheSanPham().getId().equals(chiTiet.getBienTheSanPham().getId())) {
                                        throw new IllegalArgumentException(String.format(
                                                        "IMEI [%s] không thuộc biến thể [%s].",
                                                        imeiClean, chiTiet.getBienTheSanPham().getMaSku()));
                                }

                                // Kiểm tra IMEI đang trong kho
                                if (!"trong_kho".equals(may.getTinhTrang())) {
                                        throw new IllegalArgumentException(String.format(
                                                        "IMEI [%s] không ở trạng thái 'trong_kho' (hiện tại: '%s').",
                                                        imeiClean, may.getTinhTrang()));
                                }

                                // ⚠️ Lưu ý: Đã xóa kiểm tra may.getKho() vì field kho đã bị xóa khỏi entity
                                // MayDienThoai.
                                // Hệ thống hiện chỉ có một kho duy nhất, nên không cần kiểm tra kho của IMEI.

                                // Lưu tạm để execute sau
                                imeiToUpdate.add(may);
                        }

                        // Kiểm tra tồn kho t?ng (Safety Stock)
                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPham(khoTong, chiTiet.getBienTheSanPham())
                                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                        "Không tìm thấy tồn kho cho sản phẩm [%s] tại kho t?ng.",
                                                        chiTiet.getBienTheSanPham().getMaSku())));

                        int tonKhoSauKhiTru = tonKho.getSoLuong() - chiTiet.getSoLuong();
                        if (tonKhoSauKhiTru < 0) {
                                throw new IllegalArgumentException(String.format(
                                                "Không đủ tồn kho khi xác nhận đơn [%s] với IMEI. " +
                                                                "Sản phẩm [%s] tại kho tổng: Tồn kho = %d, Bán = %d.",
                                                donHang.getMaDonHang(), chiTiet.getBienTheSanPham().getMaSku(),
                                                tonKho.getSoLuong(), chiTiet.getSoLuong()));
                        }
                }

                // ═══════════════════════════════════════════════════════════════════
                // EXECUTE: Tất cả đã pass → Cập nhật IMEI và trừ kho
                // ═══════════════════════════════════════════════════════════════════

                LocalDateTime now = LocalDateTime.now();

                // 1. Cập nhật IMEI
                for (MayDienThoai may : imeiToUpdate) {
                        may.setTinhTrang("da_ban");
                        may.setDonHang(donHang);
                        mayDienThoaiRepository.save(may);
                }
                log.info("[QuanLyDonHang] Đã cập nhật {} IMEI thành da_ban.", imeiToUpdate.size());

                // 2. Trừ kho tổng — dùng Pessimistic Lock (§3.1 chống race condition)
                for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPhamForUpdate(khoTong, chiTiet.getBienTheSanPham())
                                        .orElseThrow(); // Đã validate ở trên

                        int soLuongMoi2 = tonKho.getSoLuong() - chiTiet.getSoLuong();

                        // ⚠️ GUARD chống âm kho — lớp bảo vệ cuối cùng sau Pessimistic Lock
                        if (soLuongMoi2 < 0) {
                                throw new IllegalStateException(String.format(
                                                "[QuanLyDonHang] Tồn kho không được âm sau khi trừ (IMEI flow). "
                                                + "SKU [%s]: tồn=%d, bán=%d, mới=%d.",
                                                chiTiet.getBienTheSanPham().getMaSku(),
                                                tonKho.getSoLuong(), chiTiet.getSoLuong(), soLuongMoi2));
                        }

                        tonKho.setSoLuong(soLuongMoi2);
                        tonKho.setUpdatedAt(now);
                        tonKhoRepository.save(tonKho);

                        log.debug("[QuanLyDonHang] Trừ kho — sku=[{}]: còn lại {}",
                                        chiTiet.getBienTheSanPham().getMaSku(), tonKho.getSoLuong());
                }

                // 3. Cập nhật trạng thái đơn hàng
                donHang.setTrangThai("da_xac_nhan");
                donHang.setUpdatedAt(now);

                if (nhanVienId != null) {
                        nguoiDungRepository.findById(nhanVienId).ifPresent(donHang::setNguoiXuLy);
                }

                donHangRepository.save(donHang);

                log.info("[QuanLyDonHang] ✅ Xác nhận với IMEI thành công — maDonHang={}, trangThai=da_xac_nhan",
                                donHang.getMaDonHang());
                return donHang;
        }

        // =========================================================================
        // 3. CẬP NHẬT LỘ TRÌNH GIAO HÀNG
        // =========================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Luồng trạng thái hợp lệ:</h3>
         * 
         * <pre>
         *   da_xac_nhan → dang_giao   (nhân viên đã giao cho đơn vị vận chuyển)
         *   dang_giao   → da_hoan_thanh     (đơn vị vận chuyển xác nhận giao thành công)
         * </pre>
         *
         * ⚠️ Tạm hoãn (§7.4): Không cộng điểm thưởng / cập nhật tong_chi_tieu khi
         * da_hoan_thanh.
         */
        @Override
        @Transactional
        public DonHang capNhatTrangThai(Integer donHangId, String trangThaiMoi) {

                log.info("[QuanLyDonHang] ▶ Cập nhật trạng thái — donHangId={}, trangThaiMoi={}",
                                donHangId, trangThaiMoi);

                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                String trangThaiHienTai = donHang.getTrangThai();

                // Validate luồng chuyển trạng thái
                validLuongTrangThai(donHang.getMaDonHang(), trangThaiHienTai, trangThaiMoi);

                LocalDateTime now = LocalDateTime.now();
                donHang.setTrangThai(trangThaiMoi);
                donHang.setUpdatedAt(now);

                // Ghi ngày giao thực tế khi hoàn tất giao hàng
                if ("da_hoan_thanh".equals(trangThaiMoi)) {
                        donHang.setNgayGiaoThucTe(now);

                        // Tự động xác nhận thanh toán cho đơn COD
                        if ("chua_thanh_toan".equals(donHang.getTrangThaiThanhToan())) {
                                donHang.setTrangThaiThanhToan("da_thanh_toan");
                                log.info("[QuanLyDonHang] Đã tự động chuyển trạng thái thanh toán sang 'da_thanh_toan' cho đơn [{}].", donHang.getMaDonHang());
                        }

                        donHangRepository.save(donHang);
                        return donHang;
                }

                donHangRepository.save(donHang);

                log.info("[QuanLyDonHang] ✅ Cập nhật trạng thái — maDonHang={}: {} → {}",
                                donHang.getMaDonHang(), trangThaiHienTai, trangThaiMoi);
                return donHang;
        }

        // =========================================================================
        // 4. HỦY ĐƠN HÀNG (KÈM HOÀN KHO NẾU CẦN)
        // =========================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Logic hoàn kho (system_rules.md §2.2.7):</h3>
         * <ul>
         * <li>{@code cho_xac_nhan}: Kho chưa bị trừ → chỉ đổi trạng thái, KHÔNG thao
         * tác kho.</li>
         * <li>{@code da_xac_nhan} | {@code dang_giao}: Kho ĐÃ bị trừ trước đó
         * → BẮT BUỘC cộng hoàn lại số lượng vào {@code kho_tong}.</li>
         * <li>{@code da_hoan_thanh} | {@code da_huy}: Không được hủy → ném lỗi.</li>
         * </ul>
         */
        @Override
        @Transactional
        public DonHang huyDonHang(Integer donHangId, String lyDoHuy) {

                log.info("[QuanLyDonHang] ▶ Hủy đơn — donHangId={}", donHangId);

                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                String trangThaiHienTai = donHang.getTrangThai();

                // Chỉ cho phép hủy khi đang ở trạng thái chờ xác nhận
                if (!"cho_xac_nhan".equals(trangThaiHienTai)) {
                        throw new IllegalArgumentException(String.format(
                                        "Chỉ được hủy đơn ở trạng thái chờ xác nhận. Đơn hàng [%s] đang ở trạng thái %s.", donHang.getMaDonHang(), trangThaiHienTai));
                }

                LocalDateTime now = LocalDateTime.now();

                // ═══════════════════════════════════════════════════════════════════
                // HOÀN KHO TỒN KHO — chỉ thực hiện khi kho đã bị trừ trước đó (§2.2.7)
                // ═══════════════════════════════════════════════════════════════════
                if (TRANG_THAI_DA_TRU_KHO.contains(trangThaiHienTai)) {
                        log.info("[QuanLyDonHang] 🔄 Hoàn kho — đơn [{}] đang ở '{}', cần cộng hoàn kho_tong.",
                                        donHang.getMaDonHang(), trangThaiHienTai);

                        List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository
                                        .findByDonHangIdWithDetails(donHangId);

                        Kho khoTong = layKhoTongHoacNemLoi();

                        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                                BienTheSanPham bienThe = chiTiet.getBienTheSanPham();

                                TonKho tonKho = tonKhoRepository
                                                .findByKhoAndBienTheSanPham(khoTong, bienThe)
                                                .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                                "Không tìm thấy tồn kho cho [%s] tại kho t?ng khi hoàn hàng.",
                                                                bienThe.getMaSku())));

                                int soLuongTruoc = tonKho.getSoLuong();
                                tonKho.setSoLuong(tonKho.getSoLuong() + chiTiet.getSoLuong());
                                tonKho.setUpdatedAt(now);
                                tonKhoRepository.save(tonKho);

                                log.debug("[QuanLyDonHang] Hoàn kho — sku=[{}]: {} + {} = {}",
                                                bienThe.getMaSku(), soLuongTruoc,
                                                chiTiet.getSoLuong(), tonKho.getSoLuong());
                        }

                        log.info("[QuanLyDonHang] ✅ Hoàn kho xong — {} dòng CTDH được cộng lại vào kho_tong.",
                                        danhSachChiTiet.size());
                } else {
                        // cho_xac_nhan: kho chưa bị trừ → không cần hoàn kho
                        log.info("[QuanLyDonHang] ℹ️ Đơn [{}] ở '{}' — kho chưa bị trừ, bỏ qua bước hoàn kho.",
                                        donHang.getMaDonHang(), trangThaiHienTai);
                }

                boolean canHoanTien = "da_thanh_toan".equals(donHang.getTrangThaiThanhToan());

                if (canHoanTien) {
                        // --- TRƯỜNG HỢP 1: Đã thanh toán -> chờ hoàn tiền ---
                        donHang.setTrangThai("cho_hoan_tien");
                        donHang.setUpdatedAt(now);
                        String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
                        donHang.setGhiChu(ghiChuCu + "[YÊU CẦU HOÀN TIỀN " + now.toLocalDate() + "]: " + lyDoHuy);
                        log.info("[QuanLyDonHang] ℹ️ Đơn [{}] đã thanh toán -> chuyển sang chờ hoàn tiền.", donHang.getMaDonHang());
                } else {
                        // --- TRƯỜNG HỢP 2: Chưa thanh toán -> hủy luôn ---
                        donHang.setTrangThai("da_huy");
                        donHang.setTrangThaiThanhToan("chua_thanh_toan");
                        donHang.setUpdatedAt(now);

                        String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
                        donHang.setGhiChu(ghiChuCu + "[HỦY ĐƠN " + now.toLocalDate() + "]: " + lyDoHuy);

                        // Nhả IMEI ngay lập tức (nếu có)
                        List<MayDienThoai> imeis = mayDienThoaiRepository.findByDonHangIdAndTinhTrang(donHangId, "da_ban");
                        if (!imeis.isEmpty()) {
                                for (MayDienThoai may : imeis) {
                                        may.setTinhTrang("trong_kho");
                                        may.setDonHang(null);
                                        mayDienThoaiRepository.save(may);
                                }
                                log.info("[QuanLyDonHang] ✅ Đã nhả {} IMEI về kho cho đơn {}.", imeis.size(), donHang.getMaDonHang());
                        }

                        log.info("[QuanLyDonHang] ✅ Hủy đơn thành công — maDonHang={}", donHang.getMaDonHang());
                }

                return donHangRepository.save(donHang);
        }

        // =========================================================================
        // 4b. XÁC NHẬN HOÀN TIỀN
        // =========================================================================

        /**
         * {@inheritDoc}
         */
        @Override
        @Transactional
        public DonHang xacNhanHoanTien(Integer donHangId, Integer idNhanVien) {
                log.info("[QuanLyDonHang] ▶ Xác nhận hoàn tiền — donHangId={}, nhanVienId={}", donHangId, idNhanVien);

                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                if (!"cho_hoan_tien".equals(donHang.getTrangThai())) {
                        throw new IllegalArgumentException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái '%s'. Chỉ có thể xác nhận hoàn tiền khi ở trạng thái 'cho_hoan_tien'.",
                                        donHang.getMaDonHang(), donHang.getTrangThai()));
                }

                LocalDateTime now = LocalDateTime.now();

                // 1. Cập nhật trạng thái
                if (donHang.getGhiChu() != null && donHang.getGhiChu().contains("[GIAO THẤT BẠI")) {
                        donHang.setTrangThai("giao_that_bai");
                } else {
                        donHang.setTrangThai("da_huy");
                }
                donHang.setTrangThaiThanhToan("da_hoan_tien");
                donHang.setUpdatedAt(now);

                String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
                donHang.setGhiChu(ghiChuCu + "[XÁC NHẬN HOÀN TIỀN " + now.toLocalDate() + "]: Kế toán/Admin đã hoàn tiền.");

                if (idNhanVien != null) {
                        nguoiDungRepository.findById(idNhanVien).ifPresent(donHang::setNguoiXuLy);
                }

                // 2. Nhả IMEI về kho (nếu có)
                List<MayDienThoai> imeis = mayDienThoaiRepository.findByDonHangIdAndTinhTrang(donHangId, "da_ban");
                if (!imeis.isEmpty()) {
                        for (MayDienThoai may : imeis) {
                                may.setTinhTrang("trong_kho");
                                may.setDonHang(null);
                                mayDienThoaiRepository.save(may);
                        }
                        log.info("[QuanLyDonHang] ✅ Đã nhả {} IMEI về kho sau khi hoàn tiền cho đơn {}.", imeis.size(), donHang.getMaDonHang());
                }

                log.info("[QuanLyDonHang] ✅ Xác nhận hoàn tiền thành công — maDonHang={}", donHang.getMaDonHang());
                return donHangRepository.save(donHang);
        }

        // =========================================================================
        // 5. XÁC NHẬN THANH TOÁN CHO ĐƠN HÀNG COD
        // =========================================================================

        /**
         * {@inheritDoc}
         *
         * <h3>Luồng chi tiết:</h3>
         * <ol>
         * <li>Kiểm tra đơn hàng tồn tại.</li>
         * <li>Kiểm tra trạng thái đơn hàng phải là {@code "da_hoan_thanh"}.</li>
         * <li>Kiểm tra trạng thái thanh toán phải là {@code "chua_thanh_toan"}.</li>
         * <li>Cập nhật trạng thái thanh toán → {@code "da_thanh_toan"}.</li>
         * </ol>
         */
        @Override
        @Transactional
        public DonHang xacNhanThanhToan(Integer donHangId) {

                log.info("[QuanLyDonHang] ▶ Xác nhận thanh toán — donHangId={}", donHangId);

                // Lấy đơn hàng
                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                // Kiểm tra trạng thái đơn hàng
                if (!"da_hoan_thanh".equals(donHang.getTrangThai())) {
                        throw new IllegalArgumentException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái '%s', không thể xác nhận thanh toán. " +
                                                        "Chỉ được xác nhận khi đơn hàng đã giao ('da_hoan_thanh').",
                                        donHang.getMaDonHang(), donHang.getTrangThai()));
                }

                // Kiểm tra trạng thái thanh toán
                if (!"chua_thanh_toan".equals(donHang.getTrangThaiThanhToan())) {
                        throw new IllegalArgumentException(String.format(
                                        "Đơn hàng [%s] đang ở trạng thái thanh toán '%s', không thể xác nhận thanh toán. "
                                                        +
                                                        "Chỉ áp dụng cho đơn chưa thanh toán ('chua_thanh_toan').",
                                        donHang.getMaDonHang(), donHang.getTrangThaiThanhToan()));
                }

                // Cập nhật trạng thái thanh toán
                donHang.setTrangThaiThanhToan("da_thanh_toan");
                donHang.setUpdatedAt(LocalDateTime.now());

                DonHang saved = donHangRepository.save(donHang);

                log.info("[QuanLyDonHang] ✅ Xác nhận thanh toán thành công — maDonHang={}, trangThaiThanhToan=da_thanh_toan",
                                saved.getMaDonHang());

                return saved;
        }

        // =========================================================================
        // 6. LẤY DANH SÁCH IMEI THEO ĐƠN HÀNG (OPTIMIZED VERSION)
        // =========================================================================

        /**
         * {@inheritDoc}
         * <p>
         * Phương thức này lấy tất cả IMEI đã được gán cho đơn hàng (trạng thái
         * 'da_ban').
         * Dùng để hiển thị danh sách IMEI trên trang chi tiết đơn hàng.
         * <p>
         * <b>Optimized:</b> Sử dụng repository method
         * {@link MayDienThoaiRepository#findByDonHangIdAndTinhTrang}
         * để query trực tiếp, tránh load toàn bộ bảng may_dien_thoai.
         *
         * @param donHangId ID đơn hàng cần lấy danh sách IMEI.
         * @return Danh sách {@link MayDienThoai} thuộc đơn hàng đó.
         * @throws EntityNotFoundException nếu đơn hàng không tồn tại.
         */
        @Override
        @Transactional(readOnly = true)
        public List<MayDienThoai> layDanhSachImeiTheoDonHang(Integer donHangId) {
                log.debug("[QuanLyDonHang] Lấy danh sách IMEI theo đơn hàng — donHangId={}", donHangId);

                // Kiểm tra đơn hàng tồn tại
                if (!donHangRepository.existsById(donHangId)) {
                        throw new EntityNotFoundException("Không tìm thấy đơn hàng có ID: " + donHangId);
                }

                // Sử dụng repository method mới để query trực tiếp, tối ưu hiệu năng
                List<MayDienThoai> danhSach = mayDienThoaiRepository
                                .findByDonHangIdAndTinhTrang(donHangId, "da_ban");

                log.debug("[QuanLyDonHang] Tìm thấy {} IMEI cho đơn hàng {}", danhSach.size(), donHangId);
                return danhSach;
        }

        // =========================================================================
        // PRIVATE HELPER METHODS
        // =========================================================================

        /**
         * Lấy kho duy nhất trong hệ thống (ID = 1).
         * Ném lỗi rõ ràng nếu chưa cấu hình.
         */
        private Kho layKhoTongHoacNemLoi() {
                return khoRepository.findById(1)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Không tìm thấy kho ID=1 trong hệ thống. " +
                                                                "Vui lòng kiểm tra dữ liệu bảng kho."));
        }

        @Override
        @Transactional
        public DonHang giaoHangThatBai(Integer donHangId, String lyDo) {
                log.info("[QuanLyDonHang] ▶ Giao hàng thất bại — donHangId={}", donHangId);

                DonHang donHang = donHangRepository.findById(donHangId)
                                .orElseThrow(() -> new EntityNotFoundException(
                                                "Không tìm thấy đơn hàng có ID: " + donHangId));

                String trangThaiHienTai = donHang.getTrangThai();

                if (!"dang_giao".equals(trangThaiHienTai)) {
                        throw new IllegalArgumentException(String.format(
                                        "Chỉ được báo giao thất bại khi đơn hàng đang giao. Đơn hàng [%s] đang ở trạng thái %s.", 
                                        donHang.getMaDonHang(), trangThaiHienTai));
                }

                LocalDateTime now = LocalDateTime.now();

                // 1. Hoàn kho tồn kho (vì đơn đang giao nên chắc chắn kho đã bị trừ)
                List<ChiTietDonHang> danhSachChiTiet = chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);
                Kho khoTong = layKhoTongHoacNemLoi();

                for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                        BienTheSanPham bienThe = chiTiet.getBienTheSanPham();
                        TonKho tonKho = tonKhoRepository
                                        .findByKhoAndBienTheSanPham(khoTong, bienThe)
                                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                                        "Không tìm thấy tồn kho cho [%s] tại kho tổng khi hoàn hàng.",
                                                        bienThe.getMaSku())));

                        tonKho.setSoLuong(tonKho.getSoLuong() + chiTiet.getSoLuong());
                        tonKho.setUpdatedAt(now);
                        tonKhoRepository.save(tonKho);
                }
                log.info("[QuanLyDonHang] ✅ Hoàn kho xong khi giao thất bại.");

                // 2. Nhả IMEI ngay lập tức (nếu có)
                List<MayDienThoai> imeis = mayDienThoaiRepository.findByDonHangIdAndTinhTrang(donHangId, "da_ban");
                if (!imeis.isEmpty()) {
                        for (MayDienThoai may : imeis) {
                                may.setTinhTrang("trong_kho");
                                may.setDonHang(null);
                                mayDienThoaiRepository.save(may);
                        }
                        log.info("[QuanLyDonHang] ✅ Đã nhả {} IMEI về kho do giao thất bại.", imeis.size());
                }

                // 3. Cập nhật trạng thái và ghi chú
                boolean daThanhToanOnline = "da_thanh_toan".equals(donHang.getTrangThaiThanhToan());
                if (daThanhToanOnline) {
                        donHang.setTrangThai("cho_hoan_tien");
                        log.info("[QuanLyDonHang] ℹ️ Đơn [{}] đã thanh toán -> chuyển sang chờ hoàn tiền.", donHang.getMaDonHang());
                } else {
                        donHang.setTrangThai("giao_that_bai");
                        donHang.setTrangThaiThanhToan("chua_thanh_toan");
                }
                donHang.setUpdatedAt(now);

                String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
                donHang.setGhiChu(ghiChuCu + "[GIAO THẤT BẠI " + now.toLocalDate() + "]: " + lyDo);

                return donHangRepository.save(donHang);
        }

        /**
         * Validate luồng chuyển trạng thái giao hàng hợp lệ.
         * <p>
         * Luồng cho phép:
         * 
         * <pre>
         *   da_xac_nhan → dang_giao
         *   dang_giao   → da_hoan_thanh
         * </pre>
         *
         * @param maDonHang        Mã đơn hàng (dùng để hiển thị trong thông báo lỗi).
         * @param trangThaiHienTai Trạng thái hiện tại của đơn.
         * @param trangThaiMoi     Trạng thái muốn chuyển sang.
         * @throws IllegalArgumentException nếu chuyển trạng thái không theo đúng luồng.
         */
        private void validLuongTrangThai(String maDonHang, String trangThaiHienTai, String trangThaiMoi) {
                boolean hopLe = switch (trangThaiHienTai) {
                        case "da_xac_nhan" -> "dang_giao".equals(trangThaiMoi);
                        case "dang_giao" -> "da_hoan_thanh".equals(trangThaiMoi);
                        default -> false;
                };

                if (!hopLe) {
                        throw new IllegalArgumentException(String.format(
                                        "Không thể chuyển trạng thái đơn hàng [%s] từ '%s' sang '%s'. " +
                                                        "Luồng hợp lệ: da_xac_nhan → dang_giao → da_hoan_thanh.",
                                        maDonHang, trangThaiHienTai, trangThaiMoi));
                }
        }
}
