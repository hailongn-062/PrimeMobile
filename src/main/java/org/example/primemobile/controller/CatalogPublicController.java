package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.DanhMuc;
import org.example.primemobile.entity.HangSanXuat;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.ThongSoKyThuat;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.example.primemobile.service.IDanhMucService;
import org.example.primemobile.service.IHangSanXuatService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.service.ISanPhamService;
import org.example.primemobile.service.IThongSoKyThuatService;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/catalog")
@RequiredArgsConstructor
public class CatalogPublicController {

    private static final Logger log = LoggerFactory.getLogger(CatalogPublicController.class);

    private final ISanPhamService sanPhamService;
    private final IBienTheSanPhamService bienTheSanPhamService;
    private final IThongSoKyThuatService thongSoKyThuatService;
    private final IDanhMucService danhMucService;
    private final IHangSanXuatService hangSanXuatService;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;
    private final IKhuyenMaiService khuyenMaiService;

    @GetMapping("/san-pham")
    public ResponseEntity<?> layDanhSachSanPham(
            @RequestParam(required = false) Integer danhMucId,
            @RequestParam(required = false) Integer hangSanXuatId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "ngayTao,desc") String sort) {

        log.debug("[CatalogPublic] GET /san-pham danhMucId={}, hangSanXuatId={}, page={}, size={}",
                danhMucId, hangSanXuatId, page, size);

        Pageable pageable = buildPageable(page, size, sort);
        return ResponseEntity.ok(sanPhamService
                .layDanhSachCongKhai(null, danhMucId, hangSanXuatId, pageable)
                .map(this::productDto));
    }

    @GetMapping("/san-pham/{id}")
    public ResponseEntity<?> layChiTietSanPham(@PathVariable Integer id) {
        log.debug("[CatalogPublic] GET /san-pham/{}", id);
        try {
            SanPham sanPham = sanPhamService.layTheoId(id);
            if ("ngung_ban".equals(sanPham.getTrangThai())) {
                return ResponseEntity.status(404).body(Map.of("message", "Sản phẩm đã ngừng kinh doanh"));
            }
            Map<String, Object> dto = productDto(sanPham);
            dto.put("bienTheSanPhams", bienTheSanPhamService.layTheoSanPhamId(id).stream()
                    .map(this::variantDto)
                    .toList());
            dto.put("thongSoKyThuats", thongSoKyThuatService.layTheoSanPham(id).stream()
                    .map(this::specDto)
                    .toList());
            return ResponseEntity.ok(dto);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bien-the/san-pham/{sanPhamId}")
    public ResponseEntity<?> layBienTheCuaSanPham(@PathVariable Integer sanPhamId) {
        log.debug("[CatalogPublic] GET /bien-the/san-pham/{}", sanPhamId);
        try {
            SanPham sanPham = sanPhamService.layTheoId(sanPhamId);
            if ("ngung_ban".equals(sanPham.getTrangThai())) {
                return ResponseEntity.status(404).body(Map.of("message", "Sản phẩm đã ngừng kinh doanh"));
            }
            return ResponseEntity.ok(bienTheSanPhamService.layTheoSanPhamId(sanPhamId).stream()
                    .map(this::variantDto)
                    .toList());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/bien-the/{id}")
    public ResponseEntity<?> layChiTietBienThe(@PathVariable Integer id) {
        log.debug("[CatalogPublic] GET /bien-the/{}", id);
        try {
            return ResponseEntity.ok(variantDto(bienTheSanPhamService.getBienTheSanPham(id)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/thong-so/{sanPhamId}")
    public ResponseEntity<?> layThongSoKyThuat(@PathVariable Integer sanPhamId) {
        log.debug("[CatalogPublic] GET /thong-so/{}", sanPhamId);
        try {
            return ResponseEntity.ok(thongSoKyThuatService.layTheoSanPham(sanPhamId).stream()
                    .map(this::specDto)
                    .toList());
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/danh-muc")
    public ResponseEntity<?> layDanhMucKichHoat() {
        log.debug("[CatalogPublic] GET /danh-muc");
        return ResponseEntity.ok(danhMucService.layDanhSachKichHoat().stream()
                .map(this::categoryDto)
                .toList());
    }

    @GetMapping("/hang-san-xuat")
    public ResponseEntity<?> layDanhSachHangSanXuat() {
        log.debug("[CatalogPublic] GET /hang-san-xuat");
        return ResponseEntity.ok(hangSanXuatService.layTatCa().stream()
                .map(this::brandDto)
                .toList());
    }

    @GetMapping("/tim-kiem-nhanh")
    public ResponseEntity<?> timKiemNhanh(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        List<BienTheSanPham> results = bienTheSanPhamRepository.searchBienTheSanPham(keyword, PageRequest.of(0, 8));
        List<Map<String, Object>> dto = results.stream().map(b -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", b.getSanPham().getId());
            map.put("tenSanPham", b.getSanPham().getTenSanPham() + " " + b.getRamGb() + "GB " + b.getLuuTruGb() + "GB");
            map.put("giaBan", b.getGiaBan());
            map.put("giaSauKhuyenMai", khuyenMaiService.tinhGiaSauKhuyenMai(b.getId(), null));
            if (b.getHinhAnhSanPhams() != null && !b.getHinhAnhSanPhams().isEmpty()) {
                map.put("hinhAnh", b.getHinhAnhSanPhams().get(0).getDuongDan());
            } else {
                map.put("hinhAnh", "/images/no-image.png");
            }
            return map;
        }).toList();
        return ResponseEntity.ok(dto);
    }

    private Pageable buildPageable(int page, int size, String sort) {
        try {
            String[] parts = sort.split(",");
            Sort.Direction direction = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(direction, parts[0].trim()));
        } catch (Exception e) {
            return PageRequest.of(Math.max(page, 0), Math.max(size, 1), Sort.by(Sort.Direction.DESC, "ngayTao"));
        }
    }

    private Map<String, Object> productDto(SanPham sanPham) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", sanPham.getId());
        dto.put("maSanPham", sanPham.getMaSanPham());
        dto.put("tenSanPham", sanPham.getTenSanPham());
        dto.put("moTaNgan", sanPham.getMoTaNgan());
        dto.put("moTaChiTiet", sanPham.getMoTaChiTiet());
        dto.put("namRaMat", sanPham.getNamRaMat());

        dto.put("trangThai", sanPham.getTrangThai());
        dto.put("luotXem", sanPham.getLuotXem());
        dto.put("ngayTao", sanPham.getNgayTao());
        dto.put("updatedAt", sanPham.getUpdatedAt());
        dto.put("danhMuc", categoryDto(sanPham.getDanhMuc()));
        dto.put("hangSanXuat", brandDto(sanPham.getHangSanXuat()));
        return dto;
    }

    private Map<String, Object> variantDto(BienTheSanPham bienThe) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", bienThe.getId());
        dto.put("sanPhamId", bienThe.getSanPham() != null ? bienThe.getSanPham().getId() : null);
        dto.put("maSku", bienThe.getMaSku());
        dto.put("mauSac", bienThe.getMauSacTen());
        dto.put("ramGb", bienThe.getRamGb());
        dto.put("luuTruGb", bienThe.getLuuTruGb());
        dto.put("loaiLuuTru", bienThe.getLoaiLuuTru());
        dto.put("giaBan", bienThe.getGiaBan());
        dto.put("trongLuongGram", bienThe.getTrongLuongGram());
        dto.put("pinMah", bienThe.getPinMah());
        dto.put("trangThai", bienThe.getTrangThai());
        return dto;
    }

    private Map<String, Object> specDto(ThongSoKyThuat thongSo) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", thongSo.getId());
        dto.put("nhom", thongSo.getNhom());
        dto.put("tenThongSo", thongSo.getTenThongSo());
        dto.put("giaTri", thongSo.getGiaTri());
        dto.put("thuTu", thongSo.getThuTu());
        return dto;
    }

    private Map<String, Object> categoryDto(DanhMuc danhMuc) {
        if (danhMuc == null) {
            return null;
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", danhMuc.getId());
        dto.put("tenDanhMuc", danhMuc.getTenDanhMuc());
        dto.put("slug", danhMuc.getSlug());
        dto.put("moTa", danhMuc.getMoTa());
        dto.put("thuTu", danhMuc.getThuTu());
        dto.put("kichHoat", danhMuc.getKichHoat());
        return dto;
    }

    private Map<String, Object> brandDto(HangSanXuat hangSanXuat) {
        if (hangSanXuat == null) {
            return null;
        }
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", hangSanXuat.getId());
        dto.put("tenHang", hangSanXuat.getTenHang());
        dto.put("logo", hangSanXuat.getLogo());
        dto.put("quocGia", hangSanXuat.getQuocGia());
        return dto;
    }
}
