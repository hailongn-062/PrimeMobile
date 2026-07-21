package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.YeuThich;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.repository.YeuThichRepository;
import org.example.primemobile.service.IYeuThichService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class YeuThichServiceImpl implements IYeuThichService {

    private final YeuThichRepository yeuThichRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;

    @Override
    @Transactional
    public boolean toggleYeuThich(Integer khachHangId, Integer sanPhamId) {
        Optional<YeuThich> yeuThichOpt = yeuThichRepository.findByKhachHangIdAndSanPhamId(khachHangId, sanPhamId);
        
        if (yeuThichOpt.isPresent()) {
            // Đã có trong danh sách yêu thích -> Hủy yêu thích
            yeuThichRepository.delete(yeuThichOpt.get());
            return false;
        } else {
            // Chưa có -> Thêm vào danh sách yêu thích
            KhachHang khachHang = khachHangRepository.findById(khachHangId)
                    .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại"));
            SanPham sanPham = sanPhamRepository.findById(sanPhamId)
                    .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại"));

            YeuThich newYeuThich = YeuThich.builder()
                    .khachHang(khachHang)
                    .sanPham(sanPham)
                    .build();
            yeuThichRepository.save(newYeuThich);
            return true;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<YeuThich> layDanhSach(Integer khachHangId) {
        return yeuThichRepository.findByKhachHangIdOrderByNgayThemDesc(khachHangId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean kiemTraDaYeuThich(Integer khachHangId, Integer sanPhamId) {
        return yeuThichRepository.existsByKhachHangIdAndSanPhamId(khachHangId, sanPhamId);
    }
}
