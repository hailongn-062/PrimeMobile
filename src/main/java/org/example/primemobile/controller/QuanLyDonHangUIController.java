package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.kho.ImeiDto;
import org.example.primemobile.dto.order.ChiTietDonHangDto;
import org.example.primemobile.dto.order.DonHangChiTietDto;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.ChiTietDonHang;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.service.IQuanLyDonHangService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller UI (Thymeleaf) cho phân hệ Quản lý Đơn Hàng.
 * Base path: {@code /admin/don-hang}
 *
 * <p>Tách biệt hoàn toàn với {@link QuanLyDonHangController} (REST API).
 * Controller này chỉ render HTML — mọi thao tác nghiệp vụ (xác nhận, hủy...)
 * được thực hiện qua JavaScript fetch() gọi đến REST API.
 */
@Slf4j
@Controller
@RequestMapping("/admin/don-hang")
@RequiredArgsConstructor
public class QuanLyDonHangUIController {

    private static final String SESSION_KEY     = "CURRENT_ADMIN";
    private static final int    PAGE_SIZE       = 15;

    private final IQuanLyDonHangService quanLyDonHangService;

    // =========================================================================
    // GET /admin/don-hang — Danh sách đơn hàng
    // =========================================================================

    @GetMapping
    public String danhSach(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maDonHang,
            @RequestParam(required = false) String soDienThoai,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        log.info("[DonHangUI] Danh sách — user={}, trangThai={}, page={}",
                currentUser != null ? currentUser.getEmail() : "?", trangThai, page);

        Page<DonHang> result = quanLyDonHangService.layDanhSachDonHang(
                trangThai, maDonHang, soDienThoai,
                PageRequest.of(page, PAGE_SIZE, Sort.by(Sort.Direction.DESC, "ngayDat"))
        );

        model.addAttribute("currentUser",    currentUser);
        model.addAttribute("pageTitle",      "Quản lý Đơn hàng");
        model.addAttribute("activePage",     "don-hang");
        model.addAttribute("danhSachDonHang", result.getContent());
        model.addAttribute("tongSoTrang",    result.getTotalPages());
        model.addAttribute("trangHienTai",   result.getNumber());
        model.addAttribute("tongSoDonHang",  result.getTotalElements());

        // Giữ lại filter để hiển thị lại trên form
        model.addAttribute("filterTrangThai",   trangThai);
        model.addAttribute("filterMaDonHang",   maDonHang);
        model.addAttribute("filterSoDienThoai", soDienThoai);

        return "admin/don-hang/danh-sach";
    }

    // =========================================================================
    // GET /admin/don-hang/{id} — Chi tiết đơn hàng
    // =========================================================================

    @GetMapping("/{id}")
    public String chiTiet(@PathVariable Integer id, Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        log.info("[DonHangUI] Chi tiết — donHangId={}, user={}", id,
                currentUser != null ? currentUser.getEmail() : "?");

        // Lấy entity đơn hàng
        DonHang donHang = quanLyDonHangService.layChiTietDonHang(id);

        // Map sang DTO để view sử dụng
        DonHangChiTietDto dto = mapToDonHangChiTietDto(donHang);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("pageTitle",   "Chi tiết Đơn hàng #" + donHang.getMaDonHang());
        model.addAttribute("activePage",  "don-hang");
        model.addAttribute("donHang",     dto); // Đưa DTO vào model thay vì entity

        return "admin/don-hang/chi-tiet";
    }

    // =========================================================================
    // PRIVATE HELPERS — Map entity sang DTO (tái sử dụng từ REST Controller)
    // =========================================================================

    /**
     * Chuyển đổi entity {@link DonHang} sang DTO {@link DonHangChiTietDto}.
     */
    private DonHangChiTietDto mapToDonHangChiTietDto(DonHang donHang) {
        if (donHang == null) {
            return null;
        }

        // Lấy danh sách IMEI đã gán cho đơn hàng (trạng thái 'da_ban') trước
        List<org.example.primemobile.entity.MayDienThoai> tempMayDienThoaiList = new java.util.ArrayList<>();
        try {
            tempMayDienThoaiList = quanLyDonHangService.layDanhSachImeiTheoDonHang(donHang.getId());
        } catch (Exception e) {
            log.warn("[DonHangUI] Không thể lấy danh sách IMEI cho đơn hàng {}: {}", donHang.getId(), e.getMessage());
        }
        final List<org.example.primemobile.entity.MayDienThoai> mayDienThoaiList = tempMayDienThoaiList;

        // Map danh sách chi tiết đơn hàng
        List<ChiTietDonHangDto> chiTietDtos = donHang.getChiTietDonHangs().stream()
                .map(ct -> mapChiTietToDto(ct, mayDienThoaiList))
                .collect(Collectors.toList());

        // Lấy thông tin khách hàng
        String tenKhachHang = donHang.getKhachHang() != null
                ? donHang.getKhachHang().getHoTen()
                : donHang.getHoTenNguoiNhan();

        String soDienThoaiKhach = donHang.getKhachHang() != null
                ? donHang.getKhachHang().getSoDienThoai()
                : donHang.getSdtNguoiNhan();

        String emailKhach = donHang.getKhachHang() != null
                ? donHang.getKhachHang().getEmail()
                : null;

        // Lấy tên nhân viên xử lý
        String tenNguoiXuLy = donHang.getNguoiXuLy() != null
                ? donHang.getNguoiXuLy().getHoTen()
                : null;

        List<ImeiDto> imeiList = mayDienThoaiList.stream()
                .map(m -> new ImeiDto(m.getId(), m.getImei1(), m.getImei2(), m.getTinhTrang()))
                .collect(Collectors.toList());

        return DonHangChiTietDto.builder()
                .id(donHang.getId())
                .maDonHang(donHang.getMaDonHang())
                .ngayDat(donHang.getNgayDat())
                .trangThai(donHang.getTrangThai())
                .trangThaiThanhToan(donHang.getTrangThaiThanhToan())
                .kenhBan(donHang.getKenhBan())
                .tenKhachHang(tenKhachHang)
                .soDienThoaiKhach(soDienThoaiKhach)
                .emailKhach(emailKhach)
                .hoTenNguoiNhan(donHang.getHoTenNguoiNhan())
                .sdtNguoiNhan(donHang.getSdtNguoiNhan())
                .diaChiGiaoCuThe(donHang.getDiaChiGiaCuThe())
                .phuongXaGiao(donHang.getPhuongXaGiao())
                .quanHuyenGiao(donHang.getQuanHuyenGiao())
                .tinhThanhGiao(donHang.getTinhThanhGiao())
                .ngayGiaoDuKien(donHang.getNgayGiaoDuKien())
                .ngayGiaoThucTe(donHang.getNgayGiaoThucTe())
                .tenNguoiXuLy(tenNguoiXuLy)
                .tongTienHang(donHang.getTongTienHang())
                .tienGiamGia(donHang.getTienGiamGia())
                .phiShip(donHang.getPhiShip())
                .tongThanhToan(donHang.getTongThanhToan())
                .ghiChu(donHang.getGhiChu())
                .imeiList(imeiList)
                .chiTietDonHangs(chiTietDtos)
                .build();
    }

    /**
     * Chuyển đổi entity {@link ChiTietDonHang} sang DTO {@link ChiTietDonHangDto}.
     */
    private ChiTietDonHangDto mapChiTietToDto(ChiTietDonHang chiTiet, List<org.example.primemobile.entity.MayDienThoai> mayDienThoaiList) {
        BienTheSanPham bt = chiTiet.getBienTheSanPham();
        SanPham sp = (bt != null) ? bt.getSanPham() : null;

        List<ImeiDto> ctImeis = new java.util.ArrayList<>();
        if (mayDienThoaiList != null && bt != null) {
            ctImeis = mayDienThoaiList.stream()
                    .filter(m -> m.getBienTheSanPham() != null && m.getBienTheSanPham().getId().equals(bt.getId()))
                    .map(m -> new ImeiDto(m.getId(), m.getImei1(), m.getImei2(), m.getTinhTrang()))
                    .collect(Collectors.toList());
        }

        return ChiTietDonHangDto.builder()
                .id(chiTiet.getId())
                .soLuong(chiTiet.getSoLuong())
                .donGiaBan(chiTiet.getDonGiaBan())
                .giaGoc(bt != null && bt.getGiaBan() != null ? bt.getGiaBan() : chiTiet.getDonGiaBan())
                .thanhTien(chiTiet.getThanhTien())
                .bienTheSanPhamId(bt != null ? bt.getId() : null)
                .maSku(bt != null ? bt.getMaSku() : null)
                .mauSac(bt != null ? bt.getMauSac() : null)
                .ramGb(bt != null ? bt.getRamGb() : null)
                .luuTruGb(bt != null ? bt.getLuuTruGb() : null)
                .tenSanPham(sp != null ? sp.getTenSanPham() : null)
                .imeiList(ctImeis)
                .build();
    }
}