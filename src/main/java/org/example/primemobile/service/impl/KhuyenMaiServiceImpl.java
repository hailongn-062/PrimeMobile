package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.KhuyenMaiResult;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KhuyenMaiServiceImpl implements IKhuyenMaiService {

    private final ChuongTrinhKhuyenMaiRepository ctkmRepo;
    private final PhamViKhuyenMaiRepository phamViKhuyenMaiRepo;
    private final BienTheSanPhamRepository bienTheSanPhamRepo;
    private final SanPhamRepository sanPhamRepo;

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
    public List<ChuongTrinhKhuyenMai> timKiemVaLoc(String tuKhoa, String trangThai, String loai, java.time.LocalDateTime tuNgay, java.time.LocalDateTime denNgay) {
        return ctkmRepo.timKiemVaLocCtkm(tuKhoa, trangThai, loai, tuNgay, denNgay);
    }

    @Override
    @Transactional(readOnly = true)
    public ChuongTrinhKhuyenMai layTheoId(Integer id) {
        return ctkmRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + id));
    }

    /**
     * Hàm lưu chung: Tự động phân biệt Thêm mới (id == null) / Cập nhật (id !=
     * null).
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
            entity.setGiamToiDa(ctkm.getGiamToiDa());

            if (!"tam_dung".equals(entity.getTrangThai())) {
                LocalDateTime now = LocalDateTime.now();
                if (now.isBefore(entity.getNgayBatDau())) {
                    entity.setTrangThai("chua_bat_dau");
                } else if (now.isAfter(entity.getNgayKetThuc())) {
                    entity.setTrangThai("da_ket_thuc");
                } else {
                    entity.setTrangThai("dang_dien_ra");
                }
            }

            ChuongTrinhKhuyenMai saved = ctkmRepo.save(entity);
            log.info("[KhuyenMai] Cập nhật CTKM id={} loai={}", saved.getId(), saved.getLoai());
            return saved;

        } else {
            // === THÊM MỚI ===
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(ctkm.getNgayBatDau())) {
                ctkm.setTrangThai("chua_bat_dau");
            } else if (now.isAfter(ctkm.getNgayKetThuc())) {
                ctkm.setTrangThai("da_ket_thuc");
            } else {
                ctkm.setTrangThai("dang_dien_ra");
            }
            ChuongTrinhKhuyenMai saved = ctkmRepo.save(ctkm);
            log.info("[KhuyenMai] Tạo mới CTKM id={} loai={} ten={}", saved.getId(), saved.getLoai(),
                    saved.getTenCtkm());
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
    // SUB-FORM: PHẠM VI ÁP DỤNG (theo_san_pham)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<PhamViKhuyenMai> layPhamVi(Integer ctkmId) {
        return phamViKhuyenMaiRepo.findByChuongTrinhKhuyenMaiId(ctkmId);
    }

    @Override
    @Transactional
    public void themPhamVi(Integer ctkmId, Integer bienTheId) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CTKM ID: " + ctkmId));
        if (bienTheId == null) {
            throw new IllegalArgumentException("Vui lòng chọn biến thể cần áp dụng.");
        }
        BienTheSanPham bt = bienTheSanPhamRepo.findById(bienTheId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể ID: " + bienTheId));

        // Kiểm tra trùng
        boolean daCo = phamViKhuyenMaiRepo.findByChuongTrinhKhuyenMaiId(ctkmId)
                .stream().anyMatch(p -> p.getBienThe() != null && p.getBienThe().getId().equals(bienTheId));
        if (daCo) {
            throw new IllegalArgumentException("Biến thể này đã có trong danh sách áp dụng.");
        }

        PhamViKhuyenMai pv = new PhamViKhuyenMai();
        pv.setChuongTrinhKhuyenMai(ctkm);
        pv.setBienThe(bt);
        phamViKhuyenMaiRepo.save(pv);
        log.info("[KhuyenMai] Thêm biến thể id={} vào phạm vi CTKM id={}", bienTheId, ctkmId);
    }

    @Override
    @Transactional
    public void themNhieuPhamVi(Integer ctkmId, List<Integer> bienTheIds) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy CTKM ID: " + ctkmId));
        
        if (bienTheIds == null || bienTheIds.isEmpty()) return;

        List<PhamViKhuyenMai> hienTai = phamViKhuyenMaiRepo.findByChuongTrinhKhuyenMaiId(ctkmId);

        for (Integer bienTheId : bienTheIds) {
            boolean daCo = hienTai.stream().anyMatch(p -> p.getBienThe() != null && p.getBienThe().getId().equals(bienTheId));
            if (!daCo) {
                BienTheSanPham bt = bienTheSanPhamRepo.findById(bienTheId).orElse(null);
                if (bt != null) {
                    PhamViKhuyenMai pv = new PhamViKhuyenMai();
                    pv.setChuongTrinhKhuyenMai(ctkm);
                    pv.setBienThe(bt);
                    phamViKhuyenMaiRepo.save(pv);
                }
            }
        }
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
    // CHỌN MÃ GIẢM GIÁ CHO ĐƠN HÀNG (ONLINE & POS)
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public List<ChuongTrinhKhuyenMai> layDanhSachChoChonDonHang() {
        return ctkmRepo.layKhuyenMaiApDungToanDonHang();
    }

    @Override
    @Transactional(readOnly = true)
    public KhuyenMaiResult apDungCtkmTheoId(Integer ctkmId, BigDecimal tongTienHang) {
        if (tongTienHang == null || tongTienHang.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng tiền hàng không hợp lệ.");
        }

        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new IllegalArgumentException("Mã giảm giá không tồn tại."));

        if (!"theo_don_hang".equals(ctkm.getLoai())) {
            throw new IllegalArgumentException("Mã này không áp dụng cho toàn đơn hàng.");
        }
        
        if (!"dang_dien_ra".equals(ctkm.getTrangThai())) {
            throw new IllegalArgumentException("Mã giảm giá đã hết hạn hoặc chưa kích hoạt.");
        }

        if (ctkm.getDonHangToiThieu() != null && tongTienHang.compareTo(ctkm.getDonHangToiThieu()) < 0) {
            throw new IllegalArgumentException("Đơn hàng chưa đạt giá trị tối thiểu " + ctkm.getDonHangToiThieu() + "đ để dùng mã này.");
        }

        BigDecimal tienGiam = tongTienHang
                .multiply(ctkm.getGiaTriUuDai())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                
        if (ctkm.getGiamToiDa() != null && tienGiam.compareTo(ctkm.getGiamToiDa()) > 0) {
            tienGiam = ctkm.getGiamToiDa();
        }

        log.info("[KhuyenMai] Chủ động chọn CTKM: id={}, ten='{}', giảm {}%, tiền giảm={}",
                ctkm.getId(), ctkm.getTenCtkm(), ctkm.getGiaTriUuDai(), tienGiam);

        return KhuyenMaiResult.builder()
                .ctkmId(ctkm.getId())
                .tenCtkm(ctkm.getTenCtkm())
                .giaTriUuDai(ctkm.getGiaTriUuDai())
                .tienGiam(tienGiam)
                .tongSauGiam(tongTienHang.subtract(tienGiam))
                .build();
    }

    // ═══════════════════════════════════════════════════════════════════════
    // POS — BÁN HÀNG TẠI QUẦY
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public ChuongTrinhKhuyenMai timKhuyenMaiTotNhatChoDonHang(BigDecimal tongTienHang) {

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
                .filter(ctkm -> {
                    BigDecimal nguong = ctkm.getDonHangToiThieu();
                    if (nguong == null) return true;
                    return tongTienHang.compareTo(nguong) >= 0;
                })
                .max(Comparator.comparing(ctkm -> tinhTienGiam(ctkm, tongTienHang),
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
                        tongTienHang));

        return ketQua.orElse(null);
    }

    @Override
    @Transactional
    public void toggleTrangThai(Integer id) {
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + id));

        if ("tam_dung".equals(ctkm.getTrangThai())) {
            LocalDateTime now = LocalDateTime.now();
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
            ctkm.setTrangThai("tam_dung");
            log.info("[KhuyenMai] Tạm dừng CTKM id={} (trước đó: {})", id, ctkm.getTrangThai());
        }

        ctkmRepo.save(ctkm);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TÍNH GIÁ SAU KHUYẾN MÃI ĐỘNG
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    @Transactional(readOnly = true)
    public BigDecimal tinhGiaSauKhuyenMai(Integer bienTheId, BigDecimal tongTienHang) {
        BienTheSanPham bienThe = bienTheSanPhamRepo.findById(bienTheId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể ID: " + bienTheId));
        BigDecimal giaGoc = bienThe.getGiaBan();
        BigDecimal giaSauKM = giaGoc;

        // ── 1. Tính giá sau khuyến mãi theo biến thể sản phẩm (nếu có) ───────────────
        List<PhamViKhuyenMai> pvList = phamViKhuyenMaiRepo.findActiveGiamGiaTrucTiepByBienTheId(bienTheId);
        if (!pvList.isEmpty()) {
            PhamViKhuyenMai pv = pvList.get(0);
            BigDecimal phanTram = pv.getChuongTrinhKhuyenMai().getGiaTriUuDai();
            BigDecimal tienGiam = giaGoc.multiply(phanTram.divide(new BigDecimal("100"), 10, RoundingMode.HALF_UP));
            
            if (pv.getChuongTrinhKhuyenMai().getGiamToiDa() != null && tienGiam.compareTo(pv.getChuongTrinhKhuyenMai().getGiamToiDa()) > 0) {
                tienGiam = pv.getChuongTrinhKhuyenMai().getGiamToiDa();
            }
            giaSauKM = giaGoc.subtract(tienGiam);
            
            log.debug("[KhuyenMai] Theo sản phẩm: biếnTheId={}, giá gốc={}, giảm {}%, giá giảm={}",
                    bienTheId, giaGoc, phanTram, giaSauKM);
        }

        // (Bỏ tính khuyến mãi theo đơn hàng vào giá sản phẩm để tránh lỗi double discount)

        log.debug("[KhuyenMai] Kết quả cuối: biếnTheId={}, giá gốc={}, giá sau KM={}", bienTheId, giaGoc, giaSauKM);
        return giaSauKM;
    }

    // ── Tính khuyến mãi cho đơn hàng (dùng chung Online & POS) ────────────

    @Override
    @Transactional(readOnly = true)
    public KhuyenMaiResult tinhKhuyenMaiChoDonHang(BigDecimal tongTienHang) {
        if (tongTienHang == null || tongTienHang.compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("[KhuyenMai] tongTienHang không hợp lệ ({}), trả về không giảm giá.", tongTienHang);
            return KhuyenMaiResult.builder()
                    .tienGiam(BigDecimal.ZERO)
                    .tongSauGiam(tongTienHang != null ? tongTienHang : BigDecimal.ZERO)
                    .build();
        }

        ChuongTrinhKhuyenMai best = timKhuyenMaiTotNhatChoDonHang(tongTienHang);

        if (best == null) {
            log.debug("[KhuyenMai] Không có CTKM phù hợp cho đơn hàng trị giá {}.", tongTienHang);
            return KhuyenMaiResult.builder()
                    .tienGiam(BigDecimal.ZERO)
                    .tongSauGiam(tongTienHang)
                    .build();
        }

        BigDecimal tienGiam = tongTienHang
                .multiply(best.getGiaTriUuDai())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
                
        if (best.getGiamToiDa() != null && tienGiam.compareTo(best.getGiamToiDa()) > 0) {
            tienGiam = best.getGiamToiDa();
        }

        log.info("[KhuyenMai] Áp dụng CTKM cho đơn hàng: id={}, ten='{}', giảm {}%, tiền giảm={}, tổng sau giảm={}",
                best.getId(), best.getTenCtkm(), best.getGiaTriUuDai(), tienGiam, tongTienHang.subtract(tienGiam));

        return KhuyenMaiResult.builder()
                .ctkmId(best.getId())
                .tenCtkm(best.getTenCtkm())
                .giaTriUuDai(best.getGiaTriUuDai())
                .tienGiam(tienGiam)
                .tongSauGiam(tongTienHang.subtract(tienGiam))
                .build();
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
        if (req.getGiamToiDa() != null && req.getGiamToiDa().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giảm tối đa phải lớn hơn 0 nếu được thiết lập.");
        }

        switch (req.getLoai()) {
            case "theo_don_hang" -> {
                // Có thể kèm điều kiện donHangToiThieu (không bắt buộc)
                if (req.getDonHangToiThieu() != null
                        && req.getDonHangToiThieu().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Giá trị đơn hàng tối thiểu phải lớn hơn 0 nếu được thiết lập.");
                }
            }
            case "theo_san_pham" -> { /* không yêu cầu thêm */ }
            default -> throw new IllegalArgumentException("Loại khuyến mãi không hợp lệ: " + req.getLoai());
        }
    }

    private BigDecimal tinhTienGiam(ChuongTrinhKhuyenMai ctkm, BigDecimal tongTienHang) {
        if (ctkm.getGiaTriUuDai() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal tienGiam = tongTienHang
                .multiply(ctkm.getGiaTriUuDai())
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        if (ctkm.getGiamToiDa() != null && tienGiam.compareTo(ctkm.getGiamToiDa()) > 0) {
            tienGiam = ctkm.getGiamToiDa();
        }
        return tienGiam;
    }
}