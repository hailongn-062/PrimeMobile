package org.example.primemobile.service;

import org.example.primemobile.entity.HinhAnhSanPham;

import java.util.List;

public interface IHinhAnhSanPhamService {

    List<HinhAnhSanPham> layTheoBienThe(Integer bienTheId);

    HinhAnhSanPham themAnh(Integer bienTheId, HinhAnhSanPham hinhAnh);

    void xoaAnh(Integer id);

    void datLamAnhChinh(Integer id);
}
