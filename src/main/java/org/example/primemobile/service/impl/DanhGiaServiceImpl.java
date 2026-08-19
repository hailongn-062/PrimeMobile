package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.DanhGiaSanPham;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.DanhGiaSanPhamRepository;
import org.example.primemobile.repository.DonHangRepository;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.service.IDanhGiaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DanhGiaServiceImpl implements IDanhGiaService {

    private final DanhGiaSanPhamRepository danhGiaRepository;
    private final DonHangRepository donHangRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;

    @Override
    @Transactional(readOnly = true)
    public KiemTraDieuKienDto kiemTraDieuKienDanhGia(Integer khachHangId, Integer sanPhamId) {
        // Find all orders of this customer
        List<DonHang> orders = donHangRepository.findByKhachHangIdOrderByNgayDatDesc(khachHangId);
        
        for (DonHang order : orders) {
            // Only 'da_hoan_thanh' orders are eligible
            if (!"da_hoan_thanh".equals(order.getTrangThai())) {
                continue;
            }
            // Check if the order contains the product
            boolean hasProduct = order.getChiTietDonHangs().stream()
                    .anyMatch(ct -> ct.getBienTheSanPham() != null && ct.getBienTheSanPham().getSanPham().getId().equals(sanPhamId));
            
            if (hasProduct) {
                // Check if already reviewed
                boolean alreadyReviewed = danhGiaRepository.existsByKhachHangIdAndDonHangIdAndSanPhamId(khachHangId, order.getId(), sanPhamId);
                if (!alreadyReviewed) {
                    return new KiemTraDieuKienDto(true, order.getId(), "Hợp lệ");
                }
            }
        }
        
        return new KiemTraDieuKienDto(false, null, "Bạn chưa mua sản phẩm này, đơn hàng chưa hoàn tất, hoặc đã đánh giá hết số lượt mua.");
    }

    @Override
    @Transactional
    public DanhGiaSanPham taoDanhGia(Integer khachHangId, DanhGiaRequest request) {
        if (request.sao() < 1 || request.sao() > 5) {
            throw new IllegalArgumentException("Số sao đánh giá phải từ 1 đến 5.");
        }

        // 1. Kiểm tra đơn hàng thuộc về khách hàng và trạng thái đã hoàn thành
        DonHang donHang = donHangRepository.findById(request.donHangId())
                .orElseThrow(() -> new IllegalArgumentException("Đơn hàng không tồn tại."));
                
        if (donHang.getKhachHang() == null || !donHang.getKhachHang().getId().equals(khachHangId)) {
            throw new IllegalArgumentException("Đơn hàng không thuộc về tài khoản này.");
        }
        if (!"da_hoan_thanh".equals(donHang.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ có thể đánh giá sản phẩm trong đơn hàng đã hoàn thành.");
        }

        // 2. Kiểm tra đơn hàng có chứa sản phẩm này không
        boolean sanPhamTrongDon = donHang.getChiTietDonHangs().stream()
                .anyMatch(ct -> ct.getBienTheSanPham().getSanPham().getId().equals(request.sanPhamId()));
        if (!sanPhamTrongDon) {
            throw new IllegalArgumentException("Sản phẩm không có trong đơn hàng này.");
        }

        // 3. Kiểm tra xem đã đánh giá sản phẩm này trong đơn hàng này chưa (Unique KhachHang, DonHang, SanPham)
        if (danhGiaRepository.existsByKhachHangIdAndDonHangIdAndSanPhamId(khachHangId, request.donHangId(), request.sanPhamId())) {
            throw new IllegalArgumentException("Bạn đã đánh giá sản phẩm này trong đơn hàng này rồi.");
        }

        // 4. Lấy thực thể Khách hàng & Sản phẩm
        KhachHang khachHang = khachHangRepository.findById(khachHangId)
                .orElseThrow(() -> new IllegalArgumentException("Khách hàng không tồn tại."));
        SanPham sanPham = sanPhamRepository.findById(request.sanPhamId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không tồn tại."));

        // 5. Tạo đánh giá (mặc định cho_duyet)
        DanhGiaSanPham newDanhGia = DanhGiaSanPham.builder()
                .khachHang(khachHang)
                .donHang(donHang)
                .sanPham(sanPham)
                .sao(request.sao())
                .tieuDe(request.tieuDe())
                .noiDung(request.noiDung())
                .hinhAnhJson(request.hinhAnhJson())
                .trangThai("cho_duyet")
                .build();

        return danhGiaRepository.save(newDanhGia);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DanhGiaSanPham> layDanhGiaTheoSanPham(Integer sanPhamId) {
        return danhGiaRepository.findBySanPhamIdAndTrangThaiOrderByNgayTaoDesc(sanPhamId, "da_duyet");
    }

    @Override
    @Transactional(readOnly = true)
    public SaoTrungBinhDto tinhSaoTrungBinh(Integer sanPhamId) {
        Double avg = danhGiaRepository.calculateAverageSaoBySanPhamId(sanPhamId);
        Long count = danhGiaRepository.countBySanPhamIdAndTrangThaiDaDuyet(sanPhamId);
        
        if (avg == null) {
            avg = 0.0;
        } else {
            // Làm tròn 1 chữ số thập phân
            avg = Math.round(avg * 10.0) / 10.0;
        }
        
        return new SaoTrungBinhDto(avg, count == null ? 0L : count);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DanhGiaSanPham> layTatCaDanhGia(Pageable pageable) {
        return danhGiaRepository.findAllByOrderByNgayTaoDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DanhGiaSanPham> timKiemVaLocDanhGia(String tuKhoa, String trangThai, Integer sao, java.time.LocalDateTime tuNgay, java.time.LocalDateTime denNgay, Pageable pageable) {
        Page<DanhGiaSanPham> page = danhGiaRepository.timKiemVaLocDanhGia(tuKhoa, trangThai, sao, tuNgay, denNgay, pageable);
        page.getContent().forEach(dg -> {
            if (dg.getDonHang() != null && dg.getDonHang().getChiTietDonHangs() != null) {
                String variantStr = dg.getDonHang().getChiTietDonHangs().stream()
                    .filter(ct -> ct.getBienTheSanPham() != null && ct.getBienTheSanPham().getSanPham().getId().equals(dg.getSanPham().getId()))
                    .map(ct -> ct.getBienTheSanPham().getRamGb() + "GB RAM - " + ct.getBienTheSanPham().getLuuTruGb() + "GB - " + ct.getBienTheSanPham().getMauSacTen())
                    .collect(java.util.stream.Collectors.joining(", "));
                dg.setTenBienTheMua(variantStr);
            }
        });
        return page;
    }

    @Override
    @Transactional
    public DanhGiaSanPham duyetDanhGia(Integer danhGiaId) {
        DanhGiaSanPham danhGia = danhGiaRepository.findById(danhGiaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        danhGia.setTrangThai("da_duyet");
        return danhGiaRepository.save(danhGia);
    }

    @Override
    @Transactional
    public DanhGiaSanPham anDanhGia(Integer danhGiaId) {
        DanhGiaSanPham danhGia = danhGiaRepository.findById(danhGiaId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đánh giá"));
        danhGia.setTrangThai("an");
        return danhGiaRepository.save(danhGia);
    }
}
