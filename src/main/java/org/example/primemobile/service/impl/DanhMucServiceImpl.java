package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.repository.DanhMucRepository;
import org.example.primemobile.service.IDanhMucService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Triển khai phân hệ Quản lý Danh mục sản phẩm.
 * <p>
 * Đặc điểm nổi bật:
 * <ul>
 *   <li><b>Auto-slug:</b> Slug URL-friendly được tự động sinh từ {@code tenDanhMuc}
 *       bằng cách chuẩn hóa Unicode → loại dấu → thay khoảng trắng bằng {@code '-'}.</li>
 *   <li><b>Soft-delete:</b> {@link #voHieuHoa(Integer)} chỉ đặt {@code kichHoat = false},
 *       không xóa vật lý để bảo toàn tham chiếu bảng {@code san_pham}.</li>
 *   <li><b>Duplicate guard:</b> Kiểm tra tên trùng lặp không phân biệt hoa thường
 *       trước mọi thao tác INSERT / UPDATE.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DanhMucServiceImpl implements IDanhMucService {

    private final DanhMucRepository danhMucRepository;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<DanhMuc> layTatCa() {
        return danhMucRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<DanhMuc> layDanhSachKichHoat() {
        return danhMucRepository.findAll().stream()
                .filter(DanhMuc::getKichHoat)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public DanhMuc layTheoId(Integer id) {
        return danhMucRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy danh mục có ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     *   <li>Validate tên không trùng lặp.</li>
     *   <li>Sinh slug từ {@code tenDanhMuc}.</li>
     *   <li>Kiểm tra slug chưa trùng (trường hợp hai tên khác nhau nhưng slug giống nhau).</li>
     *   <li>Lưu entity.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DanhMuc them(DanhMuc danhMuc) {
        String ten = danhMuc.getTenDanhMuc() == null ? "" : danhMuc.getTenDanhMuc().trim();

        // Guard: tên không được rỗng
        if (ten.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }

        // Guard: tên trùng lặp
        if (danhMucRepository.existsByTenDanhMucIgnoreCase(ten)) {
            throw new IllegalArgumentException(
                    "Danh mục \"" + ten + "\" đã tồn tại. Vui lòng chọn tên khác.");
        }

        // Sinh slug và kiểm tra trùng slug
        String slug = taoSlug(ten);
        if (danhMucRepository.existsBySlug(slug)) {
            slug = slug + "-" + System.currentTimeMillis() % 10000;
            log.warn("[DanhMuc] Slug trùng lặp, thêm hậu tố: {}", slug);
        }

        danhMuc.setTenDanhMuc(ten);
        danhMuc.setSlug(slug);
        danhMuc.setId(null); // Đảm bảo INSERT, không UPDATE nhầm

        DanhMuc saved = danhMucRepository.save(danhMuc);
        log.info("[DanhMuc] Đã thêm danh mục mới — id={}, ten={}, slug={}",
                saved.getId(), saved.getTenDanhMuc(), saved.getSlug());
        return saved;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     *   <li>Lấy entity hiện tại (ném {@link EntityNotFoundException} nếu không có).</li>
     *   <li>Validate tên mới không trùng với danh mục KHÁC.</li>
     *   <li>Cập nhật slug nếu tên thay đổi.</li>
     *   <li>Ghi đè các trường được phép cập nhật.</li>
     * </ol>
     */
    @Override
    @Transactional
    public DanhMuc capNhat(Integer id, DanhMuc danhMucMoi) {
        DanhMuc existing = layTheoId(id);

        String tenMoi = danhMucMoi.getTenDanhMuc() == null
                ? "" : danhMucMoi.getTenDanhMuc().trim();

        if (tenMoi.isBlank()) {
            throw new IllegalArgumentException("Tên danh mục không được để trống.");
        }

        // Guard: tên mới có trùng với danh mục KHÁC không?
        if (danhMucRepository.existsByTenDanhMucIgnoreCaseAndIdNot(tenMoi, id)) {
            throw new IllegalArgumentException(
                    "Danh mục \"" + tenMoi + "\" đã tồn tại. Vui lòng chọn tên khác.");
        }

        // Sinh slug mới nếu tên thay đổi
        String slugMoi = taoSlug(tenMoi);
        if (danhMucRepository.existsBySlugAndIdNot(slugMoi, id)) {
            slugMoi = slugMoi + "-" + System.currentTimeMillis() % 10000;
            log.warn("[DanhMuc] Slug trùng lặp khi cập nhật, thêm hậu tố: {}", slugMoi);
        }

        // Ghi đè các trường được phép cập nhật
        existing.setTenDanhMuc(tenMoi);
        existing.setSlug(slugMoi);
        existing.setMoTa(danhMucMoi.getMoTa());
        if (danhMucMoi.getThuTu() != null) {
            existing.setThuTu(danhMucMoi.getThuTu());
        }
        if (danhMucMoi.getKichHoat() != null) {
            existing.setKichHoat(danhMucMoi.getKichHoat());
        }

        DanhMuc updated = danhMucRepository.save(existing);
        log.info("[DanhMuc] Đã cập nhật danh mục — id={}, ten={}, slug={}",
                updated.getId(), updated.getTenDanhMuc(), updated.getSlug());
        return updated;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void voHieuHoa(Integer id) {
        DanhMuc danhMuc = layTheoId(id);
        danhMuc.setKichHoat(false);
        danhMucRepository.save(danhMuc);
        log.info("[DanhMuc] Đã vô hiệu hóa danh mục — id={}, ten={}",
                id, danhMuc.getTenDanhMuc());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void kichHoatLai(Integer id) {
        DanhMuc danhMuc = layTheoId(id);
        danhMuc.setKichHoat(true);
        danhMucRepository.save(danhMuc);
        log.info("[DanhMuc] Đã kích hoạt lại danh mục — id={}, ten={}",
                id, danhMuc.getTenDanhMuc());
    }

    // =========================================================================
    // PRIVATE HELPER
    // =========================================================================

    /**
     * Tạo slug URL-friendly từ chuỗi tiếng Việt.
     * <p>
     * Ví dụ: {@code "Điện Thoại Android"} → {@code "dien-thoai-android"}.
     * <p>
     * Thuật toán:
     * <ol>
     *   <li>Chuẩn hóa Unicode sang dạng NFD (tách ký tự và dấu).</li>
     *   <li>Xóa toàn bộ dấu phụ (combining marks, pattern {@code \p{M}}).</li>
     *   <li>Chuyển về chữ thường.</li>
     *   <li>Thay khoảng trắng và ký tự đặc biệt bằng {@code '-'}.</li>
     *   <li>Loại bỏ các dấu {@code '-'} ở đầu/cuối.</li>
     * </ol>
     *
     * @param input Chuỗi đầu vào (tên danh mục).
     * @return Slug URL-friendly, chữ thường, không dấu.
     */
    private static final Pattern NON_LATIN     = Pattern.compile("[^\\w-]");
    private static final Pattern MULTI_DASH    = Pattern.compile("-+");

    private String taoSlug(String input) {
        if (input == null || input.isBlank()) return "";
        // 1. Chuẩn hóa NFD và loại dấu
        String normalized = Normalizer.normalize(input.trim(), Normalizer.Form.NFD);
        String withoutAccents = normalized.replaceAll("\\p{M}", "");
        // 2. Chuyển 'đ' / 'Đ' thủ công (không bị loại bởi NFD)
        withoutAccents = withoutAccents.replace("đ", "d").replace("Đ", "D");
        // 3. Chữ thường, thay khoảng trắng bằng '-'
        String lower = withoutAccents.toLowerCase().replace(" ", "-");
        // 4. Xóa ký tự không phải latin/số/dấu gạch
        String clean = NON_LATIN.matcher(lower).replaceAll("");
        // 5. Thu gọn nhiều dấu '-' liên tiếp
        String slug  = MULTI_DASH.matcher(clean).replaceAll("-");
        // 6. Trim đầu cuối
        return slug.replaceAll("^-|-$", "");
    }
}
