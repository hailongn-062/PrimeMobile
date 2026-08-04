package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.TrungTamBaoHanh;
import org.example.primemobile.service.ITrungTamBaoHanhService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/trung-tam-bao-hanh")
@RequiredArgsConstructor
public class TrungTamBaoHanhUIController {

    private final ITrungTamBaoHanhService service;

    @GetMapping("/danh-sach")
    public String index(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Page<TrungTamBaoHanh> ttbhPage = service.timKiem(trangThai, tuKhoa, page, size);

        model.addAttribute("ttbhPage", ttbhPage);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("pageTitle", "Quản lý Trung tâm bảo hành");
        model.addAttribute("activePage", "trung-tam-bao-hanh");

        return "admin/trung-tam-bao-hanh/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        TrungTamBaoHanh trungTamBaoHanh;
        if (id != null) {
            trungTamBaoHanh = service.layTheoId(id).orElse(new TrungTamBaoHanh());
        } else {
            trungTamBaoHanh = new TrungTamBaoHanh();
            trungTamBaoHanh.setTrangThai("hoat_dong"); // default
        }
        
        model.addAttribute("trungTamBaoHanh", trungTamBaoHanh);
        model.addAttribute("pageTitle", id != null ? "Cập nhật Trung tâm bảo hành" : "Thêm mới Trung tâm bảo hành");
        model.addAttribute("activePage", "trung-tam-bao-hanh");
        
        return "admin/trung-tam-bao-hanh/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("trungTamBaoHanh") TrungTamBaoHanh trungTamBaoHanh) {
        if (trungTamBaoHanh.getId() != null) {
            service.capNhat(trungTamBaoHanh.getId(), trungTamBaoHanh);
        } else {
            service.themMoi(trungTamBaoHanh);
        }
        return "redirect:/admin/trung-tam-bao-hanh/danh-sach";
    }
}
