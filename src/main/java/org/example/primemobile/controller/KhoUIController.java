package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.Kho;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.KhoRepository;
import org.example.primemobile.repository.NhaCungCapRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
            // Chỉ lấy kho_tong
            Optional<Kho> khoTongOpt = khoRepository.findByLoai("kho_tong");
            List<Kho> danhSachKhoTong = khoTongOpt.map(List::of).orElseGet(List::of);

            model.addAttribute("danhSachKho", danhSachKhoTong);
            model.addAttribute("danhSachNhaCungCap", nhaCungCapRepository.findAll());
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
}
