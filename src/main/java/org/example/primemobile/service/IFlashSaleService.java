package org.example.primemobile.service;

import java.util.List;

import org.example.primemobile.dto.flashsale.TaoFlashSaleRequest;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IFlashSaleService {
    Page<ChuongTrinhKhuyenMai> layDanhSachFlashSale(Pageable pageable);

    List<ChuongTrinhKhuyenMai> layFlashSaleDangDienRa();

    ChuongTrinhKhuyenMai layChiTietFlashSale(Integer id);

    ChuongTrinhKhuyenMai taoFlashSale(TaoFlashSaleRequest request);

    ChuongTrinhKhuyenMai capNhatFlashSale(Integer id, TaoFlashSaleRequest request);

    ChuongTrinhKhuyenMai doiTrangThai(Integer id, String trangThai);
}
