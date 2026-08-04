package org.example.primemobile.service.impl;

import org.example.primemobile.entity.TrungTamBaoHanh;
import org.example.primemobile.repository.TrungTamBaoHanhRepository;
import org.example.primemobile.service.ITrungTamBaoHanhService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TrungTamBaoHanhServiceImpl implements ITrungTamBaoHanhService {

    @Autowired
    private TrungTamBaoHanhRepository trungTamBaoHanhRepository;

    @Override
    public Page<TrungTamBaoHanh> timKiem(String trangThai, String tuKhoa, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        String finalTrangThai = (trangThai != null && trangThai.trim().isEmpty()) ? null : trangThai;
        String finalTuKhoa = (tuKhoa != null && tuKhoa.trim().isEmpty()) ? null : tuKhoa;
        return trungTamBaoHanhRepository.timKiem(finalTrangThai, finalTuKhoa, pageable);
    }

    @Override
    public List<TrungTamBaoHanh> layDanhSachHoatDong() {
        return trungTamBaoHanhRepository.findByTrangThai("hoat_dong");
    }

    @Override
    public Optional<TrungTamBaoHanh> layTheoId(Integer id) {
        return trungTamBaoHanhRepository.findById(id);
    }

    @Override
    public TrungTamBaoHanh themMoi(TrungTamBaoHanh trungTamBaoHanh) {
        if (trungTamBaoHanhRepository.existsByTenTrungTamIgnoreCase(trungTamBaoHanh.getTenTrungTam())) {
            throw new IllegalArgumentException("Tên Trung tâm bảo hành đã tồn tại");
        }
        if (trungTamBaoHanh.getTrangThai() == null) {
            trungTamBaoHanh.setTrangThai("hoat_dong");
        }
        return trungTamBaoHanhRepository.save(trungTamBaoHanh);
    }

    @Override
    public TrungTamBaoHanh capNhat(Integer id, TrungTamBaoHanh trungTamBaoHanh) {
        TrungTamBaoHanh old = trungTamBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Trung tâm bảo hành"));
        
        if (trungTamBaoHanhRepository.existsByTenTrungTamIgnoreCaseAndIdNot(trungTamBaoHanh.getTenTrungTam(), id)) {
            throw new IllegalArgumentException("Tên Trung tâm bảo hành đã tồn tại");
        }

        old.setTenTrungTam(trungTamBaoHanh.getTenTrungTam());
        old.setSoDienThoai(trungTamBaoHanh.getSoDienThoai());
        old.setDiaChi(trungTamBaoHanh.getDiaChi());
        old.setNguoiLienHe(trungTamBaoHanh.getNguoiLienHe());
        if (trungTamBaoHanh.getTrangThai() != null) {
            old.setTrangThai(trungTamBaoHanh.getTrangThai());
        }

        return trungTamBaoHanhRepository.save(old);
    }

    @Override
    public TrungTamBaoHanh doiTrangThai(Integer id) {
        TrungTamBaoHanh ttbh = trungTamBaoHanhRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy Trung tâm bảo hành"));
        if ("hoat_dong".equals(ttbh.getTrangThai())) {
            ttbh.setTrangThai("ngung_hoat_dong");
        } else {
            ttbh.setTrangThai("hoat_dong");
        }
        return trungTamBaoHanhRepository.save(ttbh);
    }
}
