package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TrangChuController {

    private static final int SO_LUONG_NOI_BAT = 8;

    private final IDanhMucService danhMucService;
    private final ISanPhamService sanPhamService;

    @GetMapping({"/", "/trang-chu"})
    public String trangChu(Model model) {
        log.info("[TrangChu] GET /");

        model.addAttribute("danhSachDanhMuc", danhMucService.layDanhSachKichHoat());

        Page<SanPham> trangSanPham = sanPhamService.layDanhSachCongKhai(
                null,
                null,
                PageRequest.of(0, SO_LUONG_NOI_BAT, Sort.by(Sort.Direction.DESC, "id"))
        );
        model.addAttribute("sanPhamNoiBat", trangSanPham.getContent());

        model.addAttribute("pageTitle", "Trang Chủ");
        model.addAttribute("pageDescription",
                "PrimeMobile - Mua điện thoại chính hãng, giá tốt, giao hàng nhanh toàn quốc.");

        return "index";
    }

    @GetMapping("/favicon.ico")
    public String favicon() {
        return "redirect:/images/PrimeMobile_logo.png";
    }
}
