package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.repository.DanhGiaSanPhamRepository;
import org.example.primemobile.repository.YeuThichRepository;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.ISanPhamService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class TrangChuController {

    private static final int SO_LUONG_NOI_BAT = 8;

    private final IDanhMucService danhMucService;
    private final ISanPhamService sanPhamService;
    private final IKhuyenMaiService khuyenMaiService; // ✅ Inject service khuyến mãi
    private final DanhGiaSanPhamRepository danhGiaRepository;
    private final YeuThichRepository yeuThichRepository;

    @GetMapping({"/", "/trang-chu"})
    public String trangChu(Model model, HttpSession session) {
        log.info("[TrangChu] GET /");

        model.addAttribute("danhSachDanhMuc", danhMucService.layDanhSachKichHoat());

        Page<SanPham> trangSanPham = sanPhamService.layDanhSachCongKhai(
                null,
                null,
                PageRequest.of(0, SO_LUONG_NOI_BAT, Sort.by(Sort.Direction.DESC, "id"))
        );
        List<SanPham> sanPhamNoiBat = trangSanPham.getContent();

        // ── Tính giá sau khuyến mãi cho từng sản phẩm nổi bật ──
        Map<Integer, BigDecimal> giaSauKhuyenMaiTheoSanPham = new HashMap<>();
        for (SanPham sp : sanPhamNoiBat) {
            if (sp.getBienTheSanPhams() != null && !sp.getBienTheSanPhams().isEmpty()) {
                BienTheSanPham firstBt = sp.getBienTheSanPhams().get(0);
                BigDecimal giaSauKM = khuyenMaiService.tinhGiaSauKhuyenMai(firstBt.getId(), null);
                giaSauKhuyenMaiTheoSanPham.put(sp.getId(), giaSauKM != null ? giaSauKM : firstBt.getGiaBan());
            }
        }

        model.addAttribute("sanPhamNoiBat", sanPhamNoiBat);
        model.addAttribute("giaSauKhuyenMaiTheoSanPham", giaSauKhuyenMaiTheoSanPham); // ✅ Truyền vào view

        // ── Thêm dữ liệu Đánh giá và Yêu thích ──
        Map<Integer, Double> ratingTbTheoSp = new HashMap<>();
        Map<Integer, Long> ratingCountTheoSp = new HashMap<>();
        Set<Integer> sanPhamYeuThichIds = new HashSet<>();

        if (!sanPhamNoiBat.isEmpty()) {
            List<Integer> spIds = sanPhamNoiBat.stream().map(SanPham::getId).collect(Collectors.toList());
            List<Object[]> ratings = danhGiaRepository.getAverageSaoAndCountBySanPhamIds(spIds);
            for (Object[] row : ratings) {
                ratingTbTheoSp.put((Integer) row[0], (Double) row[1]);
                ratingCountTheoSp.put((Integer) row[0], (Long) row[2]);
            }

            SessionKhachHang kh = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
            if (kh != null) {
                List<Integer> ytIds = yeuThichRepository.findSanPhamIdsByKhachHangIdAndSanPhamIds(kh.getKhachHangId(), spIds);
                sanPhamYeuThichIds.addAll(ytIds);
            }
        }
        model.addAttribute("ratingTbTheoSp", ratingTbTheoSp);
        model.addAttribute("ratingCountTheoSp", ratingCountTheoSp);
        model.addAttribute("sanPhamYeuThichIds", sanPhamYeuThichIds);

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