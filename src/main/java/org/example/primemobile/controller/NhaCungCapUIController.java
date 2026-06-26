package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.NhaCungCap;
import org.example.primemobile.service.INhaCungCapService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/nha-cung-cap")
@RequiredArgsConstructor
public class NhaCungCapUIController {

    private final INhaCungCapService nhaCungCapService;

    @GetMapping
    public String index(
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String trangThai,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        // Trong service, nếu trangThai hoặc tuKhoa là rỗng thì bỏ qua điều kiện lọc
        Pageable pageable = PageRequest.of(page, size, Sort.by("tenNcc").ascending());
        Page<NhaCungCap> nccPage = nhaCungCapService.layDanhSach(trangThai, tuKhoa, pageable);

        model.addAttribute("nccPage", nccPage);
        model.addAttribute("tuKhoa", tuKhoa);
        model.addAttribute("trangThai", trangThai);
        model.addAttribute("pageTitle", "Quản lý Nhà cung cấp");
        model.addAttribute("activePage", "nha-cung-cap");

        return "admin/nha-cung-cap/danh-sach";
    }

    @GetMapping("/form")
    public String form(@RequestParam(value = "id", required = false) Integer id, Model model) {
        NhaCungCap nhaCungCap;
        if (id != null) {
            nhaCungCap = nhaCungCapService.layTheoId(id);
        } else {
            nhaCungCap = new NhaCungCap();
            nhaCungCap.setTrangThai("dang_hop_tac"); // default
        }
        model.addAttribute("nhaCungCap", nhaCungCap);
        model.addAttribute("pageTitle", id != null ? "Sửa Nhà Cung Cấp" : "Thêm mới Nhà Cung Cấp");
        model.addAttribute("activePage", "nha-cung-cap");
        return "admin/nha-cung-cap/form";
    }

    @PostMapping("/save")
    public String save(@ModelAttribute("nhaCungCap") NhaCungCap nhaCungCap) {
        if (nhaCungCap.getId() != null) {
            nhaCungCapService.capNhat(nhaCungCap.getId(), nhaCungCap);
        } else {
            nhaCungCapService.taoMoi(nhaCungCap);
        }
        return "redirect:/admin/nha-cung-cap";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable("id") Integer id) {
        NhaCungCap ncc = nhaCungCapService.layTheoId(id);
        String trangThaiMoi = "dang_hop_tac".equals(ncc.getTrangThai()) ? "ngung_hop_tac" : "dang_hop_tac";
        nhaCungCapService.doiTrangThai(id, trangThaiMoi);
        return "redirect:/admin/nha-cung-cap";
    }
}
