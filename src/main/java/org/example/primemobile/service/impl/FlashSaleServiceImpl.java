package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.flashsale.TaoFlashSaleRequest;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.ChiTietFlashSale;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.ChiTietFlashSaleRepository;
import org.example.primemobile.repository.ChuongTrinhKhuyenMaiRepository;
import org.example.primemobile.service.IFlashSaleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleServiceImpl implements IFlashSaleService {

    private final ChuongTrinhKhuyenMaiRepository ctkmRepository;
    private final ChiTietFlashSaleRepository ctfsRepository;
    private final BienTheSanPhamRepository bienTheRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<ChuongTrinhKhuyenMai> layDanhSachFlashSale(Pageable pageable) {
        return ctkmRepository.layDanhSachFlashSale(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChuongTrinhKhuyenMai> layFlashSaleDangDienRa() {
        return ctkmRepository.layFlashSaleDangDienRa();
    }

    @Override
    @Transactional(readOnly = true)
    public ChuongTrinhKhuyenMai layChiTietFlashSale(Integer id) {
        return ctkmRepository.findFlashSaleByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Flash Sale ID: " + id));
    }

    @Override
    @Transactional
    public ChuongTrinhKhuyenMai taoFlashSale(TaoFlashSaleRequest request) {
        if (request.getChiTiets() == null || request.getChiTiets().isEmpty()) {
            throw new IllegalArgumentException("Flash Sale phải có ít nhất 1 sản phẩm tham gia.");
        }

        ChuongTrinhKhuyenMai flashSale = ChuongTrinhKhuyenMai.builder()
                .tenCtkm(request.getTenCtkm())
                .moTa(request.getMoTa())
                .loai("flash_sale")
                .ngayBatDau(request.getNgayBatDau())
                .ngayKetThuc(request.getNgayKetThuc())
                .gioFlashBatDau(request.getGioFlashBatDau())
                .gioFlashKetThuc(request.getGioFlashKetThuc())

                .trangThai("chua_bat_dau")
                .build();

        flashSale = ctkmRepository.save(flashSale);

        List<ChiTietFlashSale> chiTiets = new ArrayList<>();
        for (TaoFlashSaleRequest.ChiTietRequest ct : request.getChiTiets()) {
            BienTheSanPham bt = bienTheRepository.findById(ct.getBienTheSanPhamId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể ID: " + ct.getBienTheSanPhamId()));
            if (ct.getPhanTramGiam() == null || ct.getPhanTramGiam().compareTo(BigDecimal.ZERO) < 0 || ct.getPhanTramGiam().compareTo(new BigDecimal("100")) > 0) {
                 throw new IllegalArgumentException("Phần trăm giảm phải từ 0 đến 100 cho biến thể " + bt.getMaSku());
            }

            chiTiets.add(ChiTietFlashSale.builder()
                    .chuongTrinhKhuyenMai(flashSale)
                    .bienTheSanPham(bt)
                    .phanTramGiam(ct.getPhanTramGiam())
                    .soLuongGioiHan(ct.getSoLuongGioiHan())
                    .daBan(0)
                    .build());
        }
        
        ctfsRepository.saveAll(chiTiets);
        flashSale.setChiTietFlashSales(chiTiets);

        return flashSale;
    }

    @Override
    @Transactional
    public ChuongTrinhKhuyenMai capNhatFlashSale(Integer id, TaoFlashSaleRequest request) {
        ChuongTrinhKhuyenMai flashSale = layChiTietFlashSale(id);
        
        if ("da_ket_thuc".equals(flashSale.getTrangThai())) {
            throw new IllegalArgumentException("Không thể sửa Flash Sale đã kết thúc.");
        }

        flashSale.setTenCtkm(request.getTenCtkm());
        flashSale.setMoTa(request.getMoTa());
        flashSale.setNgayBatDau(request.getNgayBatDau());
        flashSale.setNgayKetThuc(request.getNgayKetThuc());
        flashSale.setGioFlashBatDau(request.getGioFlashBatDau());
        flashSale.setGioFlashKetThuc(request.getGioFlashKetThuc());


        ctfsRepository.deleteAll(flashSale.getChiTietFlashSales());
        flashSale.getChiTietFlashSales().clear();
        
        List<ChiTietFlashSale> chiTiets = new ArrayList<>();
        for (TaoFlashSaleRequest.ChiTietRequest ct : request.getChiTiets()) {
            BienTheSanPham bt = bienTheRepository.findById(ct.getBienTheSanPhamId())
                    .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy biến thể ID: " + ct.getBienTheSanPhamId()));
            if (ct.getPhanTramGiam() == null || ct.getPhanTramGiam().compareTo(BigDecimal.ZERO) < 0 || ct.getPhanTramGiam().compareTo(new BigDecimal("100")) > 0) {
                 throw new IllegalArgumentException("Phần trăm giảm phải từ 0 đến 100 cho biến thể " + bt.getMaSku());
            }

            chiTiets.add(ChiTietFlashSale.builder()
                    .chuongTrinhKhuyenMai(flashSale)
                    .bienTheSanPham(bt)
                    .phanTramGiam(ct.getPhanTramGiam())
                    .soLuongGioiHan(ct.getSoLuongGioiHan())
                    .daBan(0)
                    .build());
        }
        ctfsRepository.saveAll(chiTiets);
        flashSale.getChiTietFlashSales().addAll(chiTiets);
        
        return ctkmRepository.save(flashSale);
    }

    @Override
    @Transactional
    public ChuongTrinhKhuyenMai doiTrangThai(Integer id, String trangThai) {
        ChuongTrinhKhuyenMai flashSale = ctkmRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Flash Sale ID: " + id));
        flashSale.setTrangThai(trangThai);
        return ctkmRepository.save(flashSale);
    }
}
