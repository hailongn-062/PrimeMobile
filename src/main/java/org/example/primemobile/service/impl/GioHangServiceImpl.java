package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IGioHangService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Triển khai phân hệ Giỏ Hàng Online.
 *
 * <h2>Ràng buộc Tồn kho Online (Bắt buộc):</h2>
 * <p>
 * Mọi thao tác thêm/cập nhật đều kiểm tra tồn kho tại {@code kho_online}
 * (kho có {@code loai = 'kho_online'}).
 * Nếu số lượng yêu cầu vượt quá tồn kho thực tế → ném
 * {@link IllegalArgumentException} ngay.
 *
 * <h2>Upsert Logic (Quan trọng):</h2>
 * <p>
 * Khi thêm SKU đã có trong giỏ, Service cộng dồn số lượng thay vì INSERT bản
 * ghi mới,
 * đảm bảo ràng buộc UNIQUE (gio_hang_id, bien_the_san_pham_id) không bị vi
 * phạm.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GioHangServiceImpl implements IGioHangService {

    // -----------------------------------------------------------------------
    // CONSTANTS
    // -----------------------------------------------------------------------
    private static final String LOAI_KHO_ONLINE = "kho_online";
    private static final int TON_KHO_TOI_THIEU_DE_BAN = 5;

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------
    private final GioHangRepository gioHangRepository;
    private final ChiTietGioHangRepository chiTietGioHangRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final KhachHangRepository khachHangRepository;
    private final KhoRepository khoRepository;
    private final TonKhoRepository tonKhoRepository;
    private final IKhuyenMaiService khuyenMaiService; // ✅ Thêm service tính giá khuyến mãi động

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /**
     * {@inheritDoc}
     * <p>
     * Dùng query với FETCH JOIN để tránh N+1.
     * Tổng tiền tạm tính được tính tại Service Layer (không lưu DB).
     */
    @Override
    @Transactional(readOnly = true)
    public GioHang layGioHang(Integer khachHangId, String sessionId) {
        return timGioHangVoiChiTiet(khachHangId, sessionId).orElse(null);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Toàn bộ thao tác (tạo giỏ + upsert item + kiểm tra kho) trong 1 transaction.
     * Rollback hoàn toàn nếu vi phạm kho online.
     */
    @Override
    @Transactional
    public GioHang themVaoGioHang(Integer khachHangId, String sessionId,
                                  Integer bienTheSanPhamId, Integer soLuong) {
        // Validate đầu vào cơ bản
        if (soLuong == null || soLuong <= 0) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn 0.");
        }
        validateDinhDanhNguoiDung(khachHangId, sessionId);

        // Lấy biến thể sản phẩm
        BienTheSanPham bienThe = bienTheSanPhamRepository.findById(bienTheSanPhamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy biến thể sản phẩm ID: " + bienTheSanPhamId));

        // Lấy hoặc tạo giỏ hàng
        GioHang gioHang = timHoacTaoGioHang(khachHangId, sessionId);

        // Kiểm tra SKU đã có trong giỏ chưa (upsert)
        Optional<ChiTietGioHang> chiTietOpt = chiTietGioHangRepository
                .findByGioHangIdAndBienTheSanPhamId(gioHang.getId(), bienTheSanPhamId);

        int soLuongTong;
        if (chiTietOpt.isPresent()) {
            // SKU đã có → cộng dồn
            soLuongTong = chiTietOpt.get().getSoLuong() + soLuong;
        } else {
            // SKU chưa có → thêm mới
            soLuongTong = soLuong;
        }

        // ====================================================================
        // RÀNG BUỘC TỒN KHO ONLINE — LUẬT BẮT BUỘC
        // ====================================================================
        kiemTraTonKhoOnline(bienThe, soLuongTong);

        // Lưu hoặc cập nhật chi tiết giỏ hàng
        if (chiTietOpt.isPresent()) {
            ChiTietGioHang chiTiet = chiTietOpt.get();
            chiTiet.setSoLuong(soLuongTong);
            chiTietGioHangRepository.save(chiTiet);
            log.debug("[GioHang] Cộng dồn số lượng — gioHangId={}, maSku={}, soLuongMoi={}",
                    gioHang.getId(), bienThe.getMaSku(), soLuongTong);
        } else {
            ChiTietGioHang chiTietMoi = ChiTietGioHang.builder()
                    .gioHang(gioHang)
                    .bienTheSanPham(bienThe)
                    .soLuong(soLuongTong)
                    .ngayThem(LocalDateTime.now())
                    .build();
            chiTietGioHangRepository.save(chiTietMoi);
            gioHang.getChiTietGioHangs().add(chiTietMoi);
            log.debug("[GioHang] Thêm SKU mới vào giỏ — gioHangId={}, maSku={}",
                    gioHang.getId(), bienThe.getMaSku());
        }

        gioHang.setUpdatedAt(LocalDateTime.now());
        gioHangRepository.save(gioHang);

        log.info("[GioHang] Đã thêm vào giỏ — gioHangId={}, bienTheId={}, soLuongTong={}",
                gioHang.getId(), bienTheSanPhamId, soLuongTong);

        // Trả về giỏ hàng kèm chi tiết đã được load
        return timGioHangVoiChiTiet(khachHangId, sessionId).orElse(gioHang);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public GioHang capNhatSoLuong(Integer chiTietGioHangId, Integer soLuongMoi) {
        if (soLuongMoi == null || soLuongMoi <= 0) {
            throw new IllegalArgumentException("Số lượng cập nhật phải lớn hơn 0.");
        }

        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy dòng chi tiết giỏ hàng ID: " + chiTietGioHangId));

        // ====================================================================
        // RÀNG BUỘC TỒN KHO ONLINE — LUẬT BẮT BUỘC
        // ====================================================================
        kiemTraTonKhoOnline(chiTiet.getBienTheSanPham(), soLuongMoi);

        chiTiet.setSoLuong(soLuongMoi);
        chiTietGioHangRepository.save(chiTiet);

        GioHang gioHang = chiTiet.getGioHang();
        gioHang.setUpdatedAt(LocalDateTime.now());
        gioHangRepository.save(gioHang);

        log.info("[GioHang] Cập nhật số lượng — chiTietId={}, soLuongMoi={}", chiTietGioHangId, soLuongMoi);

        // Reload với chi tiết đầy đủ
        Integer khachHangId = gioHang.getKhachHang() != null ? gioHang.getKhachHang().getId() : null;
        String sessionId = gioHang.getSessionId();
        return timGioHangVoiChiTiet(khachHangId, sessionId).orElse(gioHang);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void xoaKhoiGioHang(Integer chiTietGioHangId) {
        ChiTietGioHang chiTiet = chiTietGioHangRepository.findById(chiTietGioHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy dòng chi tiết giỏ hàng ID: " + chiTietGioHangId));

        GioHang gioHang = chiTiet.getGioHang();
        chiTietGioHangRepository.delete(chiTiet);

        gioHang.setUpdatedAt(LocalDateTime.now());
        gioHangRepository.save(gioHang);

        log.info("[GioHang] Đã xóa SKU khỏi giỏ — chiTietId={}, gioHangId={}",
                chiTietGioHangId, gioHang.getId());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void xoaToanBoGioHang(Integer khachHangId, String sessionId) {
        timGioHang(khachHangId, sessionId).ifPresent(gh -> {
            gioHangRepository.delete(gh);
            log.info("[GioHang] Đã xóa toàn bộ giỏ hàng — gioHangId={}", gh.getId());
        });
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Kiểm tra tồn kho tại kho_online trước khi cho phép thêm/cập nhật số lượng.
     * <p>
     * Tìm kho online theo {@code loai = 'kho_online'}, sau đó truy vấn bảng
     * {@code ton_kho}.
     * Nếu không tìm thấy bản ghi tồn kho hoặc số lượng yêu cầu > tồn kho thực → ném
     * exception.
     *
     * @param bienThe Biến thể SKU cần kiểm tra.
     * @param soLuong Số lượng muốn đặt (tổng cuối cùng trong giỏ).
     * @throws IllegalArgumentException nếu kho online không đủ hàng.
     */
    private void kiemTraTonKhoOnline(BienTheSanPham bienThe, int soLuong) {
        Kho khoOnline = khoRepository.findByLoai(LOAI_KHO_ONLINE)
                .orElseThrow(() -> new IllegalStateException(
                        "Không tìm thấy kho online trong hệ thống. Liên hệ Admin."));

        int tonKhoHienTai = tonKhoRepository
                .findByKhoAndBienTheSanPham(khoOnline, bienThe)
                .map(TonKho::getSoLuong)
                .orElse(0);

        int soLuongCoTheBan = Math.max(tonKhoHienTai - TON_KHO_TOI_THIEU_DE_BAN, 0);
        if (tonKhoHienTai <= TON_KHO_TOI_THIEU_DE_BAN) {
            throw new IllegalArgumentException(
                    "Sản phẩm này chưa đủ tồn kho để bán online. Tồn kho phải lớn hơn "
                            + TON_KHO_TOI_THIEU_DE_BAN + ".");
        }

        if (soLuong > soLuongCoTheBan) {
            throw new IllegalArgumentException(
                    "Số lượng sản phẩm trong kho online không đủ. " +
                            "Yêu cầu: " + soLuong + ", có thể bán: " + soLuongCoTheBan + ".");
        }
    }

    /**
     * Tìm giỏ hàng đang tồn tại (ưu tiên theo khachHangId, fallback theo
     * sessionId).
     * Không eager-load chi tiết — dùng cho các thao tác ghi.
     */
    private Optional<GioHang> timGioHang(Integer khachHangId, String sessionId) {
        if (khachHangId != null) {
            return gioHangRepository.findByKhachHangId(khachHangId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return gioHangRepository.findBySessionId(sessionId);
        }
        return Optional.empty();
    }

    /**
     * Tìm giỏ hàng kèm chi tiết sản phẩm (FETCH JOIN) — dùng cho đọc/trả về
     * response.
     */
    private Optional<GioHang> timGioHangVoiChiTiet(Integer khachHangId, String sessionId) {
        if (khachHangId != null) {
            return gioHangRepository.findByKhachHangIdWithDetails(khachHangId);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            return gioHangRepository.findBySessionIdWithDetails(sessionId);
        }
        return Optional.empty();
    }

    /**
     * Lấy giỏ hàng hiện tại; nếu chưa có thì tạo mới.
     * Đây là điểm tạo {@link GioHang} duy nhất — đảm bảo mỗi khách chỉ có 1 giỏ.
     */
    private GioHang timHoacTaoGioHang(Integer khachHangId, String sessionId) {
        return timGioHang(khachHangId, sessionId).orElseGet(() -> {
            GioHang.GioHangBuilder builder = GioHang.builder()
                    .ngayTao(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now());

            if (khachHangId != null) {
                KhachHang khachHang = khachHangRepository.findById(khachHangId)
                        .orElseThrow(() -> new EntityNotFoundException(
                                "Không tìm thấy khách hàng ID: " + khachHangId));
                builder.khachHang(khachHang);
            } else {
                builder.sessionId(sessionId);
            }

            GioHang gioHangMoi = gioHangRepository.save(builder.build());
            log.info("[GioHang] Tạo giỏ hàng mới — gioHangId={}, khachHangId={}, sessionId={}",
                    gioHangMoi.getId(), khachHangId, sessionId);
            return gioHangMoi;
        });
    }

    /**
     * Kiểm tra ít nhất 1 trong 2 (khachHangId, sessionId) phải khác null/rỗng.
     * Khớp với CHECK constraint DB:
     * {@code (khach_hang_id IS NOT NULL OR session_id IS NOT NULL)}.
     */
    private void validateDinhDanhNguoiDung(Integer khachHangId, String sessionId) {
        boolean khongCoKhachHang = (khachHangId == null);
        boolean khongCoSession = (sessionId == null || sessionId.isBlank());
        if (khongCoKhachHang && khongCoSession) {
            throw new IllegalArgumentException(
                    "Phải cung cấp khachHangId hoặc sessionId để xác định giỏ hàng.");
        }
    }

    // =========================================================================
    // TÍNH TỔNG TIỀN GIỎ HÀNG
    // =========================================================================

    /**
     * {@inheritDoc}
     *
     * <p>
     * Cách tính:
     * <ol>
     *   <li>Tính tổng tiền sau khi áp dụng Flash Sale và Giảm giá trực tiếp
     *       cho từng sản phẩm (gọi {@link IKhuyenMaiService#tinhGiaSauKhuyenMai}
     *       với {@code tongTienHang = null}).</li>
     *   <li>Gọi {@link IKhuyenMaiService#tinhKhuyenMaiChoDonHang(BigDecimal)} để
     *       tính khuyến mãi toàn đơn (phần trăm và đơn hàng tối thiểu) trên
     *       tổng tiền đã có Flash Sale/Giảm trực tiếp.</li>
     *   <li>Trả về tổng tiền cuối cùng sau tất cả khuyến mãi.</li>
     * </ol>
     *
     * <p>
     * <b>Lưu ý:</b> Phương thức này đảm bảo áp dụng đúng thứ tự ưu tiên:
     * Flash Sale/Giảm trực tiếp (trên từng sản phẩm) trước,
     * sau đó mới giảm toàn đơn (phần trăm, đơn hàng tối thiểu).
     *
     * @param gioHang Giỏ hàng cần tính tổng.
     * @return Tổng tiền sau tất cả khuyến mãi.
     */
    @Override
    @Transactional(readOnly = true)
    public BigDecimal tinhTongTienTamTinh(GioHang gioHang) {
        if (gioHang == null || gioHang.getChiTietGioHangs() == null) {
            return BigDecimal.ZERO;
        }

        // 1. Tính tổng tiền sau Flash Sale và Giảm trực tiếp
        BigDecimal tongSauFlashVaTrucTiep = gioHang.getChiTietGioHangs().stream()
                .map(ct -> {
                    BienTheSanPham bt = ct.getBienTheSanPham();
                    // Tính giá sau flash / giảm trực tiếp (không áp dụng toàn đơn)
                    BigDecimal donGia = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
                    return donGia.multiply(BigDecimal.valueOf(ct.getSoLuong()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 2. Áp dụng khuyến mãi toàn đơn (phần trăm, đơn hàng tối thiểu)
        KhuyenMaiResult result = khuyenMaiService.tinhKhuyenMaiChoDonHang(tongSauFlashVaTrucTiep);

        // 3. Trả về tổng cuối cùng
        return result.getTongSauGiam();
    }
}