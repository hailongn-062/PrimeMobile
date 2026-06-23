package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.repository.HangSanXuatRepository;
import org.example.primemobile.service.IHangSanXuatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Triển khai phân hệ Quản lý Hãng sản xuất (thương hiệu điện thoại).
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Tên hãng phải duy nhất, không phân biệt hoa thường.</li>
 *   <li>Không xóa vật lý nếu hãng đang có sản phẩm liên kết
 *       (DB constraint sẽ bắt lỗi foreign key).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HangSanXuatServiceImpl implements IHangSanXuatService {

    private final HangSanXuatRepository hangSanXuatRepository;

    // =========================================================================
    // PUBLIC METHODS
    // =========================================================================

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<HangSanXuat> layTatCa() {
        return hangSanXuatRepository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public HangSanXuat layTheoId(Integer id) {
        return hangSanXuatRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy hãng sản xuất có ID: " + id));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     *   <li>Trim và validate tên không rỗng.</li>
     *   <li>Kiểm tra trùng lặp tên (case-insensitive).</li>
     *   <li>Lưu entity với ID do DB sinh.</li>
     * </ol>
     */
    @Override
    @Transactional
    public HangSanXuat them(HangSanXuat hangSanXuat) {
        String ten = hangSanXuat.getTenHang() == null
                ? "" : hangSanXuat.getTenHang().trim();

        if (ten.isBlank()) {
            throw new IllegalArgumentException("Tên hãng sản xuất không được để trống.");
        }

        if (hangSanXuatRepository.existsByTenHangIgnoreCase(ten)) {
            throw new IllegalArgumentException(
                    "Hãng sản xuất \"" + ten + "\" đã tồn tại. Vui lòng chọn tên khác.");
        }

        hangSanXuat.setTenHang(ten);
        hangSanXuat.setId(null); // Đảm bảo INSERT, không UPDATE nhầm

        HangSanXuat saved = hangSanXuatRepository.save(hangSanXuat);
        log.info("[HangSanXuat] Đã thêm hãng mới — id={}, ten={}", saved.getId(), saved.getTenHang());
        return saved;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Luồng xử lý:
     * <ol>
     *   <li>Lấy entity hiện tại (ném {@link EntityNotFoundException} nếu không có).</li>
     *   <li>Validate tên mới không trùng với hãng KHÁC.</li>
     *   <li>Ghi đè các trường: {@code tenHang}, {@code logo}, {@code quocGia}.</li>
     * </ol>
     */
    @Override
    @Transactional
    public HangSanXuat capNhat(Integer id, HangSanXuat hangSanXuatMoi) {
        HangSanXuat existing = layTheoId(id);

        String tenMoi = hangSanXuatMoi.getTenHang() == null
                ? "" : hangSanXuatMoi.getTenHang().trim();

        if (tenMoi.isBlank()) {
            throw new IllegalArgumentException("Tên hãng sản xuất không được để trống.");
        }

        // Guard: tên mới có trùng với hãng KHÁC không?
        if (hangSanXuatRepository.existsByTenHangIgnoreCaseAndIdNot(tenMoi, id)) {
            throw new IllegalArgumentException(
                    "Hãng sản xuất \"" + tenMoi + "\" đã tồn tại. Vui lòng chọn tên khác.");
        }

        // Ghi đè các trường được phép cập nhật
        existing.setTenHang(tenMoi);
        existing.setLogo(hangSanXuatMoi.getLogo());
        existing.setQuocGia(hangSanXuatMoi.getQuocGia());

        HangSanXuat updated = hangSanXuatRepository.save(existing);
        log.info("[HangSanXuat] Đã cập nhật hãng — id={}, ten={}", updated.getId(), updated.getTenHang());
        return updated;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Xóa vật lý. Nếu hãng đang có SanPham liên kết,
     * DB sẽ ném lỗi foreign key constraint và transaction sẽ rollback tự động.
     */
    @Override
    @Transactional
    public void xoa(Integer id) {
        HangSanXuat hang = layTheoId(id);
        hangSanXuatRepository.delete(hang);
        log.info("[HangSanXuat] Đã xóa hãng — id={}, ten={}", id, hang.getTenHang());
    }
}
