package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.MaGiamGia;
import org.example.primemobile.repository.ChuongTrinhKhuyenMaiRepository;
import org.example.primemobile.repository.MaGiamGiaRepository;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class KhuyenMaiServiceImpl implements IKhuyenMaiService {

    private final ChuongTrinhKhuyenMaiRepository ctkmRepo;
    private final MaGiamGiaRepository            maGiamGiaRepo;

    // ── ADMIN: Chương trình khuyến mãi ──────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<ChuongTrinhKhuyenMai> layDanhSach() {
        return ctkmRepo.findAll();
    }

    @Override
    @Transactional
    public ChuongTrinhKhuyenMai taoMoi(ChuongTrinhKhuyenMai request) {
        validateCtkmRequest(request);
        ChuongTrinhKhuyenMai entity = ChuongTrinhKhuyenMai.builder()
                .tenCtkm(request.getTenCtkm())
                .moTa(request.getMoTa())
                .loai(request.getLoai())
                .giaTriUuDai(request.getGiaTriUuDai())
                .laPhanTram(request.getLaPhanTram() != null ? request.getLaPhanTram() : false)
                .giamToiDa(request.getGiamToiDa())
                .ngayBatDau(request.getNgayBatDau())
                .ngayKetThuc(request.getNgayKetThuc())
                .gioFlashBatDau(request.getGioFlashBatDau())
                .gioFlashKetThuc(request.getGioFlashKetThuc())
                .soLuongToiDa(request.getSoLuongToiDa())
                .trangThai("chua_bat_dau")
                .build();
        ChuongTrinhKhuyenMai saved = ctkmRepo.save(entity);
        log.info("[KhuyenMai] Tạo CTKM thành công — id={}, ten={}", saved.getId(), saved.getTenCtkm());
        return saved;
    }

    @Override
    @Transactional
    public ChuongTrinhKhuyenMai capNhat(Integer id, ChuongTrinhKhuyenMai request) {
        ChuongTrinhKhuyenMai entity = ctkmRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + id));
        if (request.getTenCtkm()     != null) entity.setTenCtkm(request.getTenCtkm());
        if (request.getMoTa()        != null) entity.setMoTa(request.getMoTa());
        if (request.getGiaTriUuDai() != null) entity.setGiaTriUuDai(request.getGiaTriUuDai());
        if (request.getGiamToiDa()   != null) entity.setGiamToiDa(request.getGiamToiDa());
        if (request.getNgayBatDau()  != null) entity.setNgayBatDau(request.getNgayBatDau());
        if (request.getNgayKetThuc() != null) entity.setNgayKetThuc(request.getNgayKetThuc());
        if (request.getTrangThai()   != null) entity.setTrangThai(request.getTrangThai());
        return ctkmRepo.save(entity);
    }

    // ── ADMIN: Sinh mã giảm giá ──────────────────────────────────────────

    @Override
    @Transactional
    public List<MaGiamGia> sinhMaGiamGia(Integer ctkmId, int soLuong, String prefix) {
        if (soLuong <= 0 || soLuong > 500) {
            throw new IllegalArgumentException("Số lượng mã phải trong khoảng 1 - 500.");
        }
        ChuongTrinhKhuyenMai ctkm = ctkmRepo.findById(ctkmId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình KM ID: " + ctkmId));

        if (!"ma_code".equals(ctkm.getLoai())) {
            throw new IllegalArgumentException(
                    "Chương trình [" + ctkm.getTenCtkm() + "] không phải loại 'ma_code'. " +
                    "Chỉ loại 'ma_code' mới được sinh mã giảm giá.");
        }

        String tiToPrefix = (prefix != null && !prefix.isBlank()) ? prefix.toUpperCase().trim() : "CODE";
        List<MaGiamGia> danhSach = new ArrayList<>(soLuong);

        int attempts = 0;
        while (danhSach.size() < soLuong && attempts < soLuong * 3) {
            attempts++;
            // Sinh mã ngẫu nhiên: PREFIX-XXXXXXXX (8 ký tự hex từ UUID)
            String maCode = tiToPrefix + "-" + UUID.randomUUID().toString()
                    .replace("-", "").substring(0, 8).toUpperCase();

            if (maGiamGiaRepo.existsByMaCode(maCode)) continue; // Trùng → thử lại

            MaGiamGia ma = MaGiamGia.builder()
                    .chuongTrinhKhuyenMai(ctkm)
                    .maCode(maCode)
                    .donHangToiThieu(BigDecimal.ZERO)
                    .soLuongToiDa(1)
                    .daSuDung(0)
                    .build();
            danhSach.add(ma);
        }

        if (danhSach.size() < soLuong) {
            throw new IllegalStateException("Không thể sinh đủ " + soLuong + " mã duy nhất. Vui lòng thử lại.");
        }

        List<MaGiamGia> saved = maGiamGiaRepo.saveAll(danhSach);
        log.info("[KhuyenMai] Sinh {} mã giảm giá cho ctkmId={}", saved.size(), ctkmId);
        return saved;
    }

    // ── PUBLIC: Check mã tại Checkout ────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public BigDecimal checkMa(String maCode, BigDecimal tongTien) {
        if (maCode == null || maCode.isBlank()) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống.");
        }
        if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Tổng tiền đơn hàng phải lớn hơn 0.");
        }

        // Bước 1: Tìm mã (kèm CTKM cha)
        MaGiamGia ma = maGiamGiaRepo.findByMaCodeWithCtkm(maCode.trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Mã giảm giá '" + maCode + "' không tồn tại trong hệ thống."));

        ChuongTrinhKhuyenMai ctkm = ma.getChuongTrinhKhuyenMai();

        // Bước 2: Chương trình phải đang diễn ra
        if (!"dang_dien_ra".equals(ctkm.getTrangThai())) {
            throw new IllegalArgumentException(String.format(
                    "Mã '%s' thuộc chương trình [%s] đang ở trạng thái '%s', không thể sử dụng.",
                    maCode, ctkm.getTenCtkm(), ctkm.getTrangThai()));
        }

        // Bước 3: Chưa hết hạn
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(ctkm.getNgayKetThuc())) {
            throw new IllegalArgumentException(String.format(
                    "Mã '%s' đã hết hạn vào ngày %s.", maCode, ctkm.getNgayKetThuc().toLocalDate()));
        }
        if (now.isBefore(ctkm.getNgayBatDau())) {
            throw new IllegalArgumentException(String.format(
                    "Mã '%s' chưa đến thời gian áp dụng (bắt đầu: %s).",
                    maCode, ctkm.getNgayBatDau().toLocalDate()));
        }

        // Bước 4: Còn lượt dùng
        if (ma.getDaSuDung() >= ma.getSoLuongToiDa()) {
            throw new IllegalArgumentException(
                    "Mã '" + maCode + "' đã hết lượt sử dụng.");
        }

        // Bước 5: Đơn hàng đạt giá trị tối thiểu
        if (tongTien.compareTo(ma.getDonHangToiThieu()) < 0) {
            throw new IllegalArgumentException(String.format(
                    "Đơn hàng chưa đạt giá trị tối thiểu để dùng mã '%s'. " +
                    "Yêu cầu: %,.0f VNĐ | Đơn hiện tại: %,.0f VNĐ.",
                    maCode, ma.getDonHangToiThieu(), tongTien));
        }

        // Tính số tiền được giảm
        BigDecimal soTienGiam = tinhSoTienGiam(ctkm, ma, tongTien);
        log.info("[KhuyenMai] checkMa='{}' | tongTien={} | giảm={}", maCode, tongTien, soTienGiam);
        return soTienGiam;
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────────

    /**
     * Tính số tiền được giảm dựa trên cấu hình CTKM.
     * Đảm bảo số tiền giảm không vượt tổng đơn hàng.
     */
    private BigDecimal tinhSoTienGiam(ChuongTrinhKhuyenMai ctkm,
                                       MaGiamGia ma, BigDecimal tongTien) {
        BigDecimal soTienGiam;

        if (Boolean.TRUE.equals(ctkm.getLaPhanTram())) {
            // Giảm theo phần trăm
            soTienGiam = tongTien
                    .multiply(ctkm.getGiaTriUuDai())
                    .divide(BigDecimal.valueOf(100), 0, RoundingMode.DOWN);
            // Áp dụng giảm tối đa (ưu tiên giamToiDa của mã, fallback về của CTKM)
            BigDecimal giamToiDa = ma.getGiamToiDa() != null ? ma.getGiamToiDa() : ctkm.getGiamToiDa();
            if (giamToiDa != null && soTienGiam.compareTo(giamToiDa) > 0) {
                soTienGiam = giamToiDa;
            }
        } else {
            // Giảm số tiền cố định
            soTienGiam = ctkm.getGiaTriUuDai();
        }

        // Không giảm quá tổng đơn hàng
        if (soTienGiam.compareTo(tongTien) > 0) soTienGiam = tongTien;
        return soTienGiam;
    }

    private void validateCtkmRequest(ChuongTrinhKhuyenMai req) {
        if (req.getTenCtkm() == null || req.getTenCtkm().isBlank()) {
            throw new IllegalArgumentException("Tên chương trình khuyến mãi không được trống.");
        }
        if (req.getLoai() == null || req.getLoai().isBlank()) {
            throw new IllegalArgumentException("Loại khuyến mãi không được trống.");
        }
        if (req.getNgayBatDau() == null || req.getNgayKetThuc() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và kết thúc không được trống.");
        }
        if (req.getNgayBatDau().isAfter(req.getNgayKetThuc())) {
            throw new IllegalArgumentException("Ngày bắt đầu phải trước ngày kết thúc.");
        }
    }
}
