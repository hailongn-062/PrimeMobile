package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.PhamViKhuyenMai;
import org.example.primemobile.repository.ChuongTrinhKhuyenMaiRepository;
import org.example.primemobile.repository.PhamViKhuyenMaiRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class KhuyenMaiPublicUIController {

    private final ChuongTrinhKhuyenMaiRepository chuongTrinhKhuyenMaiRepository;
    private final PhamViKhuyenMaiRepository phamViKhuyenMaiRepository;

    @GetMapping("/khuyen-mai")
    public String danhSachKhuyenMai(Model model) {
        List<ChuongTrinhKhuyenMai> danhSach = chuongTrinhKhuyenMaiRepository.findAllByOrderByIdDesc()
                .stream()
                .filter(ctkm -> !"da_ket_thuc".equals(ctkm.getTrangThai()))
                .filter(ctkm -> !"tam_dung".equals(ctkm.getTrangThai()))
                .toList();

        model.addAttribute("danhSachKhuyenMai", danhSach);
        model.addAttribute("pageTitle", "Khuyến mãi");
        return "khuyen-mai/danh-sach";
    }

    @GetMapping("/khuyen-mai/{id}")
    public String chiTietKhuyenMai(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai khuyenMai = chuongTrinhKhuyenMaiRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình khuyến mãi."));

        List<PhamViKhuyenMai> phamVis = phamViKhuyenMaiRepository.findByChuongTrinhKhuyenMaiId(id);

        model.addAttribute("khuyenMai", khuyenMai);
        model.addAttribute("phamVis", phamVis);
        model.addAttribute("pageTitle", khuyenMai.getTenCtkm());
        return "khuyen-mai/chi-tiet";
    }
}
