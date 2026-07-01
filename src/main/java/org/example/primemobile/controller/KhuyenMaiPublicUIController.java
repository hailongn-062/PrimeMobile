package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.ChiTietFlashSale;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.PhamViKhuyenMai;
import org.example.primemobile.repository.ChiTietFlashSaleRepository;
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
    private final ChiTietFlashSaleRepository chiTietFlashSaleRepository;

    @GetMapping({"/khuyen-mai", "/flash-sale"})
    public String danhSachKhuyenMai(
            @RequestParam(required = false) String loai,
            HttpServletRequest request,
            Model model) {
        boolean chiFlashSale = "flash_sale".equalsIgnoreCase(loai)
                || request.getRequestURI().contains("flash-sale");
        List<ChuongTrinhKhuyenMai> danhSach = chuongTrinhKhuyenMaiRepository.findAllByOrderByIdDesc()
                .stream()
                .filter(ctkm -> !"da_ket_thuc".equals(ctkm.getTrangThai()))
                .filter(ctkm -> !"tam_dung".equals(ctkm.getTrangThai()))
                .filter(ctkm -> !chiFlashSale || "flash_sale".equals(ctkm.getLoai()))
                .toList();

        model.addAttribute("danhSachKhuyenMai", danhSach);
        model.addAttribute("chiFlashSale", chiFlashSale);
        model.addAttribute("pageTitle", chiFlashSale ? "Flash Sale" : "Khuyến mãi");
        return "khuyen-mai/danh-sach";
    }

    @GetMapping("/khuyen-mai/{id}")
    public String chiTietKhuyenMai(@PathVariable Integer id, Model model) {
        ChuongTrinhKhuyenMai khuyenMai = chuongTrinhKhuyenMaiRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy chương trình khuyến mãi."));

        List<PhamViKhuyenMai> phamVis = phamViKhuyenMaiRepository.findByChuongTrinhKhuyenMaiId(id);
        List<ChiTietFlashSale> flashSales = chiTietFlashSaleRepository.findByChuongTrinhKhuyenMaiId(id);

        model.addAttribute("khuyenMai", khuyenMai);
        model.addAttribute("phamVis", phamVis);
        model.addAttribute("flashSales", flashSales);
        model.addAttribute("pageTitle", khuyenMai.getTenCtkm());
        return "khuyen-mai/chi-tiet";
    }
}
