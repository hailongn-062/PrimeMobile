package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.HinhAnhSanPham;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.HinhAnhSanPhamRepository;
import org.example.primemobile.service.IHinhAnhSanPhamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HinhAnhSanPhamServiceImpl implements IHinhAnhSanPhamService {

    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    @Override
    public List<HinhAnhSanPham> layTheoBienThe(Integer bienTheId) {
        return hinhAnhSanPhamRepository.findByBienTheSanPhamIdOrderByThuTuAsc(bienTheId);
    }

    @Override
    @Transactional
    public HinhAnhSanPham themAnh(Integer bienTheId, HinhAnhSanPham hinhAnh) {
        BienTheSanPham bienThe = bienTheSanPhamRepository.findById(bienTheId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể: " + bienTheId));

        hinhAnh.setBienTheSanPham(bienThe);
        
        // Nếu là ảnh đầu tiên thì auto set làm ảnh chính
        List<HinhAnhSanPham> existing = hinhAnhSanPhamRepository.findByBienTheSanPhamIdOrderByThuTuAsc(bienTheId);
        if (existing.isEmpty()) {
            hinhAnh.setLaAnhChinh(true);
        } else if (hinhAnh.getLaAnhChinh() != null && hinhAnh.getLaAnhChinh()) {
            // Nếu muốn thêm ảnh và ép nó làm ảnh chính ngay từ đầu
            hinhAnhSanPhamRepository.resetAnhChinhByBienTheId(bienTheId);
        } else {
            hinhAnh.setLaAnhChinh(false);
        }
        
        return hinhAnhSanPhamRepository.save(hinhAnh);
    }

    @Override
    @Transactional
    public void xoaAnh(Integer id) {
        if (!hinhAnhSanPhamRepository.existsById(id)) {
            throw new EntityNotFoundException("Không tìm thấy ảnh: " + id);
        }
        hinhAnhSanPhamRepository.deleteById(id);
    }

    @Override
    @Transactional
    public void datLamAnhChinh(Integer id) {
        HinhAnhSanPham hinhAnh = hinhAnhSanPhamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy ảnh: " + id));

        // Reset tất cả các ảnh khác của biến thể này về false
        hinhAnhSanPhamRepository.resetAnhChinhByBienTheId(hinhAnh.getBienTheSanPham().getId());

        // Set ảnh này làm ảnh chính
        hinhAnh.setLaAnhChinh(true);
        hinhAnhSanPhamRepository.save(hinhAnh);
    }
}
