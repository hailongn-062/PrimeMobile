package org.example.primemobile.service;

import org.example.primemobile.entity.TrungTamBaoHanh;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface ITrungTamBaoHanhService {
    Page<TrungTamBaoHanh> timKiem(String trangThai, String tuKhoa, int page, int size);
    List<TrungTamBaoHanh> layDanhSachHoatDong();
    Optional<TrungTamBaoHanh> layTheoId(Integer id);
    TrungTamBaoHanh themMoi(TrungTamBaoHanh trungTamBaoHanh);
    TrungTamBaoHanh capNhat(Integer id, TrungTamBaoHanh trungTamBaoHanh);
    TrungTamBaoHanh doiTrangThai(Integer id);
}
