package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.DiaChiKhachHang;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.repository.DiaChiKhachHangRepository;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.service.IDiaChiService;
import org.example.primemobile.util.ValidationUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DiaChiServiceImpl implements IDiaChiService {

    private final DiaChiKhachHangRepository diaChiRepo;
    private final KhachHangRepository       khachHangRepo;

    @Override
    @Transactional(readOnly = true)
    public List<DiaChiKhachHang> layDanhSach(Integer khachHangId) {
        validateKhachHang(khachHangId);
        return diaChiRepo.findByKhachHangIdOrderByMacDinhDesc(khachHangId);
    }

    @Override
    @Transactional
    public DiaChiKhachHang them(Integer khachHangId, DiaChiKhachHang request) {
        KhachHang khachHang = layKhachHangHoacNemLoi(khachHangId);

        // Giới hạn tối đa MAX_DIA_CHI địa chỉ / khách hàng
        long soLuongHienTai = diaChiRepo.countByKhachHangId(khachHangId);
        if (soLuongHienTai >= MAX_DIA_CHI) {
            throw new IllegalArgumentException(String.format(
                    "Mỗi khách hàng chỉ được lưu tối đa %d địa chỉ. " +
                    "Vui lòng xóa bớt địa chỉ cũ trước khi thêm mới.", MAX_DIA_CHI));
        }

        validateDiaChiRequest(request);

        boolean isMacDinh = Boolean.TRUE.equals(request.getMacDinh());

        // Nếu đây là địa chỉ đầu tiên, tự động set làm mặc định
        boolean laDiaDauTien = soLuongHienTai == 0;
        if (laDiaDauTien) {
            request = DiaChiKhachHang.builder()
                    .khachHang(khachHang)
                    .loaiDiaChi(request.getLoaiDiaChi() != null ? request.getLoaiDiaChi() : "nha_rieng")
                    .hoTenNguoiNhan(khachHang.getHoTen())
                    .soDienThoaiNguoiNhan(khachHang.getSoDienThoai())
                    .tenGoiNho(request.getTenGoiNho())
                    .diaChiChiTiet(request.getDiaChiChiTiet())
                    .tinhThanhId(request.getTinhThanhId())
                    .quanHuyenId(request.getQuanHuyenId())
                    .phuongXaCode(request.getPhuongXaCode())
                    .tinhThanhTen(request.getTinhThanhTen())
                    .quanHuyenTen(request.getQuanHuyenTen())
                    .phuongXaTen(request.getPhuongXaTen())
                    .macDinh(true)
                    .build();
        } else {
            request.setKhachHang(khachHang);
            request.setMacDinh(false);
            if (request.getHoTenNguoiNhan() == null) request.setHoTenNguoiNhan(khachHang.getHoTen());
            if (request.getSoDienThoaiNguoiNhan() == null) request.setSoDienThoaiNguoiNhan(khachHang.getSoDienThoai());
        }

        DiaChiKhachHang saved = diaChiRepo.save(request);
        
        if (!laDiaDauTien && isMacDinh) {
            saved = setMacDinh(saved.getId(), khachHangId);
        }

        log.info("[DiaChi] Thêm thành công — khachHangId={}, diaChiId={}, macDinh={}",
                khachHangId, saved.getId(), saved.getMacDinh());
        return saved;
    }

    @Override
    @Transactional
    public DiaChiKhachHang sua(Integer diaChiId, Integer khachHangId, DiaChiKhachHang request) {
        DiaChiKhachHang entity = layDiaChiCuaKhach(diaChiId, khachHangId);
        boolean isMacDinh = Boolean.TRUE.equals(request.getMacDinh());

        // Chỉ cập nhật field nào được gửi lên
        if (request.getLoaiDiaChi()           != null) entity.setLoaiDiaChi(request.getLoaiDiaChi());
        if (request.getHoTenNguoiNhan()       != null) entity.setHoTenNguoiNhan(request.getHoTenNguoiNhan());
        if (request.getSoDienThoaiNguoiNhan() != null) {
            if (!ValidationUtils.isValidPhoneNumber(request.getSoDienThoaiNguoiNhan())) {
                throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
            }
            entity.setSoDienThoaiNguoiNhan(request.getSoDienThoaiNguoiNhan());
        }
        if (request.getTenGoiNho()            != null) entity.setTenGoiNho(request.getTenGoiNho());
        if (request.getDiaChiChiTiet()        != null) entity.setDiaChiChiTiet(request.getDiaChiChiTiet());
        if (request.getTinhThanhId()          != null) entity.setTinhThanhId(request.getTinhThanhId());
        if (request.getQuanHuyenId()          != null) entity.setQuanHuyenId(request.getQuanHuyenId());
        if (request.getPhuongXaCode()         != null) entity.setPhuongXaCode(request.getPhuongXaCode());
        if (request.getTinhThanhTen()         != null) entity.setTinhThanhTen(request.getTinhThanhTen());
        if (request.getQuanHuyenTen()         != null) entity.setQuanHuyenTen(request.getQuanHuyenTen());
        if (request.getPhuongXaTen()          != null) entity.setPhuongXaTen(request.getPhuongXaTen());

        log.info("[DiaChi] Sửa thành công — diaChiId={}", diaChiId);
        DiaChiKhachHang saved = diaChiRepo.save(entity);
        
        if (isMacDinh && !Boolean.TRUE.equals(entity.getMacDinh())) {
            saved = setMacDinh(saved.getId(), khachHangId);
        }
        return saved;
    }

    @Override
    @Transactional
    public DiaChiKhachHang setMacDinh(Integer diaChiId, Integer khachHangId) {
        // Xác nhận địa chỉ thuộc về khách hàng này
        DiaChiKhachHang entity = layDiaChiCuaKhach(diaChiId, khachHangId);

        // Reset tất cả địa chỉ của khách về macDinh = false
        diaChiRepo.resetMacDinhByKhachHangId(khachHangId);

        // Set địa chỉ này làm mặc định
        entity.setMacDinh(true);
        DiaChiKhachHang saved = diaChiRepo.save(entity);
        log.info("[DiaChi] Set mặc định — diaChiId={}, khachHangId={}", diaChiId, khachHangId);
        return saved;
    }

    @Override
    @Transactional
    public void xoa(Integer diaChiId, Integer khachHangId) {
        DiaChiKhachHang entity = layDiaChiCuaKhach(diaChiId, khachHangId);

        // Không được xóa địa chỉ duy nhất còn lại
        long soLuong = diaChiRepo.countByKhachHangId(khachHangId);
        if (soLuong <= 1) {
            throw new IllegalArgumentException(
                    "Không thể xóa địa chỉ duy nhất. Vui lòng thêm địa chỉ khác trước khi xóa.");
        }

        // Nếu xóa địa chỉ mặc định → tự động chuyển mặc định cho địa chỉ khác
        if (Boolean.TRUE.equals(entity.getMacDinh())) {
            diaChiRepo.findByKhachHangIdOrderByMacDinhDesc(khachHangId).stream()
                    .filter(d -> !d.getId().equals(diaChiId))
                    .findFirst()
                    .ifPresent(d -> {
                        d.setMacDinh(true);
                        diaChiRepo.save(d);
                    });
        }

        diaChiRepo.deleteById(diaChiId);
        log.info("[DiaChi] Xóa thành công — diaChiId={}", diaChiId);
    }

    // ── PRIVATE HELPERS ──────────────────────────────────────────────────

    private KhachHang layKhachHangHoacNemLoi(Integer khachHangId) {
        return khachHangRepo.findById(khachHangId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy khách hàng ID: " + khachHangId));
    }

    private void validateKhachHang(Integer khachHangId) {
        if (!khachHangRepo.existsById(khachHangId)) {
            throw new EntityNotFoundException("Không tìm thấy khách hàng ID: " + khachHangId);
        }
    }

    /**
     * Lấy địa chỉ và đảm bảo nó thuộc về đúng khách hàng (tránh IDOR attack).
     */
    private DiaChiKhachHang layDiaChiCuaKhach(Integer diaChiId, Integer khachHangId) {
        DiaChiKhachHang entity = diaChiRepo.findById(diaChiId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy địa chỉ ID: " + diaChiId));
        if (!entity.getKhachHang().getId().equals(khachHangId)) {
            throw new IllegalArgumentException(
                    "Địa chỉ ID " + diaChiId + " không thuộc về khách hàng ID " + khachHangId + ".");
        }
        return entity;
    }

    private void validateDiaChiRequest(DiaChiKhachHang req) {
        if (req.getSoDienThoaiNguoiNhan() != null && !req.getSoDienThoaiNguoiNhan().isBlank()) {
            if (!ValidationUtils.isValidPhoneNumber(req.getSoDienThoaiNguoiNhan())) {
                throw new IllegalArgumentException(ValidationUtils.PHONE_INVALID_MSG);
            }
        }
        if (req.getDiaChiChiTiet() == null || req.getDiaChiChiTiet().isBlank()) {
            throw new IllegalArgumentException("Địa chỉ chi tiết không được trống.");
        }
        if (req.getTinhThanhId() == null || req.getQuanHuyenId() == null || req.getPhuongXaCode() == null) {
            throw new IllegalArgumentException("Thông tin tỉnh/thành, quận/huyện, phường/xã không được trống.");
        }
        if (req.getTinhThanhTen() == null || req.getQuanHuyenTen() == null || req.getPhuongXaTen() == null) {
            throw new IllegalArgumentException("Tên tỉnh/thành, quận/huyện, phường/xã không được trống.");
        }
    }
}
