package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KhuyenMaiServiceImpl implements IKhuyenMaiService {

    private final ChuongTrinhKhuyenMaiRepository ctkmRepo;
    private final ChiTietFlashSaleRepository      chiTietFlashSaleRepo;
    private final PhamViKhuyenMaiRepository       phamViKhuyenMaiRepo;
    private final BienTheSanPhamRepository        bienTheSanPhamRepo;
    private final SanPhamRepository               sanPhamRepo;

    // ═══════════════════════════════════════════════════════════════════════
    // CRUD CHÍNH
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ChuongTrinhKhuyenMai> layDanhSach() {
        return ctkmRepo.findAllByOrderByIdDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public ChuongTrinhKhuyenMai layTheoId(Integer id) {
        return ctkmRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + id));
    }

    /**
     * Hàm lưu chung: Tự động phân biệt Thêm mới (id == null) / Cập nhật (id != null).
     * Validate logic theo từng loại khuyến mãi.
     */
    @Override
    @Transactional
    public ChuongTrinhKhuyenMai luu(ChuongTrinhKhuyenMai ctkm) {
        validateTheoLoai(ctkm);

        if (ctkm.getId() != null) {
            // === CẬP NHẬT ===
            ChuongTrinhKhuyenMai entity = ctkmRepo.findById(ctkm.getId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CTKM ID: " + ctkm.getId()));

            entity.setTenCtkm(ctkm.getTenCtkm());
            entity.setMoTa(ctkm.getMoTa());
            entity.setGiaTriUuDai(ctkm.getGiaTriUuDai());

            entity.setNgayBatDau(ctkm.getNgayBatDau());
            entity.setNgayKetThuc(ctkm.getNgayKetThuc());
            // Chỉ cập nhật các trường thuộc loại tương ứng
            entity.setDonHangToiThieu(ctkm.getDonHangToiThieu());
            entity.setGioFlashBatDau(ctkm.getGioFlashBatDau());
            entity.setGioFlashKetThuc(ctkm.getGioFlashKetThuc());

            if (ctkm.getTrangThai() != null && !ctkm.getTrangThai().isBlank()) {
                entity.setTrangThai(ctkm.getTrangThai());
            }

            ChuongTrinhKhuyenMai saved = ctkmRepo.save(entity);
            log.info("[KhuyenMai] Cập nhật CTKM id={} loai={}", saved.getId(), saved.getLoai());
            return saved;

        } else {
            // === THÊM MỚI ===
            if (ctkm.getTrangThai() == null || ctkm.getTrangThai().isBlank()) {
                ctkm.setTrangThai("chua_bat_dau");
            }
            if (ctkm.getSoLanDaDung() == null) {
                ctkm.setSoLanDaDung(0);
            }
            ChuongTrinhKhuyenMai saved = ctkmRepo.save(ctkm);
            log.info("[KhuyenMai] Tạo mới CTKM id={} loai={} ten={}", saved.getId(), saved.getLoai(), saved.getTenCtkm());
            return saved;
        }
    }

    @Override
    @Transactional
    public void xoa(Integer id) {
        if (!ctkmRepo.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy CTKM ID: " + id);
        }
        ctkmRepo.deleteById(id);
        log.info("[KhuyenMai] Đã xóa CTKM id={}", id);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUB-FORM: FLASH SALE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ChiTietFlashSale> layChiTietFlashSale(Integer ctkmId) {
        return chiTietFlashSaleRepo.findByChuongTrinhKhuyenMaiId(ctkmId);
    }

    @Override
    @Transactional
    public void themChiTietFlashSale(Integer ctkmId, Integer bienTheId,
                                     BigDecimal phanTramGiam, Integer soLuongGioiHan) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CTKM ID: " + ctkmId));
        BienTheSanPham bt = bienTheSanPhamRepo.findById(bienTheId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể ID: " + bienTheId));

        if (phanTramGiam == null || phanTramGiam.compareTo(BigDecimal.ZERO) <= 0
                || phanTramGiam.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm phải từ 0.01 đến 100.");
        }
        if (soLuongGioiHan == null || soLuongGioiHan <= 0) {
            throw new IllegalArgumentException("Số lượng giới hạn phải lớn hơn 0.");
        }

        // Kiểm tra biến thể đã tồn tại trong flash sale chưa
        boolean daConan = chiTietFlashSaleRepo.findByChuongTrinhKhuyenMaiId(ctkmId)
                .stream().anyMatch(c -> c.getBienTheSanPham().getId().equals(bienTheId));
        if (daConan) {
            throw new IllegalArgumentException("Biến thể này đã có trong Flash Sale. Vui lòng xóa dòng cũ trước khi thêm lại.");
        }

        ChiTietFlashSale ct = ChiTietFlashSale.builder()
                .chuongTrinhKhuyenMai(ctkm)
                .bienTheSanPham(bt)
                .phanTramGiam(phanTramGiam)
                .soLuongGioiHan(soLuongGioiHan)
                .daBan(0)
                .build();
        chiTietFlashSaleRepo.save(ct);
        log.info("[KhuyenMai] Thêm biến thể id={} vào Flash Sale id={}", bienTheId, ctkmId);
    }

    @Override
    @Transactional
    public void xoaChiTietFlashSale(Integer chiTietId) {
        chiTietFlashSaleRepo.deleteById(chiTietId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SUB-FORM: PHẠM VI ÁP DỤNG (giam_gia_truc_tiep)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<PhamViKhuyenMai> layPhamVi(Integer ctkmId) {
        return phamViKhuyenMaiRepo.findByChuongTrinhKhuyenMaiId(ctkmId);
    }

    @Override
    @Transactional
    public void themPhamVi(Integer ctkmId, Integer sanPhamId) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CTKM ID: " + ctkmId));
        if (sanPhamId == null) {
            throw new IllegalArgumentException("Vui lòng chọn sản phẩm cần áp dụng.");
        }
        SanPham sp = sanPhamRepo.findById(sanPhamId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm ID: " + sanPhamId));

        // Kiểm tra trùng
        boolean daConan = phamViKhuyenMaiRepo.findByChuongTrinhKhuyenMaiId(ctkmId)
                .stream().anyMatch(p -> p.getSanPham() != null && p.getSanPham().getId().equals(sanPhamId));
        if (daConan) {
            throw new IllegalArgumentException("Sản phẩm này đã có trong danh sách áp dụng.");
        }

        PhamViKhuyenMai pv = new PhamViKhuyenMai();
        pv.setChuongTrinhKhuyenMai(ctkm);
        pv.setSanPham(sp);
        phamViKhuyenMaiRepo.save(pv);
        log.info("[KhuyenMai] Thêm sản phẩm id={} vào phạm vi CTKM id={}", sanPhamId, ctkmId);
    }

    @Override
    @Transactional
    public void xoaPhamVi(Integer phamViId) {
        phamViKhuyenMaiRepo.deleteById(phamViId);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // PUBLIC (FRONTEND)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ChuongTrinhKhuyenMai> layKhuyenMaiDangDienRa() {
        return ctkmRepo.layKhuyenMaiDangDienRa();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POS — BÁN HÀNG TẠI QUẦY
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * {@inheritDoc}
     *
     * <h3>Thuật toán (O(n), 1 DB round-trip):</h3>
     * <ol>
     *   <li>Query DB: Lấy tất cả CTKM đang diễn ra, loại áp dụng toàn đơn hàng
     *       ({@code phan_tram}, {@code don_hang_toi_thieu}).</li>
     *   <li>Filter eligibility: Bỏ qua CTKM có {@code donHangToiThieu > tongTienHang}.</li>
     *   <li>Tính tiền giảm theo loại:
     *       <ul>
     *         <li>{@code phan_tram}          → tongTienHang × (giaTriUuDai / 100)</li>
     *         <li>{@code don_hang_toi_thieu} → tongTienHang × (giaTriUuDai / 100)</li>
     *       </ul>
     *   </li>
     *   <li>Trả về CTKM có tiền giảm lớn nhất. Nếu bằng nhau, ưu tiên ID nhỏ hơn
     *       (CTKM được tạo trước).</li>
     * </ol>
     *
     * @param tongTienHang Tổng tiền hàng (phải >= 0). Nếu null hoặc <= 0, trả về null ngay.
     * @return CTKM tốt nhất hoặc {@code null} nếu không có CTKM phù hợp.
     */
    @Override
    @Transactional(readOnly = true)
    public ChuongTrinhKhuyenMai timKhuyenMaiTotNhatChoDonHang(BigDecimal tongTienHang) {

        // Guard: Không thể áp khuyến mãi nếu tổng tiền hàng không hợp lệ
        if (tongTienHang == null || tongTienHang.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[KhuyenMai-POS] tongTienHang không hợp lệ ({}), bỏ qua tìm khuyến mãi.",
                    tongTienHang);
            return null;
        }

        List<ChuongTrinhKhuyenMai> danhSach = ctkmRepo.layKhuyenMaiApDungToanDonHang();

        if (danhSach.isEmpty()) {
            log.debug("[KhuyenMai-POS] Không có CTKM nào đang diễn ra cho toàn đơn hàng.");
            return null;
        }

        Optional<ChuongTrinhKhuyenMai> ketQua = danhSach.stream()
                // ─── Bước 1: Filter điều kiện đơn tối thiểu ───────────────
                .filter(ctkm -> {
                    BigDecimal nguong = ctkm.getDonHangToiThieu();
                    // Nếu không có ngưỡng tối thiểu → luôn đủ điều kiện
                    if (nguong == null) return true;
                    // Chỉ áp dụng khi tổng tiền hàng >= ngưỡng
                    return tongTienHang.compareTo(nguong) >= 0;
                })
                // ─── Bước 2: Chọn CTKM có tiền giảm lớn nhất ────────────
                .max(Comparator
                        .comparing(ctkm -> tinhTienGiam(ctkm, tongTienHang),
                                   Comparator.naturalOrder()));

        ketQua.ifPresentOrElse(
                ctkm -> log.info(
                        "[KhuyenMai-POS] Chọn CTKM tốt nhất: id={}, ten='{}', loai={}, " +
                        "giaTriUuDai={}%, tienGiam={}, tongTienHang={}",
                        ctkm.getId(), ctkm.getTenCtkm(), ctkm.getLoai(),
                        ctkm.getGiaTriUuDai(),
                        tinhTienGiam(ctkm, tongTienHang),
                        tongTienHang),
                () -> log.debug(
                        "[KhuyenMai-POS] Không có CTKM nào đủ điều kiện cho đơn {}.",
                        tongTienHang)
        );

        return ketQua.orElse(null);
    }

    @Override
    @Transactional
    public void toggleTrangThai(Integer id) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + id));

        if ("tam_dung".equals(ctkm.getTrangThai())) {
            // === Mở lại: tính toán trạng thái thực tế theo thời gian hiện tại ===
            java.time.LocalDateTime now = java.time.LocalDateTime.now();

            String trangThaiMoi;
            if (now.isBefore(ctkm.getNgayBatDau())) {
                trangThaiMoi = "chua_bat_dau";
            } else if (now.isAfter(ctkm.getNgayKetThuc())) {
                trangThaiMoi = "da_ket_thuc";
            } else {
                trangThaiMoi = "dang_dien_ra";
            }

            ctkm.setTrangThai(trangThaiMoi);
            log.info("[KhuyenMai] Mở lại CTKM id={} → trạng thái mới = {}", id, trangThaiMoi);

        } else {
            // === Tạm dừng bất kỳ trạng thái khác ===
            ctkm.setTrangThai("tam_dung");
            log.info("[KhuyenMai] Tạm dừng CTKM id={} (trước đó: {})", id, ctkm.getTrangThai());
        }

        ctkmRepo.save(ctkm);
    }


    // ═══════════════════════════════════════════════════════════════════════
    // PRIVATE HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private void validateTheoLoai(ChuongTrinhKhuyenMai req) {
        if (req.getTenCtkm() == null || req.getTenCtkm().isBlank()) {
            throw new IllegalArgumentException("Tên chương trình khuyến mãi không được để trống.");
        }
        if (req.getLoai() == null || req.getLoai().isBlank()) {
            throw new IllegalArgumentException("Loại khuyến mãi không hợp lệ.");
        }
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được để trống.");
        }
        if (req.getNgayBatDau().isAfter(req.getNgayKetThuc())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }
        if (req.getGiaTriUuDai() == null || req.getGiaTriUuDai().compareTo(BigDecimal.ZERO) <= 0
                || req.getGiaTriUuDai().compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("Giá trị ưu đãi (%) phải từ 0.01 đến 100.");
        }

        switch (req.getLoai()) {
            case "don_hang_toi_thieu" -> {
                if (req.getDonHangToiThieu() == null || req.getDonHangToiThieu().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("Loại 'Đơn hàng tối thiểu' yêu cầu nhập số tiền tối thiểu hợp lệ.");
                }
            }
            case "flash_sale" -> {
                if (req.getGioFlashBatDau() == null || req.getGioFlashKetThuc() == null) {
                    throw new IllegalArgumentException("Flash Sale yêu cầu nhập giờ flash bắt đầu và kết thúc.");
                }
                if (req.getGioFlashBatDau().isAfter(req.getGioFlashKetThuc())) {
                    throw new IllegalArgumentException("Giờ flash bắt đầu phải trước giờ flash kết thúc.");
                }
            }
            case "phan_tram", "giam_gia_truc_tiep" -> { /* không yêu cầu thêm */ }
            default -> throw new IllegalArgumentException("Loại khuyến mãi không hợp lệ: " + req.getLoai());
        }
    }   // ← đóng validateTheoLoai()

    /**
     * Tính số tiền được giảm của 1 CTKM với tổng tiền hàng cho trước.
     *
     * <p>Công thức:
     * <ul>
     *   <li>{@code "phan_tram"} / {@code "don_hang_toi_thieu"}:
     *       {@code tongTienHang × (giaTriUuDai / 100)}, làm tròn HALF_UP 2 chữ số thập phân.</li>
     * </ul>
     *
     * @param ctkm         Chương trình khuyến mãi cần tính.
     * @param tongTienHang Tổng tiền hàng của đơn.
     * @return Số tiền được giảm (>= 0). Trả về ZERO nếu giaTriUuDai null hoặc loại không xác định.
     */
    private BigDecimal tinhTienGiam(ChuongTrinhKhuyenMai ctkm, BigDecimal tongTienHang) {
        if (ctkm.getGiaTriUuDai() == null) {
            return BigDecimal.ZERO;
        }
        // Cả 2 loại "phan_tram" và "don_hang_toi_thieu" đều giảm theo phần trăm
        return tongTienHang
                .multiply(ctkm.getGiaTriUuDai())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }
}
