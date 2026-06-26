package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.NhaCungCap;
import org.example.primemobile.repository.NhaCungCapRepository;
import org.example.primemobile.service.INhaCungCapService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class NhaCungCapServiceImpl implements INhaCungCapService {

    private static final Set<String> TRANG_THAI_HOP_LE =
            Set.of("dang_hop_tac", "ngung_hop_tac");

    private final NhaCungCapRepository nhaCungCapRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<NhaCungCap> layDanhSach(String trangThai, String tuKhoa, Pageable pageable) {
        String tt = (trangThai != null && !trangThai.isBlank()) ? trangThai.trim() : null;
        String tk = (tuKhoa    != null && !tuKhoa.isBlank())    ? tuKhoa.trim()    : null;
        return nhaCungCapRepo.timKiem(tt, tk, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<NhaCungCap> layDangHopTac() {
        return nhaCungCapRepo.findByTrangThaiOrderByTenNccAsc("dang_hop_tac");
    }

    @Override
    @Transactional
    public NhaCungCap taoMoi(NhaCungCap request) {
        if (request.getTenNcc() == null || request.getTenNcc().isBlank()) {
            throw new IllegalArgumentException("Tên nhà cung cấp không được trống.");
        }
        // Tự sinh mã NCC nếu không truyền
        String maNcc = (request.getMaNcc() != null && !request.getMaNcc().isBlank())
                ? request.getMaNcc().toUpperCase().trim()
                : "NCC-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        if (nhaCungCapRepo.existsByMaNcc(maNcc)) {
            throw new IllegalArgumentException("Mã nhà cung cấp '" + maNcc + "' đã tồn tại.");
        }

        NhaCungCap entity = NhaCungCap.builder()
                .maNcc(maNcc)
                .tenNcc(request.getTenNcc().trim())
                .soDienThoai(request.getSoDienThoai())
                .email(request.getEmail())
                .diaChi(request.getDiaChi())
                .nguoiLienHe(request.getNguoiLienHe())
                .trangThai("dang_hop_tac")
                .build();

        NhaCungCap saved = nhaCungCapRepo.save(entity);
        log.info("[NhaCungCap] Tạo mới — maNcc={}, ten={}", saved.getMaNcc(), saved.getTenNcc());
        return saved;
    }

    @Override
    @Transactional
    public NhaCungCap capNhat(Integer id, NhaCungCap request) {
        NhaCungCap entity = layHoacNemLoi(id);
        if (request.getTenNcc()     != null) entity.setTenNcc(request.getTenNcc().trim());
        if (request.getSoDienThoai()!= null) entity.setSoDienThoai(request.getSoDienThoai());
        if (request.getEmail()      != null) entity.setEmail(request.getEmail());
        if (request.getDiaChi()     != null) entity.setDiaChi(request.getDiaChi());
        if (request.getNguoiLienHe()!= null) entity.setNguoiLienHe(request.getNguoiLienHe());
        log.info("[NhaCungCap] Cập nhật — id={}", id);
        return nhaCungCapRepo.save(entity);
    }

    @Override
    @Transactional
    public NhaCungCap doiTrangThai(Integer id, String trangThaiMoi) {
        if (!TRANG_THAI_HOP_LE.contains(trangThaiMoi)) {
            throw new IllegalArgumentException(
                    "Trạng thái '" + trangThaiMoi + "' không hợp lệ. " +
                    "Chỉ chấp nhận: 'dang_hop_tac' hoặc 'ngung_hop_tac'.");
        }
        NhaCungCap entity = layHoacNemLoi(id);
        entity.setTrangThai(trangThaiMoi);
        log.info("[NhaCungCap] Đổi trạng thái id={} → {}", id, trangThaiMoi);
        return nhaCungCapRepo.save(entity);
    }

    private NhaCungCap layHoacNemLoi(Integer id) {
        return nhaCungCapRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy nhà cung cấp ID: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public NhaCungCap layTheoId(Integer id) {
        return layHoacNemLoi(id);
    }
}
