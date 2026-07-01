package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.IHangSanXuatService;
import org.example.primemobile.service.ISanPhamService;
import org.example.primemobile.service.IThongSoKyThuatService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SanPhamPublicUIController {

    private static final int PAGE_SIZE = 12;
    private static final int SEARCH_LIMIT = 200;
    private static final String LOAI_KHO_ONLINE = "kho_online";
    private static final int TON_KHO_TOI_THIEU_DE_BAN = 5;

    private final ISanPhamService sanPhamService;
    private final IDanhMucService danhMucService;
    private final IHangSanXuatService hangSanXuatService;
    private final IBienTheSanPhamService bienTheSanPhamService;
    private final IThongSoKyThuatService thongSoKyThuatService;
    private final TonKhoRepository tonKhoRepository;

    @GetMapping("/san-pham")
    public String danhSachSanPham(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            @RequestParam(required = false) String danhMuc,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        List<DanhMuc> danhMucs = danhMucService.layDanhSachKichHoat();
        List<HangSanXuat> hangSanXuats = hangSanXuatService.layTatCa();

        Integer resolvedDanhMucId = danhMucId;
        Integer resolvedHangSanXuatId = hangSanXuatId;
        String filterLabel = "Tất cả sản phẩm";

        if (hasText(danhMuc) && resolvedDanhMucId == null && resolvedHangSanXuatId == null) {
            String key = normalize(danhMuc);
            for (DanhMuc dm : danhMucs) {
                if (matches(dm.getSlug(), key) || matches(dm.getTenDanhMuc(), key)) {
                    resolvedDanhMucId = dm.getId();
                    filterLabel = dm.getTenDanhMuc();
                    break;
                }
            }
            if (resolvedDanhMucId == null) {
                for (HangSanXuat hang : hangSanXuats) {
                    if (matches(hang.getTenHang(), key) || matchesBrandAlias(hang.getTenHang(), key)) {
                        resolvedHangSanXuatId = hang.getId();
                        filterLabel = hang.getTenHang();
                        break;
                    }
                }
            }
        }

        String keyword = firstText(tuKhoa, q);
        int safePage = Math.max(page, 0);
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        List<SanPham> sanPhams;
        int totalPages;
        long totalItems;
        boolean hasPrevious;
        boolean hasNext;

        if (hasText(keyword)) {
            Page<SanPham> allProducts = sanPhamService.layDanhSachCongKhai(
                    resolvedDanhMucId,
                    resolvedHangSanXuatId,
                    PageRequest.of(0, SEARCH_LIMIT, sort));

            List<SanPham> filtered = allProducts.getContent().stream()
                    .filter(sp -> containsText(sp, keyword))
                    .toList();

            int fromIndex = Math.min(safePage * PAGE_SIZE, filtered.size());
            int toIndex = Math.min(fromIndex + PAGE_SIZE, filtered.size());
            sanPhams = filtered.subList(fromIndex, toIndex);
            totalItems = filtered.size();
            totalPages = Math.max(1, (int) Math.ceil(filtered.size() / (double) PAGE_SIZE));
            hasPrevious = safePage > 0;
            hasNext = safePage + 1 < totalPages;
            filterLabel = "Kết quả tìm kiếm";
        } else {
            Pageable pageable = PageRequest.of(safePage, PAGE_SIZE, sort);
            Page<SanPham> productPage = sanPhamService.layDanhSachCongKhai(
                    resolvedDanhMucId,
                    resolvedHangSanXuatId,
                    pageable);
            sanPhams = productPage.getContent();
            totalItems = productPage.getTotalElements();
            totalPages = productPage.getTotalPages() == 0 ? 1 : productPage.getTotalPages();
            hasPrevious = productPage.hasPrevious();
            hasNext = productPage.hasNext();
        }

        model.addAttribute("sanPhams", sanPhams);
        model.addAttribute("danhMucs", danhMucs);
        model.addAttribute("hangSanXuats", hangSanXuats);
        model.addAttribute("selectedDanhMucId", resolvedDanhMucId);
        model.addAttribute("selectedHangSanXuatId", resolvedHangSanXuatId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("filterLabel", filterLabel);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasPrevious", hasPrevious);
        model.addAttribute("hasNext", hasNext);
        model.addAttribute("pageTitle", hasText(keyword) ? "Tìm kiếm sản phẩm" : "Sản phẩm");

        return "san-pham/danh-sach";
    }

    @GetMapping("/san-pham/{id}")
    public String chiTietSanPham(@PathVariable Integer id, Model model) {
        SanPham sanPham = sanPhamService.layTheoId(id);
        if (!"dang_ban".equals(sanPham.getTrangThai())) {
            throw new EntityNotFoundException("Sản phẩm không còn được kinh doanh.");
        }

        List<BienTheSanPham> bienTheSanPhams = bienTheSanPhamService.layTheoSanPhamId(id);
        Map<Integer, Integer> tonKhoTheoBienThe = layTonKhoOnlineTheoBienThe(bienTheSanPhams);
        Map<Integer, Integer> soLuongCoTheBanTheoBienThe = tonKhoTheoBienThe.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> Math.max(entry.getValue() - TON_KHO_TOI_THIEU_DE_BAN, 0)));

        model.addAttribute("sanPham", sanPham);
        model.addAttribute("bienTheSanPhams", bienTheSanPhams);
        model.addAttribute("tonKhoTheoBienThe", tonKhoTheoBienThe);
        model.addAttribute("soLuongCoTheBanTheoBienThe", soLuongCoTheBanTheoBienThe);
        model.addAttribute("tonKhoToiThieuDeBan", TON_KHO_TOI_THIEU_DE_BAN);
        model.addAttribute("thongSoKyThuats", thongSoKyThuatService.layTheoSanPham(id));
        model.addAttribute("pageTitle", sanPham.getTenSanPham());
        return "san-pham/chi-tiet";
    }

    private Map<Integer, Integer> layTonKhoOnlineTheoBienThe(List<BienTheSanPham> bienTheSanPhams) {
        List<Integer> ids = bienTheSanPhams.stream()
                .map(BienTheSanPham::getId)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return tonKhoRepository.tongTonKhoTheoBienTheIds(LOAI_KHO_ONLINE, ids)
                .stream()
                .collect(Collectors.toMap(
                        row -> ((Number) row[0]).intValue(),
                        row -> ((Number) row[1]).intValue()));
    }

    private boolean containsText(SanPham sanPham, String keyword) {
        String key = normalize(keyword);
        return normalize(sanPham.getTenSanPham()).contains(key)
                || (sanPham.getHangSanXuat() != null && normalize(sanPham.getHangSanXuat().getTenHang()).contains(key))
                || (sanPham.getDanhMuc() != null && normalize(sanPham.getDanhMuc().getTenDanhMuc()).contains(key));
    }

    private boolean matches(String value, String key) {
        String normalized = normalize(value);
        return normalized.equals(key) || normalized.contains(key) || key.contains(normalized);
    }

    private boolean matchesBrandAlias(String brandName, String key) {
        String brand = normalize(brandName);
        return switch (key) {
            case "iphone", "ios", "apple" -> "apple".equals(brand);
            default -> false;
        };
    }

    private String firstText(String first, String second) {
        if (hasText(first)) {
            return first.trim();
        }
        if (hasText(second)) {
            return second.trim();
        }
        return "";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
