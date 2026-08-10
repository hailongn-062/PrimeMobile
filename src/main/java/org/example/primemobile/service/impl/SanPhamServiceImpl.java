package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.DanhMucRepository;
import org.example.primemobile.repository.HangSanXuatRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * Triển khai phân hệ Quản lý Sản phẩm (Model điện thoại).
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Mã sản phẩm ({@code maSanPham}) phải duy nhất toàn hệ thống.</li>
 *   <li>Không xóa vật lý — chỉ đổi trạng thái {@code "ngung_ban"} để bảo toàn lịch sử.</li>
 *   <li>Phân trang + lọc kết hợp theo {@code danhMucId} và/hoặc {@code hangSanXuatId}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SanPhamServiceImpl implements ISanPhamService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ TRẠNG THÁI (khớp CHECK constraint chk_sp_trang_thai trong DB)
    // -----------------------------------------------------------------------
    private static final Set<String> TRANG_THAI_HOP_LE =
            Set.of("dang_ban", "ngung_ban", "sap_ra_mat");

    // -----------------------------------------------------------------------
    // DEPENDENCIES
    // -----------------------------------------------------------------------
    private final SanPhamRepository      sanPhamRepository;
    private final DanhMucRepository      danhMucRepository;
    private final HangSanXuatRepository  hangSanXuatRepository;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<SanPham> layDanhSach(String tuKhoa, Integer danhMucId, Integer hangSanXuatId, Pageable pageable) {
        return sanPhamRepository.timKiemVaLocSanPham(tuKhoa, danhMucId, hangSanXuatId, pageable);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Chỉ trả sản phẩm {@code trangThai = 'dang_ban'} — dùng cho trang public Frontend.
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SanPham> layDanhSachCongKhai(String tuKhoa, Integer danhMucId, Integer hangSanXuatId, Pageable pageable) {
        return sanPhamRepository.timKiemSanPhamPublic(tuKhoa, danhMucId, hangSanXuatId, pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public SanPham layTheoId(Integer id) {
        return sanPhamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy sản phẩm có ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng:
     * <ol>
     *   <li>Validate mã sản phẩm không trùng.</li>
     *   <li>Resolve DanhMuc và HangSanXuat từ ID trong nested object.</li>
     *   <li>Lưu SanPham với các giá trị mặc định hợp lệ.</li>
     * </ol>
     */
    @Override
    @Transactional
    public SanPham them(SanPham sanPham) {
        // Validate mã sản phẩm
        String ma = validateMaSanPham(sanPham.getMaSanPham(), null);
        sanPham.setMaSanPham(ma);

        // Resolve DanhMuc
        DanhMuc danhMuc = resolveDanhMuc(sanPham);
        sanPham.setDanhMuc(danhMuc);

        // Resolve HangSanXuat
        HangSanXuat hang = resolveHangSanXuat(sanPham);
        sanPham.setHangSanXuat(hang);

        // Đặt giá trị mặc định an toàn
        sanPham.setId(null);
        sanPham.setLuotXem(0);
        sanPham.setNgayTao(LocalDateTime.now());
        sanPham.setUpdatedAt(LocalDateTime.now());
        if (sanPham.getTrangThai() == null || !TRANG_THAI_HOP_LE.contains(sanPham.getTrangThai())) {
            sanPham.setTrangThai("dang_ban");
        }


        SanPham saved = sanPhamRepository.save(sanPham);
        log.info("[SanPham] Đã thêm sản phẩm mới — id={}, ma={}, ten={}",
                saved.getId(), saved.getMaSanPham(), saved.getTenSanPham());
        return saved;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng:
     * <ol>
     *   <li>Load entity hiện tại.</li>
     *   <li>Validate mã sản phẩm mới không trùng với sản phẩm KHÁC.</li>
     *   <li>Resolve DanhMuc / HangSanXuat nếu client truyền ID mới.</li>
     *   <li>Ghi đè các trường được phép cập nhật.</li>
     * </ol>
     */
    @Override
    @Transactional
    public SanPham capNhat(Integer id, SanPham sanPhamMoi) {
        SanPham existing = layTheoId(id);

        // Validate mã sản phẩm mới (loại trừ chính nó)
        String maMoi = validateMaSanPham(sanPhamMoi.getMaSanPham(), id);
        existing.setMaSanPham(maMoi);

        // Cập nhật thông tin mô tả
        if (sanPhamMoi.getTenSanPham() != null && !sanPhamMoi.getTenSanPham().isBlank()) {
            existing.setTenSanPham(sanPhamMoi.getTenSanPham().trim());
        }
        existing.setMoTaNgan(sanPhamMoi.getMoTaNgan());
        existing.setMoTaChiTiet(sanPhamMoi.getMoTaChiTiet());
        existing.setNamRaMat(sanPhamMoi.getNamRaMat());


        // Đổi danh mục nếu client truyền ID mới
        if (sanPhamMoi.getDanhMuc() != null && sanPhamMoi.getDanhMuc().getId() != null) {
            DanhMuc danhMucMoi = danhMucRepository.findById(sanPhamMoi.getDanhMuc().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy danh mục ID: " + sanPhamMoi.getDanhMuc().getId()));
            existing.setDanhMuc(danhMucMoi);
        }

        // Đổi hãng nếu client truyền ID mới
        if (sanPhamMoi.getHangSanXuat() != null && sanPhamMoi.getHangSanXuat().getId() != null) {
            HangSanXuat hangMoi = hangSanXuatRepository.findById(sanPhamMoi.getHangSanXuat().getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy hãng sản xuất ID: " + sanPhamMoi.getHangSanXuat().getId()));
            existing.setHangSanXuat(hangMoi);
        }

        existing.setUpdatedAt(LocalDateTime.now());

        SanPham updated = sanPhamRepository.save(existing);
        log.info("[SanPham] Đã cập nhật sản phẩm — id={}, ma={}", updated.getId(), updated.getMaSanPham());
        return updated;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public SanPham doiTrangThai(Integer id, String trangThai) {
        if (trangThai == null || !TRANG_THAI_HOP_LE.contains(trangThai)) {
            throw new IllegalArgumentException(
                    "Trạng thái không hợp lệ: \"" + trangThai + "\". " +
                    "Giá trị hợp lệ: " + TRANG_THAI_HOP_LE);
        }
        SanPham sanPham = layTheoId(id);
        sanPham.setTrangThai(trangThai);
        sanPham.setUpdatedAt(LocalDateTime.now());
        SanPham updated = sanPhamRepository.save(sanPham);
        log.info("[SanPham] Đổi trạng thái — id={}, trangThai={}", id, trangThai);
        return updated;
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Validate và chuẩn hóa mã sản phẩm.
     * Nếu {@code excludeId != null}, bỏ qua chính sản phẩm đó khi kiểm tra trùng.
     */
    private String validateMaSanPham(String ma, Integer excludeId) {
        if (ma == null || ma.isBlank()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống.");
        }
        String maTrim = ma.trim().toUpperCase();
        boolean trung = (excludeId == null)
                ? sanPhamRepository.existsByMaSanPham(maTrim)
                : sanPhamRepository.existsByMaSanPhamAndIdNot(maTrim, excludeId);
        if (trung) {
            throw new IllegalArgumentException(
                    "Mã sản phẩm \"" + maTrim + "\" đã tồn tại. Vui lòng dùng mã khác.");
        }
        return maTrim;
    }

    /** Resolve DanhMuc từ nested ID trong request body. */
    private DanhMuc resolveDanhMuc(SanPham sanPham) {
        if (sanPham.getDanhMuc() == null || sanPham.getDanhMuc().getId() == null) {
            throw new IllegalArgumentException("Phải cung cấp danh mục cho sản phẩm (danhMuc.id).");
        }
        return danhMucRepository.findById(sanPham.getDanhMuc().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy danh mục ID: " + sanPham.getDanhMuc().getId()));
    }

    /** Resolve HangSanXuat từ nested ID trong request body. */
    private HangSanXuat resolveHangSanXuat(SanPham sanPham) {
        if (sanPham.getHangSanXuat() == null || sanPham.getHangSanXuat().getId() == null) {
            throw new IllegalArgumentException("Phải cung cấp hãng sản xuất cho sản phẩm (hangSanXuat.id).");
        }
        return hangSanXuatRepository.findById(sanPham.getHangSanXuat().getId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy hãng sản xuất ID: " + sanPham.getHangSanXuat().getId()));
    }
}
