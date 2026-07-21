package org.example.primemobile.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.baohanh.TaoYeuCauBaoHanhRequest;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IBaoHanhService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.example.primemobile.dto.baohanh.TraCuuBaoHanhResponse;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BaoHanhServiceImpl implements IBaoHanhService {

    private final PhieuBaoHanhRepository phieuBaoHanhRepository;
    private final YeuCauBaoHanhRepository yeuCauBaoHanhRepository;
    private final TrungTamBaoHanhRepository trungTamBaoHanhRepository;
    private final NguoiDungRepository nguoiDungRepository;
    private final MayDienThoaiRepository mayDienThoaiRepository;

    @Override
    public PhieuBaoHanh traCuuPhieuBaoHanh(String imei) {
        PhieuBaoHanh phieu = phieuBaoHanhRepository.findByMayDienThoaiImei1AndTrangThai(imei, "con_hieu_luc")
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy phiếu bảo hành hợp lệ cho IMEI này"));

        if (phieu.getNgayHetHan().isBefore(LocalDate.now())) {
            throw new IllegalStateException("Phiếu bảo hành đã hết hạn vào ngày: " + phieu.getNgayHetHan());
        }

        return phieu;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TraCuuBaoHanhResponse> traCuuTheoSoDienThoai(String sdt) {
        List<PhieuBaoHanh> danhSach = phieuBaoHanhRepository.findBySdtWithDetails(sdt, "con_hieu_luc");
        log.info("Tra cứu SĐT={}, tìm thấy {} phiếu", sdt, danhSach.size());

        return danhSach.stream()
                .filter(p -> !p.getNgayHetHan().isBefore(LocalDate.now()))
                .map(p -> TraCuuBaoHanhResponse.builder()
                        .idPhieu(p.getId())
                        .tenSanPham(p.getMayDienThoai().getBienTheSanPham().getSanPham().getTenSanPham())
                        .tenBienThe(p.getMayDienThoai().getBienTheSanPham().getMauSac() + " - " + 
                                    p.getMayDienThoai().getBienTheSanPham().getRamGb() + "GB/" + 
                                    p.getMayDienThoai().getBienTheSanPham().getLuuTruGb() + "GB")
                        .imei(p.getMayDienThoai().getImei1())
                        .tenKhachHang(p.getKhachHang().getHoTen())
                        .soDienThoai(p.getKhachHang().getSoDienThoai())
                        .ngayBatDau(p.getNgayBatDau())
                        .ngayHetHan(p.getNgayHetHan())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Tự động tạo PhieuBaoHanh cho mọi MayDienThoai trong đơn hàng đã hoàn thành.
     * - Lấy danh sách may theo don_hang_id
     * - Lấy baoHanhThang từ SanPham (qua BienTheSanPham)
     * - Chỉ tạo nếu baoHanhThang > 0 và chưa có phiếu
     */
    @Override
    @Transactional
    public void taoPhieuBaoHanhChoDonHang(DonHang donHang) {
        if (donHang.getKhachHang() == null) {
            log.info("[BaoHanh] Đơn {} không có KhachHang → bỏ qua tạo phiếu BH", donHang.getMaDonHang());
            return;
        }

        List<MayDienThoai> imeiList = mayDienThoaiRepository.findByDonHangId(donHang.getId());
        if (imeiList.isEmpty()) {
            log.info("[BaoHanh] Đơn {} không có IMEI nào → bỏ qua tạo phiếu BH", donHang.getMaDonHang());
            return;
        }

        LocalDate ngayBatDau = donHang.getNgayGiaoThucTe() != null
                ? donHang.getNgayGiaoThucTe().toLocalDate()
                : LocalDate.now();

        for (MayDienThoai may : imeiList) {
            // Kiểm tra chưa có phiếu
            if (phieuBaoHanhRepository.findByMayDienThoaiImei1AndTrangThai(may.getImei1(), "con_hieu_luc").isPresent()) {
                log.info("[BaoHanh] IMEI {} đã có phiếu BH → bỏ qua", may.getImei1());
                continue;
            }

            int soThang = may.getBienTheSanPham().getSanPham().getBaoHanhThang();
            if (soThang <= 0) {
                log.info("[BaoHanh] IMEI {} - SP không có BH → bỏ qua", may.getImei1());
                continue;
            }

            PhieuBaoHanh phieu = PhieuBaoHanh.builder()
                    .maPhieu("TEMP-" + may.getImei1()) // Temp before getting ID
                    .mayDienThoai(may)
                    .khachHang(donHang.getKhachHang())
                    .donHang(donHang)
                    .soThangBaoHanh(soThang)
                    .ngayBatDau(ngayBatDau)
                    .ngayHetHan(ngayBatDau.plusMonths(soThang))
                    .trangThai("con_hieu_luc")
                    .build();

            phieuBaoHanhRepository.saveAndFlush(phieu);
            phieu.setMaPhieu(String.format("PBH-%06d", phieu.getId()));
            phieuBaoHanhRepository.save(phieu);
            
            log.info("[BaoHanh] ✅ Tạo phiếu BH cho IMEI {} - {} tháng (hết hạn: {})",
                    may.getImei1(), soThang, phieu.getNgayHetHan());
        }
    }

    @Override
    @Transactional
    public YeuCauBaoHanh taoYeuCauBaoHanh(Integer nguoiTiepNhanId, TaoYeuCauBaoHanhRequest request) {
        PhieuBaoHanh phieu = traCuuPhieuBaoHanh(request.getImei());
        
        NguoiDung nguoiTiepNhan = nguoiDungRepository.findById(nguoiTiepNhanId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy người tiếp nhận"));
                
        TrungTamBaoHanh ttbh = trungTamBaoHanhRepository.findById(request.getTrungTamBaoHanhId())
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy Trung tâm bảo hành"));

        YeuCauBaoHanh yeuCau = YeuCauBaoHanh.builder()
                .maYeuCau("TEMP-" + System.currentTimeMillis()) // Temp code
                .phieuBaoHanh(phieu)
                .nguoiTiepNhan(nguoiTiepNhan)
                .trungTamBaoHanh(ttbh)
                .moTaLoi(request.getMoTaLoi())
                .ghiChu(request.getGhiChu())
                .hinhThuc("sua_chua") // Chỉ nhận sửa chữa
                .trangThai("tiep_nhan")
                .ngayTiepNhan(LocalDateTime.now())
                .build();

        yeuCauBaoHanhRepository.saveAndFlush(yeuCau);
        // User requested "PBH-000001" format for each reception ticket
        yeuCau.setMaYeuCau(String.format("PBH-%06d", yeuCau.getId()));
        yeuCauBaoHanhRepository.save(yeuCau);

        // Đổi trạng thái máy thành bao_hanh
        MayDienThoai may = phieu.getMayDienThoai();
        may.setTinhTrang("bao_hanh");
        mayDienThoaiRepository.save(may);
        
        log.info("Đã tạo yêu cầu bảo hành {} cho IMEI {}", yeuCau.getId(), request.getImei());
        return yeuCau;
    }

    @Override
    @Transactional
    public YeuCauBaoHanh capNhatTrangThaiYeuCau(Integer yeuCauId, String trangThaiMoi, String ketQua) {
        YeuCauBaoHanh yeuCau = yeuCauBaoHanhRepository.findById(yeuCauId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy yêu cầu bảo hành"));

        switch (trangThaiMoi) {
            case "da_gui_ttbh":
                yeuCau.setTrangThai("da_gui_ttbh");
                yeuCau.setNgayGuiTtbh(LocalDateTime.now());
                break;
            case "da_nhan_lai_ttbh":
                yeuCau.setTrangThai("da_nhan_lai_ttbh");
                yeuCau.setNgayNhanLaiTtbh(LocalDateTime.now());
                if (ketQua != null && !ketQua.trim().isEmpty()) {
                    yeuCau.setKetQuaTtbh(ketQua);
                }
                break;
            case "da_tra_khach":
                yeuCau.setTrangThai("da_tra_khach");
                yeuCau.setNgayTraKhach(LocalDateTime.now());
                
                // Trả về cho khách -> trạng thái máy về da_ban
                MayDienThoai may = yeuCau.getPhieuBaoHanh().getMayDienThoai();
                may.setTinhTrang("da_ban");
                mayDienThoaiRepository.save(may);
                break;
            default:
                throw new IllegalArgumentException("Trạng thái bảo hành không hợp lệ: " + trangThaiMoi);
        }

        return yeuCauBaoHanhRepository.save(yeuCau);
    }

    @Override
    public Page<YeuCauBaoHanh> layDanhSachYeuCau(Pageable pageable) {
        return yeuCauBaoHanhRepository.findAllByOrderByNgayTiepNhanDesc(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<org.example.primemobile.dto.baohanh.YeuCauBaoHanhResponse> layDanhSachYeuCauDtos() {
        return yeuCauBaoHanhRepository.findAllWithDetails().stream()
                .map(y -> {
                    var pbh = y.getPhieuBaoHanh();
                    var may = (pbh != null) ? pbh.getMayDienThoai() : null;
                    var btsp = (may != null) ? may.getBienTheSanPham() : null;
                    var sp = (btsp != null) ? btsp.getSanPham() : null;
                    var kh = (pbh != null) ? pbh.getKhachHang() : null;

                    return org.example.primemobile.dto.baohanh.YeuCauBaoHanhResponse.builder()
                        .id(y.getId())
                        .maYeuCau(y.getMaYeuCau())
                        .maPhieu(pbh != null ? pbh.getMaPhieu() : null)
                        .trangThai(y.getTrangThai())
                        .moTaLoi(y.getMoTaLoi())
                        .ghiChu(y.getGhiChu())
                        .ketQuaTtbh(y.getKetQuaTtbh())
                        .ngayTiepNhan(y.getNgayTiepNhan())
                        .ngayGuiTtbh(y.getNgayGuiTtbh())
                        .ngayNhanLaiTtbh(y.getNgayNhanLaiTtbh())
                        .ngayTraKhach(y.getNgayTraKhach())
                        .imei(may != null ? may.getImei1() : null)
                        .tenTrungTam(y.getTrungTamBaoHanh() != null ? y.getTrungTamBaoHanh().getTenTrungTam() : null)
                        .tenNguoiTiepNhan(y.getNguoiTiepNhan() != null ? y.getNguoiTiepNhan().getHoTen() : null)
                        .tenKhachHang(kh != null ? kh.getHoTen() : null)
                        .soDienThoai(kh != null ? kh.getSoDienThoai() : null)
                        .tenSanPham(sp != null ? sp.getTenSanPham() : null)
                        .tenBienThe(btsp != null ? (btsp.getMauSac() + " - " + btsp.getRamGb() + "GB/" + btsp.getLuuTruGb() + "GB") : null)
                        .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<YeuCauBaoHanh> layLichSuBaoHanhKhachHang(Integer khachHangId) {
        // Implement sau nếu cần cho trang cá nhân khách hàng
        return List.of();
    }
}
