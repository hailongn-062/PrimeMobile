package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.Kho;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.TonKho;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.KhoRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * Triển khai đầy đủ phân hệ Quản lý Biến thể Sản phẩm (SKU).
 *
 * <h2>Luật Nghiệp Vụ Quan Trọng — Khởi Tạo Tồn Kho Tự Động:</h2>
 * <p>
 * Khi một biến thể mới được thêm thành công, hệ thống PHẢI tự động tạo
 * bản ghi {@link TonKho} với {@code soLuong = 0} tại TẤT CẢ kho đang
 * hoạt động ({@code kichHoat = true}). Toàn bộ thao tác (lưu biến thể +
 * khởi tạo tồn kho) được bao trong một {@code @Transactional} duy nhất —
 * nếu tạo tồn kho thất bại, toàn bộ rollback.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BienTheSanPhamServiceImpl implements IBienTheSanPhamService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ TRẠNG THÁI (khớp CHECK constraint chk_bt_trang_thai trong DB)
    // -----------------------------------------------------------------------
    private static final Set<String> TRANG_THAI_HOP_LE =
            Set.of("con_hang", "het_hang", "ngung_kinh_doanh");

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final SanPhamRepository        sanPhamRepository;
    private final KhoRepository            khoRepository;
    private final TonKhoRepository         tonKhoRepository;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public BienTheSanPham getBienTheSanPham(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy biến thể sản phẩm có ID: " + id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<BienTheSanPham> layTheoSanPhamId(Integer sanPhamId) {
        // Validate sản phẩm tồn tại trước khi lấy biến thể
        if (!sanPhamRepository.existsById(sanPhamId)) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm có ID: " + sanPhamId);
        }
        return bienTheSanPhamRepository.findBySanPhamId(sanPhamId);
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng chi tiết (trong 1 Transaction):</h3>
     * <ol>
     *   <li>Validate sản phẩm cha tồn tại.</li>
     *   <li>Validate {@code maSku} không trùng.</li>
     *   <li>Lưu biến thể vào DB.</li>
     *   <li><b>[LUẬT BẮT BUỘC]</b> Tìm tất cả kho đang hoạt động →
     *       tạo bản ghi {@code TonKho(soLuong=0)} cho biến thể tại mỗi kho.</li>
     * </ol>
     */
    @Override
    @Transactional
    public BienTheSanPham them(Integer sanPhamId, BienTheSanPham bienTheSanPham) {
        // Bước 1: Validate và lấy sản phẩm cha
        SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy sản phẩm có ID: " + sanPhamId));

        // Bước 2: Validate mã SKU
        validateMaSku(bienTheSanPham.getMaSku(), null);



        // Bước 4: Gán sản phẩm cha và giá trị mặc định, lưu biến thể
        bienTheSanPham.setSanPham(sanPham);
        bienTheSanPham.setId(null);
        bienTheSanPham.setNgayTao(LocalDateTime.now());
        bienTheSanPham.setUpdatedAt(LocalDateTime.now());
        if (bienTheSanPham.getTrangThai() == null || !TRANG_THAI_HOP_LE.contains(bienTheSanPham.getTrangThai())) {
            bienTheSanPham.setTrangThai("con_hang");
        }
        if (bienTheSanPham.getLoaiLuuTru() == null || bienTheSanPham.getLoaiLuuTru().isBlank()) {
            bienTheSanPham.setLoaiLuuTru("UFS");
        }

        BienTheSanPham saved = bienTheSanPhamRepository.save(bienTheSanPham);
        log.info("[BienTheSanPham] Đã lưu biến thể — id={}, maSku={}, sanPhamId={}",
                saved.getId(), saved.getMaSku(), sanPhamId);

        // ====================================================================
        // Bước 5: LUẬT BẮT BUỘC — Khởi tạo Tồn kho tại tất cả kho hoạt động
        // ====================================================================
        List<Kho> tatCaKhoHoatDong = khoRepository.findAll().stream()
                .filter(Kho::getKichHoat)
                .toList();

        if (tatCaKhoHoatDong.isEmpty()) {
            log.warn("[BienTheSanPham] Không tìm thấy kho nào đang hoạt động. " +
                     "Tồn kho chưa được khởi tạo cho SKU: {}", saved.getMaSku());
        } else {
            LocalDateTime now = LocalDateTime.now();
            for (Kho kho : tatCaKhoHoatDong) {
                TonKho tonKho = TonKho.builder()
                        .kho(kho)
                        .bienTheSanPham(saved)
                        .soLuong(0)
                        .updatedAt(now)
                        .build();
                tonKhoRepository.save(tonKho);
                log.debug("[BienTheSanPham] Khởi tạo tồn kho — kho={}, sku={}, soLuong=0",
                        kho.getTenKho(), saved.getMaSku());
            }
            log.info("[BienTheSanPham] Đã khởi tạo tồn kho tại {} kho cho SKU: {}",
                    tatCaKhoHoatDong.size(), saved.getMaSku());
        }
        // ====================================================================

        return saved;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Các trường được phép cập nhật: {@code maSku}, {@code mauSac},
     * {@code maMauHex}, {@code ramGb}, {@code luuTruGb}, {@code loaiLuuTru},
     * {@code giaNhap}, {@code giaBan}, {@code giaKhuyenMai}, {@code trongLuongGram}, {@code pinMah}.
     */
    @Override
    @Transactional
    public BienTheSanPham capNhat(Integer id, BienTheSanPham bienTheMoi) {
        BienTheSanPham existing = getBienTheSanPham(id);

        // Validate mã SKU mới (loại trừ chính nó)
        validateMaSku(bienTheMoi.getMaSku(), id);
        existing.setMaSku(bienTheMoi.getMaSku().trim().toUpperCase());



        // Ghi đè các trường thông tin biến thể
        if (bienTheMoi.getMauSac() != null && !bienTheMoi.getMauSac().isBlank()) {
            existing.setMauSac(bienTheMoi.getMauSac().trim());
        }
        existing.setMaMauHex(bienTheMoi.getMaMauHex());
        if (bienTheMoi.getRamGb() != null)          existing.setRamGb(bienTheMoi.getRamGb());
        if (bienTheMoi.getLuuTruGb() != null)        existing.setLuuTruGb(bienTheMoi.getLuuTruGb());
        if (bienTheMoi.getLoaiLuuTru() != null && !bienTheMoi.getLoaiLuuTru().isBlank()) {
            existing.setLoaiLuuTru(bienTheMoi.getLoaiLuuTru());
        }
        if (bienTheMoi.getGiaNhap() != null)         existing.setGiaNhap(bienTheMoi.getGiaNhap());
        if (bienTheMoi.getGiaBan() != null)           existing.setGiaBan(bienTheMoi.getGiaBan());
        existing.setGiaKhuyenMai(bienTheMoi.getGiaKhuyenMai()); // Cho phép set null (xóa KM)
        existing.setTrongLuongGram(bienTheMoi.getTrongLuongGram());
        existing.setPinMah(bienTheMoi.getPinMah());
        existing.setUpdatedAt(LocalDateTime.now());

        BienTheSanPham updated = bienTheSanPhamRepository.save(existing);
        log.info("[BienTheSanPham] Đã cập nhật biến thể — id={}, maSku={}", updated.getId(), updated.getMaSku());
        return updated;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BienTheSanPham doiTrangThai(Integer id, String trangThai) {
        if (trangThai == null || !TRANG_THAI_HOP_LE.contains(trangThai)) {
            throw new IllegalArgumentException(
                    "Trạng thái biến thể không hợp lệ: \"" + trangThai + "\". " +
                    "Giá trị hợp lệ: " + TRANG_THAI_HOP_LE);
        }
        BienTheSanPham bienThe = getBienTheSanPham(id);
        bienThe.setTrangThai(trangThai);
        bienThe.setUpdatedAt(LocalDateTime.now());
        BienTheSanPham updated = bienTheSanPhamRepository.save(bienThe);
        log.info("[BienTheSanPham] Đổi trạng thái — id={}, trangThai={}", id, trangThai);
        return updated;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /** Validate mã SKU không rỗng và không trùng. {@code excludeId} null = thêm mới. */
    private void validateMaSku(String maSku, Integer excludeId) {
        if (maSku == null || maSku.isBlank()) {
            throw new IllegalArgumentException("Mã SKU không được để trống.");
        }
        String maSkuTrim = maSku.trim().toUpperCase();
        boolean trung = (excludeId == null)
                ? bienTheSanPhamRepository.existsByMaSku(maSkuTrim)
                : bienTheSanPhamRepository.existsByMaSkuAndIdNot(maSkuTrim, excludeId);
        if (trung) {
            throw new IllegalArgumentException(
                    "Mã SKU \"" + maSkuTrim + "\" đã tồn tại. Vui lòng dùng mã SKU khác.");
        }
    }


}
