package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.DanhGiaSanPhamRepository;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.repository.YeuThichRepository;
import org.example.primemobile.service.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static org.springframework.util.StringUtils.hasText;

@Slf4j
@Controller
@RequiredArgsConstructor
public class SanPhamPublicUIController {

    private static final int PAGE_SIZE = 12;
    private static final int SEARCH_LIMIT = 200;
    private static final int    KHO_ID                  = 1; // Kho duy nhất trong hệ thống

    private final ISanPhamService sanPhamService;
    private final IDanhMucService danhMucService;
    private final IHangSanXuatService hangSanXuatService;
    private final IBienTheSanPhamService bienTheSanPhamService;
    private final IThongSoKyThuatService thongSoKyThuatService;
    private final TonKhoRepository tonKhoRepository;
    private final IKhuyenMaiService khuyenMaiService;
    private final DanhGiaSanPhamRepository danhGiaRepository;
    private final YeuThichRepository yeuThichRepository;
    private final org.example.primemobile.repository.ChiTietDonHangRepository chiTietDonHangRepository;

    @GetMapping("/san-pham")
    public String danhSachSanPham(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            @RequestParam(required = false) String danhMuc,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) List<Integer> luuTruGbs,
            @RequestParam(required = false) List<String> mauSacs,
            @RequestParam(required = false) String sortOption,
            @RequestParam(defaultValue = "0") int page,
            Model model,
            HttpSession session) {

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
        
        // 1. Fetch products from DB
        // Fetch a large number (e.g. 500) to allow accurate in-memory filtering and sorting.
        Page<SanPham> allProducts = sanPhamService.layDanhSachCongKhai(
                null,
                resolvedDanhMucId,
                resolvedHangSanXuatId,
                PageRequest.of(0, 1000, Sort.by(Sort.Direction.DESC, "id")));

        List<SanPham> sanPhamsList = new ArrayList<>(allProducts.getContent());

        // Pre-calculate prices to avoid recalculating during sort/filter
        Map<Integer, BigDecimal> giaMinTheoSanPham = new HashMap<>();
        Map<Integer, BigDecimal> giaMaxTheoSanPham = new HashMap<>();
        Map<Integer, BigDecimal> giaSauKhuyenMaiTheoSanPham = new HashMap<>();

        for (SanPham sp : sanPhamsList) {
            if (sp.getBienTheSanPhams() != null && !sp.getBienTheSanPhams().isEmpty()) {
                BigDecimal minP = null;
                BigDecimal maxP = null;
                // Display price is usually the first variant's price
                BienTheSanPham firstBt = sp.getBienTheSanPhams().get(0);
                BigDecimal giaSauKMDauTien = khuyenMaiService.tinhGiaSauKhuyenMai(firstBt.getId(), null);
                giaSauKhuyenMaiTheoSanPham.put(sp.getId(), giaSauKMDauTien != null ? giaSauKMDauTien : firstBt.getGiaBan());

                for (BienTheSanPham bt : sp.getBienTheSanPhams()) {
                    BigDecimal giaKM = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
                    BigDecimal finalGia = giaKM != null ? giaKM : bt.getGiaBan();
                    if (minP == null || finalGia.compareTo(minP) < 0) minP = finalGia;
                    if (maxP == null || finalGia.compareTo(maxP) > 0) maxP = finalGia;
                }
                giaMinTheoSanPham.put(sp.getId(), minP);
                giaMaxTheoSanPham.put(sp.getId(), maxP);
            }
        }

        // 2. Apply Filters (In-Memory)
        List<SanPham> filtered = sanPhamsList.stream().filter(sp -> {
            // Keyword filter
            if (hasText(keyword) && !containsText(sp, keyword)) {
                return false;
            }

            // Variants filter (Price, ROM, Color)
            boolean hasMatchingVariant = false;
            if (sp.getBienTheSanPhams() != null) {
                for (BienTheSanPham bt : sp.getBienTheSanPhams()) {
                    boolean match = true;
                    // Price filter
                    BigDecimal gia = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
                    if (gia == null) gia = bt.getGiaBan();
                    if (minPrice != null && gia.compareTo(minPrice) < 0) match = false;
                    if (maxPrice != null && gia.compareTo(maxPrice) > 0) match = false;
                    
                    // Memory filter
                    if (luuTruGbs != null && !luuTruGbs.isEmpty() && !luuTruGbs.contains(bt.getLuuTruGb())) match = false;
                    
                    // Color filter
                    if (mauSacs != null && !mauSacs.isEmpty() && !mauSacs.contains(bt.getMauSac())) match = false;

                    if (match) {
                        hasMatchingVariant = true;
                        break;
                    }
                }
            }
            // If any specific variant filter is active, we MUST have a matching variant
            if ((minPrice != null || maxPrice != null || (luuTruGbs != null && !luuTruGbs.isEmpty()) || (mauSacs != null && !mauSacs.isEmpty())) && !hasMatchingVariant) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());

        // 3. Apply Sorting
        if (hasText(sortOption)) {
            if ("price_asc".equals(sortOption)) {
                filtered.sort(Comparator.comparing((SanPham sp) -> giaMinTheoSanPham.getOrDefault(sp.getId(), BigDecimal.valueOf(Long.MAX_VALUE))));
            } else if ("price_desc".equals(sortOption)) {
                filtered.sort(Comparator.comparing((SanPham sp) -> giaMinTheoSanPham.getOrDefault(sp.getId(), BigDecimal.ZERO)).reversed());
            } else if ("best_selling".equals(sortOption)) {
                List<Object[]> thongKe = chiTietDonHangRepository.thongKeSoLuongBanTheoSanPham();
                Map<Integer, Long> soldMap = new HashMap<>();
                for (Object[] row : thongKe) {
                    soldMap.put((Integer) row[0], ((Number) row[1]).longValue());
                }
                filtered.sort((sp1, sp2) -> {
                    Long s1 = soldMap.getOrDefault(sp1.getId(), 0L);
                    Long s2 = soldMap.getOrDefault(sp2.getId(), 0L);
                    return s2.compareTo(s1);
                });
            } else if ("newest".equals(sortOption)) {
                filtered.sort(Comparator.comparing(SanPham::getId).reversed());
            }
        }

        // 4. Pagination
        int totalItems = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalItems / (double) PAGE_SIZE));
        safePage = Math.min(safePage, totalPages - 1); // Avoid out of bounds
        if (safePage < 0) safePage = 0;
        
        int fromIndex = Math.min(safePage * PAGE_SIZE, totalItems);
        int toIndex = Math.min(fromIndex + PAGE_SIZE, totalItems);
        List<SanPham> paginatedSanPhams = filtered.subList(fromIndex, toIndex);

        if (hasText(keyword) && totalItems > 0) {
            filterLabel = "Kết quả tìm kiếm";
        }

        model.addAttribute("sanPhams", paginatedSanPhams);
        model.addAttribute("danhMucs", danhMucs);
        model.addAttribute("hangSanXuats", hangSanXuats);
        model.addAttribute("selectedDanhMucId", resolvedDanhMucId);
        model.addAttribute("selectedHangSanXuatId", resolvedHangSanXuatId);
        model.addAttribute("keyword", keyword);
        model.addAttribute("filterLabel", filterLabel);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalItems", totalItems);
        model.addAttribute("hasPrevious", safePage > 0);
        model.addAttribute("hasNext", safePage + 1 < totalPages);
        model.addAttribute("pageTitle", hasText(keyword) ? "Tìm kiếm sản phẩm" : "Sản phẩm");
        
        // Pass filter states to view
        model.addAttribute("minPrice", minPrice);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("luuTruGbs", luuTruGbs != null ? luuTruGbs : new ArrayList<>());
        model.addAttribute("mauSacs", mauSacs != null ? mauSacs : new ArrayList<>());
        model.addAttribute("sortOption", sortOption);
        model.addAttribute("giaSauKhuyenMaiTheoSanPham", giaSauKhuyenMaiTheoSanPham);

        // ── Thêm dữ liệu Đánh giá và Yêu thích ──
        Map<Integer, Double> ratingTbTheoSp = new HashMap<>();
        Map<Integer, Long> ratingCountTheoSp = new HashMap<>();
        Set<Integer> sanPhamYeuThichIds = new HashSet<>();

        if (!paginatedSanPhams.isEmpty()) {
            List<Integer> spIds = paginatedSanPhams.stream().map(SanPham::getId).collect(Collectors.toList());
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

        return "san-pham/danh-sach";
    }


    @GetMapping("/san-pham/{id}")
    public String chiTietSanPham(@PathVariable Integer id, Model model) {
        SanPham sanPham = sanPhamService.layTheoId(id);
        if (!"dang_ban".equals(sanPham.getTrangThai())) {
            throw new EntityNotFoundException("Sản phẩm không còn được kinh doanh.");
        }

        List<BienTheSanPham> bienTheSanPhams = bienTheSanPhamService.layTheoSanPhamId(id);
        Map<Integer, Integer> tonKhoTheoBienThe = layTonkhoTongTheoBienThe(bienTheSanPhams);
        Map<Integer, Integer> soLuongCoTheBanTheoBienThe = tonKhoTheoBienThe.entrySet()
                .stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue)); // tồn kho thực tế = số lượng có thể mua (§3.1)

        // ── Tính giá sau khuyến mãi cho từng biến thể (Flash Sale và Giảm trực tiếp) ──
        Map<Integer, BigDecimal> giaSauKhuyenMaiTheoBienThe = new HashMap<>();
        for (BienTheSanPham bt : bienTheSanPhams) {
            // Truyền tongTienHang = null vì chưa có tổng đơn, chỉ áp dụng Flash Sale và Giảm trực tiếp
            BigDecimal giaSauKM = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
            giaSauKhuyenMaiTheoBienThe.put(bt.getId(), giaSauKM != null ? giaSauKM : bt.getGiaBan());
        }

        // ── Tạo danh sách JSON để frontend xử lý động ──
        List<Map<String, Object>> variantsJson = new java.util.ArrayList<>();
        for (BienTheSanPham bt : bienTheSanPhams) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bt.getId());
            map.put("sku", bt.getMaSku());
            map.put("ramGb", bt.getRamGb());
            map.put("luuTruGb", bt.getLuuTruGb());
            String ramStr = bt.getRamGb() != null ? bt.getRamGb() + "GB" : "0GB";
            String romStr = formatDungLuong(bt.getLuuTruGb());
            map.put("versionKey", ramStr + " - " + romStr);
            map.put("mauSac", bt.getMauSac());
            map.put("maMauHex", bt.getMaMauHex() != null ? bt.getMaMauHex() : "#e5e7eb");
            map.put("giaBan", bt.getGiaBan());
            map.put("giaSauKM", giaSauKhuyenMaiTheoBienThe.getOrDefault(bt.getId(), bt.getGiaBan()));
            map.put("coTheBan", soLuongCoTheBanTheoBienThe.getOrDefault(bt.getId(), 0));
            map.put("trangThai", bt.getTrangThai());
            
            List<String> hinhAnhs = new java.util.ArrayList<>();
            if (bt.getHinhAnhSanPhams() != null && !bt.getHinhAnhSanPhams().isEmpty()) {
                for (org.example.primemobile.entity.HinhAnhSanPham ha : bt.getHinhAnhSanPhams()) {
                    hinhAnhs.add(ha.getDuongDan());
                }
            }
            map.put("hinhAnhs", hinhAnhs);
            variantsJson.add(map);
        }
        
        model.addAttribute("sanPham", sanPham);
        model.addAttribute("bienTheSanPhams", bienTheSanPhams);
        model.addAttribute("tonKhoTheoBienThe", tonKhoTheoBienThe);
        model.addAttribute("soLuongCoTheBanTheoBienThe", soLuongCoTheBanTheoBienThe);
        // tonKhoToiThieuDeBan đã bị xóa — không còn Safety Stock (§3.1)
        
        List<org.example.primemobile.entity.ThongSoKyThuat> thongSoKyThuats = thongSoKyThuatService.layTheoSanPham(id);
        java.util.Map<String, java.util.List<org.example.primemobile.entity.ThongSoKyThuat>> thongSoKyThuatGrouped = new java.util.LinkedHashMap<>();
        for (org.example.primemobile.entity.ThongSoKyThuat ts : thongSoKyThuats) {
            String nhom = (ts.getNhom() != null && !ts.getNhom().trim().isEmpty()) ? ts.getNhom() : "Thông tin chung";
            thongSoKyThuatGrouped.computeIfAbsent(nhom, k -> new java.util.ArrayList<>()).add(ts);
        }
        
        model.addAttribute("thongSoKyThuats", thongSoKyThuats);
        model.addAttribute("thongSoKyThuatGrouped", thongSoKyThuatGrouped);
        model.addAttribute("giaSauKhuyenMaiTheoBienThe", giaSauKhuyenMaiTheoBienThe); 
        model.addAttribute("variantsJson", variantsJson); 
        model.addAttribute("pageTitle", sanPham.getTenSanPham());
        return "san-pham/chi-tiet";
    }

    private Map<Integer, Integer> layTonkhoTongTheoBienThe(List<BienTheSanPham> bienTheSanPhams) {
        List<Integer> ids = bienTheSanPhams.stream()
                .map(BienTheSanPham::getId)
                .toList();
        if (ids.isEmpty()) {
            return Map.of();
        }

        return tonKhoRepository.tongTonKhoTheoBienTheIds(KHO_ID, ids)
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

    private String formatDungLuong(Integer gb) {
        if (gb == null) return "";
        if (gb >= 1024 && gb % 1024 == 0) {
            return (gb / 1024) + "TB";
        }
        return gb + "GB";
    }
}
