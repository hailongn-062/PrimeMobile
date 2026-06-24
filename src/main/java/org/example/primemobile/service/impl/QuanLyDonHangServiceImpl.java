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
 *   <li><b>§2.2.5 – Trừ kho khi xác nhận:</b> Kho_online bị trừ đúng tại bước {@code xacNhanDonHang},
 *       không phải lúc khách đặt hàng.</li>
 *   <li><b>§3.1 – Safety Stock Rule:</b> Sau khi trừ, tồn kho {@code kho_online}
 *       KHÔNG ĐƯỢC xuống dưới {@value #TON_KHO_TOI_THIEU} đơn vị / SKU.</li>
 *   <li><b>§2.2.7 – Hoàn kho khi hủy:</b> Nếu đơn đang ở {@code da_xac_nhan} hoặc {@code dang_giao}
 *       (kho đã bị trừ), BẮT BUỘC cộng hoàn lại vào kho_online khi hủy.</li>
 *   <li><b>§7.4 – Tạm hoãn tích điểm:</b> Không viết code cộng điểm / cộng tong_chi_tieu
 *       khi đơn chuyển sang {@code da_giao}.</li>
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

    /** Mức tồn kho tối thiểu bắt buộc (system_rules.md §3.1). */
    private static final int TON_KHO_TOI_THIEU = 5;

    /** Loại kho phục vụ đơn hàng online. */
    private static final String LOAI_KHO_ONLINE = "kho_online";

    /** Các trạng thái đơn hàng mà kho đã bị trừ — cần hoàn kho khi hủy. */
    private static final Set<String> TRANG_THAI_DA_TRU_KHO = Set.of("da_xac_nhan", "dang_giao");

    // ───────────────────────────────────────────────────────────────────────
    // DEPENDENCIES
    // ───────────────────────────────────────────────────────────────────────

    private final DonHangRepository             donHangRepository;
    private final ChiTietDonHangRepository      chiTietDonHangRepository;
    private final KhoRepository                 khoRepository;
    private final TonKhoRepository              tonKhoRepository;
    private final NguoiDungRepository           nguoiDungRepository;

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
        String tt  = (trangThai   != null && !trangThai.isBlank())   ? trangThai.trim()   : null;
        String ma  = (maDonHang   != null && !maDonHang.isBlank())   ? maDonHang.trim()   : null;
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
     *   <li>Validate đơn hàng ở trạng thái {@code "cho_xac_nhan"}.</li>
     *   <li>Load ChiTietDonHang với FETCH JOIN (tránh N+1).</li>
     *   <li><b>Fail-Fast — PRE-VALIDATE toàn bộ:</b>
     *       Với mỗi SKU: kiểm tra tồn kho kho_online &ge; (soLuong + Safety Stock).
     *       Ném lỗi ngay nếu bất kỳ SKU nào vi phạm — chưa trừ dòng nào.</li>
     *   <li><b>EXECUTE:</b> Tất cả pass → trừ kho từng dòng.</li>
     *   <li>Cập nhật trạng thái đơn → {@code "da_xac_nhan"}, ghi nhân viên xử lý.</li>
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

        // --- Lấy danh sách chi tiết đơn (eager-load) ---
        List<ChiTietDonHang> danhSachChiTiet =
                chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

        if (danhSachChiTiet.isEmpty()) {
            throw new IllegalArgumentException(
                    "Đơn hàng [" + donHang.getMaDonHang() + "] không có sản phẩm nào. " +
                    "Không thể xác nhận đơn rỗng.");
        }

        // --- Lấy kho online ---
        Kho khoOnline = layKhoOnlineHoacNemLoi();

        // ═══════════════════════════════════════════════════════════════════
        // BƯỚC FAIL-FAST: PRE-VALIDATE TOÀN BỘ trước khi trừ bất kỳ dòng nào
        // ═══════════════════════════════════════════════════════════════════
        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
            BienTheSanPham bienThe = chiTiet.getBienTheSanPham();

            TonKho tonKho = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoOnline, bienThe)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(
                            "Không tìm thấy tồn kho cho sản phẩm [%s] tại kho online. " +
                            "Vui lòng kiểm tra dữ liệu kho.",
                            bienThe.getMaSku())));

            int tonKhoSauKhiTru = tonKho.getSoLuong() - chiTiet.getSoLuong();

            // ⚠️ SAFETY STOCK RULE §3.1: Sau khi trừ KHÔNG được < 5
            if (tonKhoSauKhiTru < TON_KHO_TOI_THIEU) {
                throw new IllegalArgumentException(String.format(
                        "Vi phạm quy tắc tồn kho tối thiểu khi xác nhận đơn [%s]. " +
                        "Sản phẩm [%s] tại kho online: Tồn kho = %d, Bán = %d, " +
                        "Còn lại = %d (< mức tối thiểu %d).",
                        donHang.getMaDonHang(), bienThe.getMaSku(),
                        tonKho.getSoLuong(), chiTiet.getSoLuong(),
                        tonKhoSauKhiTru, TON_KHO_TOI_THIEU));
            }
        }

        // ═══════════════════════════════════════════════════════════════════
        // EXECUTE: Tất cả SKU đã pass → Trừ kho thực tế
        // ═══════════════════════════════════════════════════════════════════
        LocalDateTime now = LocalDateTime.now();
        for (ChiTietDonHang chiTiet : danhSachChiTiet) {
            TonKho tonKho = tonKhoRepository
                    .findByKhoAndBienTheSanPham(khoOnline, chiTiet.getBienTheSanPham())
                    .orElseThrow(); // Đã validate ở trên, không thể null

            int soLuongTruoc = tonKho.getSoLuong();
            tonKho.setSoLuong(tonKho.getSoLuong() - chiTiet.getSoLuong());
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
    // 3. CẬP NHẬT LỘ TRÌNH GIAO HÀNG
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng trạng thái hợp lệ:</h3>
     * <pre>
     *   da_xac_nhan → dang_giao   (nhân viên đã giao cho đơn vị vận chuyển)
     *   dang_giao   → da_giao     (đơn vị vận chuyển xác nhận giao thành công)
     * </pre>
     *
     * ⚠️ Tạm hoãn (§7.4): Không cộng điểm thưởng / cập nhật tong_chi_tieu khi da_giao.
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
        if ("da_giao".equals(trangThaiMoi)) {
            donHang.setNgayGiaoThucTe(now);
            // ⚠️ TODO (§7.4): Cộng điểm thưởng & cập nhật tong_chi_tieu cho khách hàng
            // sẽ được triển khai ở sprint sau khi có lệnh mới.
            log.info("[QuanLyDonHang] ℹ️ §7.4 Tạm hoãn: Chưa cộng điểm/tong_chi_tieu cho đơn [{}].",
                    donHang.getMaDonHang());
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
     *   <li>{@code cho_xac_nhan}: Kho chưa bị trừ → chỉ đổi trạng thái, KHÔNG thao tác kho.</li>
     *   <li>{@code da_xac_nhan} | {@code dang_giao}: Kho ĐÃ bị trừ trước đó
     *       → BẮT BUỘC cộng hoàn lại số lượng vào {@code kho_online}.</li>
     *   <li>{@code da_giao} | {@code da_huy}: Không được hủy → ném lỗi.</li>
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

        // Chỉ cho phép hủy khi đang ở các trạng thái hủy được
        if ("da_giao".equals(trangThaiHienTai)) {
            throw new IllegalArgumentException(String.format(
                    "Đơn hàng [%s] đã giao thành công, không thể hủy.", donHang.getMaDonHang()));
        }
        if ("da_huy".equals(trangThaiHienTai)) {
            throw new IllegalArgumentException(String.format(
                    "Đơn hàng [%s] đã bị hủy trước đó.", donHang.getMaDonHang()));
        }

        LocalDateTime now = LocalDateTime.now();

        // ═══════════════════════════════════════════════════════════════════
        // HOÀN KHO — chỉ thực hiện khi kho đã bị trừ trước đó (§2.2.7)
        // ═══════════════════════════════════════════════════════════════════
        if (TRANG_THAI_DA_TRU_KHO.contains(trangThaiHienTai)) {
            log.info("[QuanLyDonHang] 🔄 Hoàn kho — đơn [{}] đang ở '{}', cần cộng hoàn kho_online.",
                    donHang.getMaDonHang(), trangThaiHienTai);

            List<ChiTietDonHang> danhSachChiTiet =
                    chiTietDonHangRepository.findByDonHangIdWithDetails(donHangId);

            Kho khoOnline = layKhoOnlineHoacNemLoi();

            for (ChiTietDonHang chiTiet : danhSachChiTiet) {
                BienTheSanPham bienThe = chiTiet.getBienTheSanPham();

                TonKho tonKho = tonKhoRepository
                        .findByKhoAndBienTheSanPham(khoOnline, bienThe)
                        .orElseThrow(() -> new EntityNotFoundException(String.format(
                                "Không tìm thấy tồn kho cho [%s] tại kho online khi hoàn hàng.",
                                bienThe.getMaSku())));

                int soLuongTruoc = tonKho.getSoLuong();
                tonKho.setSoLuong(tonKho.getSoLuong() + chiTiet.getSoLuong());
                tonKho.setUpdatedAt(now);
                tonKhoRepository.save(tonKho);

                log.debug("[QuanLyDonHang] Hoàn kho — sku=[{}]: {} + {} = {}",
                        bienThe.getMaSku(), soLuongTruoc,
                        chiTiet.getSoLuong(), tonKho.getSoLuong());
            }

            log.info("[QuanLyDonHang] ✅ Hoàn kho xong — {} dòng CTDH được cộng lại vào kho_online.",
                    danhSachChiTiet.size());
        } else {
            // cho_xac_nhan: kho chưa bị trừ → không cần hoàn kho
            log.info("[QuanLyDonHang] ℹ️ Đơn [{}] ở '{}' — kho chưa bị trừ, bỏ qua bước hoàn kho.",
                    donHang.getMaDonHang(), trangThaiHienTai);
        }

        // --- Cập nhật trạng thái đơn hàng ---
        donHang.setTrangThai("da_huy");
        donHang.setUpdatedAt(now);

        // Ghi lý do hủy vào ghiChu (nối thêm, không ghi đè)
        String ghiChuCu = (donHang.getGhiChu() != null) ? donHang.getGhiChu() + " | " : "";
        donHang.setGhiChu(ghiChuCu + "[HỦY ĐƠN " + now.toLocalDate() + "]: " + lyDoHuy);

        donHangRepository.save(donHang);

        log.info("[QuanLyDonHang] ✅ Hủy đơn thành công — maDonHang={}", donHang.getMaDonHang());
        return donHang;
    }

    // =========================================================================
    // PRIVATE HELPER METHODS
    // =========================================================================

    /**
     * Tìm kho online trong hệ thống. Ném lỗi rõ ràng nếu chưa cấu hình.
     */
    private Kho layKhoOnlineHoacNemLoi() {
        return khoRepository.findByLoai(LOAI_KHO_ONLINE)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy kho online trong hệ thống. " +
                        "Vui lòng kiểm tra dữ liệu bảng kho."));
    }

    /**
     * Validate luồng chuyển trạng thái giao hàng hợp lệ.
     * <p>
     * Luồng cho phép:
     * <pre>
     *   da_xac_nhan → dang_giao
     *   dang_giao   → da_giao
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
            case "dang_giao"   -> "da_giao".equals(trangThaiMoi);
            default            -> false;
        };

        if (!hopLe) {
            throw new IllegalArgumentException(String.format(
                    "Không thể chuyển trạng thái đơn hàng [%s] từ '%s' sang '%s'. " +
                    "Luồng hợp lệ: da_xac_nhan → dang_giao → da_giao.",
                    maDonHang, trangThaiHienTai, trangThaiMoi));
        }
    }
}
