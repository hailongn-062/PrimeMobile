package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.MauSac;
import org.example.primemobile.repository.MauSacRepository;
import org.example.primemobile.service.IMauSacService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MauSacServiceImpl implements IMauSacService {

    private final MauSacRepository mauSacRepository;

    @Override
    public List<MauSac> layTatCa() {
        return mauSacRepository.findAll();
    }

    @Override
    public Page<MauSac> layDanhSachPhanTrang(String tuKhoa, Pageable pageable) {
        if (tuKhoa != null && !tuKhoa.trim().isEmpty()) {
            return mauSacRepository.findByTenMauContainingIgnoreCase(tuKhoa.trim(), pageable);
        }
        return mauSacRepository.findAll(pageable);
    }

    @Override
    public MauSac layTheoId(Integer id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc ID: " + id));
    }

    @Override
    @Transactional
    public MauSac them(MauSac mauSac) {
        validateUnique(mauSac.getTenMau(), null);
        mauSac.setTenMau(mauSac.getTenMau().trim());
        return mauSacRepository.save(mauSac);
    }

    @Override
    @Transactional
    public MauSac capNhat(Integer id, MauSac mauSac) {
        MauSac existing = layTheoId(id);
        validateUnique(mauSac.getTenMau(), id);
        
        existing.setTenMau(mauSac.getTenMau().trim());
        existing.setMoTa(mauSac.getMoTa());
        
        return mauSacRepository.save(existing);
    }

    private void validateUnique(String tenMau, Integer excludeId) {
        if (tenMau == null || tenMau.trim().isEmpty()) {
            throw new IllegalArgumentException("Tên màu không được để trống");
        }
        Optional<MauSac> existing = mauSacRepository.findByTenMauIgnoreCase(tenMau.trim());
        if (existing.isPresent() && !existing.get().getId().equals(excludeId)) {
            throw new IllegalArgumentException("Tên màu đã tồn tại: " + tenMau);
        }
    }
}
