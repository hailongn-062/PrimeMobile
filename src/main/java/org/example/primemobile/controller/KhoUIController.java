package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.Kho;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.KhoRepository;
import org.example.primemobile.repository.NhaCungCapRepository;
import org.example.primemobile.service.IMayDienThoaiService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Slf4j
@Controller
@RequestMapping("/admin/kho")
@RequiredArgsConstructor
public class KhoUIController {

    private final KhoRepository khoRepository;
    private final NhaCungCapRepository nhaCungCapRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final IMayDienThoaiService mayDienThoaiService; // ✅ Inject service để lấy IMEI

    @GetMapping
    public String trangChuKho() {
        return "redirect:/admin/kho/quan-ly";
    }

    @GetMapping("/quan-ly")
    public String quanLyKho(Model model) {
        try {
            model.addAttribute("danhSachKho", khoRepository.findAll());

            model.addAttribute("pageTitle", "Quản lý Kho");
            model.addAttribute("activePage", "quan-ly-kho");
        } catch (Exception e) {
            log.error("Lỗi khi tải trang Quản lý Kho", e);
        }
        return "admin/kho/quan-ly-kho";
    }

    @GetMapping("/phieu-nhap")
    public String phieuNhapKho(Model model) {
        try {
            // Lấy kho duy nhất trong hệ thống (ID = 1)
            List<Kho> danhSachKho = khoRepository.findById(1)
                    .map(List::of).orElseGet(List::of);

            model.addAttribute("danhSachKho", danhSachKho);
            model.addAttribute("danhSachNhaCungCap", nhaCungCapRepository.findByTrangThaiOrderByTenNccAsc("dang_hop_tac"));
            model.addAttribute("danhSachBienThe", bienTheSanPhamRepository.findAll());

            model.addAttribute("pageTitle", "Tạo Phiếu Nhập Kho");
            model.addAttribute("activePage", "phieu-nhap");
        } catch (Exception e) {
            log.error("Lỗi khi tải trang Phiếu Nhập Kho", e);
        }
        return "admin/kho/phieu-nhap";
    }

    @GetMapping("/phieu-chuyen")
    public String phieuChuyenKho(Model model) {
        try {
            model.addAttribute("danhSachKho", khoRepository.findAll());
            model.addAttribute("danhSachBienThe", bienTheSanPhamRepository.findAll());

            model.addAttribute("pageTitle", "Quản lý Phiếu Chuyển Kho");
            model.addAttribute("activePage", "phieu-chuyen");
        } catch (Exception e) {
            log.error("Lỗi khi tải trang Phiếu Chuyển Kho", e);
        }
        return "admin/kho/phieu-chuyen";
    }

    @GetMapping("/kiem-soat-imei")
    public String kiemSoatImei(Model model) {
        try {
            model.addAttribute("danhSachKho", khoRepository.findAll());
            model.addAttribute("danhSachBienThe", bienTheSanPhamRepository.findAll());

            model.addAttribute("pageTitle", "Kiểm soát IMEI");
            model.addAttribute("activePage", "kiem-soat-imei");
        } catch (Exception e) {
            log.error("Lỗi khi tải trang Kiểm soát IMEI", e);
        }
        return "admin/kho/kiem-soat-imei";
    }

    // =========================================================================
    // GET /admin/kho/imei/{bienTheId} — Xem danh sách IMEI của một biến thể
    // =========================================================================

    /**
     * Hiển thị danh sách IMEI của một biến thể sản phẩm.
     * <p>
     * Hỗ trợ lọc theo kho và trạng thái IMEI.
     *
     * @param bienTheId ID của biến thể sản phẩm (SKU) cần xem IMEI.
     * @param khoId     (Optional) ID kho để lọc IMEI theo kho.
     * @param tinhTrang (Optional) Trạng thái IMEI để lọc ('trong_kho', 'da_ban', 'bao_hanh', 'loi_hong').
     * @param model     Spring Model để truyền dữ liệu sang view.
     * @return Tên view "admin/kho/chi-tiet-imei".
     */
    @GetMapping("/imei/{bienTheId}")
    public String chiTietImei(
            @PathVariable Integer bienTheId,
            @RequestParam(required = false) Integer khoId,
            @RequestParam(required = false) String tinhTrang,
            Model model) {

        log.info("[KhoUI] Xem chi tiết IMEI — bienTheId={}, khoId={}, tinhTrang={}",
                bienTheId, khoId, tinhTrang);

        try {
            // Lấy thông tin biến thể
            var bienThe = bienTheSanPhamRepository.findById(bienTheId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể sản phẩm"));

            // Lấy danh sách IMEI (không lọc theo kho vì hệ thống chỉ có 1 kho duy nhất)
            List<org.example.primemobile.entity.MayDienThoai> danhSachImei =
                    mayDienThoaiService.layDanhSachTheoBienTheVaTrangThai(bienTheId, tinhTrang);

            // Lấy danh sách kho để hiển thị dropdown lọc
            List<Kho> danhSachKho = khoRepository.findAll();

            // Truyền dữ liệu vào model
            model.addAttribute("bienThe", bienThe);
            model.addAttribute("danhSachImei", danhSachImei);
            model.addAttribute("danhSachKho", danhSachKho);
            model.addAttribute("selectedKhoId", khoId);
            model.addAttribute("selectedTinhTrang", tinhTrang);
            model.addAttribute("pageTitle", "Chi tiết IMEI - " + bienThe.getMaSku());
            model.addAttribute("activePage", "quan-ly-kho");

            return "admin/kho/chi-tiet-imei";

        } catch (Exception e) {
            log.error("Lỗi khi tải trang chi tiết IMEI", e);
            model.addAttribute("error", "Không thể tải danh sách IMEI: " + e.getMessage());
            return "admin/kho/chi-tiet-imei";
        }
    }
}