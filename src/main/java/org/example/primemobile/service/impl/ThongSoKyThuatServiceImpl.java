package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.ThongSoKyThuat;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.repository.ThongSoKyThuatRepository;
import org.example.primemobile.service.IThongSoKyThuatService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThongSoKyThuatServiceImpl implements IThongSoKyThuatService {

    private final ThongSoKyThuatRepository thongSoRepo;
    private final SanPhamRepository        sanPhamRepo;

    @Override
    @Transactional(readOnly = true)
    public List<ThongSoKyThuat> layTheoSanPham(Integer sanPhamId) {
        validateSanPhamTonTai(sanPhamId);
        return thongSoRepo.findBySanPhamIdOrderByNhomAscThuTuAsc(sanPhamId);
    }

    @Override
    @Transactional
    public ThongSoKyThuat them(Integer sanPhamId, ThongSoKyThuat request) {
        SanPham sanPham = layHoacNemLoi(sanPhamId);
        ThongSoKyThuat entity = ThongSoKyThuat.builder()
                .sanPham(sanPham)
                .nhom(request.getNhom() != null ? request.getNhom() : "Thông tin chung")
                .tenThongSo(request.getTenThongSo())
                .giaTri(request.getGiaTri())
                .thuTu(request.getThuTu() != null ? request.getThuTu() : 0)
                .build();
        ThongSoKyThuat saved = thongSoRepo.save(entity);
        log.info("[ThongSoKyThuat] Thêm thành công — sanPhamId={}, id={}", sanPhamId, saved.getId());
        return saved;
    }

    @Override
    @Transactional
    public ThongSoKyThuat sua(Integer id, ThongSoKyThuat request) {
        ThongSoKyThuat entity = thongSoRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy thông số kỹ thuật ID: " + id));
        if (request.getNhom()       != null) entity.setNhom(request.getNhom());
        if (request.getTenThongSo() != null) entity.setTenThongSo(request.getTenThongSo());
        if (request.getGiaTri()     != null) entity.setGiaTri(request.getGiaTri());
        if (request.getThuTu()      != null) entity.setThuTu(request.getThuTu());
        log.info("[ThongSoKyThuat] Sửa thành công — id={}", id);
        return thongSoRepo.save(entity);
    }

    @Override
    @Transactional
    public void xoa(Integer id) {
        if (!thongSoRepo.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy thông số kỹ thuật ID: " + id);
        }
        thongSoRepo.deleteById(id);
        log.info("[ThongSoKyThuat] Xóa thành công — id={}", id);
    }

    @Override
    @Transactional
    public List<ThongSoKyThuat> capNhatToanBo(Integer sanPhamId, List<ThongSoKyThuat> danhSach) {
        SanPham sanPham = layHoacNemLoi(sanPhamId);
        if (danhSach == null || danhSach.isEmpty()) {
            throw new IllegalArgumentException("Danh sách thông số không được rỗng.");
        }
        // Xóa toàn bộ cũ trong 1 câu DELETE
        thongSoRepo.deleteBySanPhamId(sanPhamId);

        // Gán sanPham và lưu mới
        List<ThongSoKyThuat> danhSachMoi = danhSach.stream()
                .map(req -> ThongSoKyThuat.builder()
                        .sanPham(sanPham)
                        .nhom(req.getNhom() != null ? req.getNhom() : "Thông tin chung")
                        .tenThongSo(req.getTenThongSo())
                        .giaTri(req.getGiaTri())
                        .thuTu(req.getThuTu() != null ? req.getThuTu() : 0)
                        .build())
                .toList();

        List<ThongSoKyThuat> saved = thongSoRepo.saveAll(danhSachMoi);
        log.info("[ThongSoKyThuat] CapNhatToanBo — sanPhamId={}, {} dòng", sanPhamId, saved.size());
        return saved;
    }

    // ── HELPERS ─────────────────────────────────────────────────────────

    private SanPham layHoacNemLoi(Integer sanPhamId) {
        return sanPhamRepo.findById(sanPhamId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy sản phẩm ID: " + sanPhamId));
    }

    private void validateSanPhamTonTai(Integer sanPhamId) {
        if (!sanPhamRepo.existsById(sanPhamId)) {
            throw new EntityNotFoundException("Không tìm thấy sản phẩm ID: " + sanPhamId);
        }
    }
}
