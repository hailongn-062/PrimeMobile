package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.HinhAnhSanPham;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.repository.HinhAnhSanPhamRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.entity.TonKho;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/public/chatbot")
@RequiredArgsConstructor
public class ChatBotController {

    private final SanPhamRepository sanPhamRepository;
    private final org.example.primemobile.service.IChatbotService chatbotService;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final TonKhoRepository tonKhoRepository;
    private final IKhuyenMaiService khuyenMaiService;

    @PostMapping({"", "/ask"})
    @Transactional
    public ResponseEntity<?> tuVan(@RequestBody Map<String, String> requestBody, jakarta.servlet.http.HttpSession session) {
        String rawMessage = requestBody.getOrDefault("message", "");
        if (rawMessage.isBlank()) {
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("success", false);
            return ResponseEntity.ok(err);
        }

        org.example.primemobile.dto.auth.SessionKhachHang currentCustomer = 
            (org.example.primemobile.dto.auth.SessionKhachHang) session.getAttribute("CURRENT_CUSTOMER");
        Integer khachHangId = currentCustomer != null ? currentCustomer.getKhachHangId() : null;
        String sessionId = session.getId();

        org.example.primemobile.entity.CuocHoiThoai cuocHoiThoai = chatbotService.layHoacTaoCuocHoiThoai(khachHangId, sessionId);
        
        org.example.primemobile.dto.ChatbotResponseDto aiResponse = chatbotService.guiTinNhan(cuocHoiThoai.getId(), rawMessage, khachHangId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("reply", aiResponse.getReply());
        
        java.util.List<Map<String, Object>> products = new java.util.ArrayList<>();
        if (aiResponse.getProductIds() != null && !aiResponse.getProductIds().isEmpty()) {
            for (Integer pId : aiResponse.getProductIds()) {
                sanPhamRepository.findById(pId).ifPresent(product -> {
                    Optional<BienTheSanPham> variant = minVariant(product);
                    if (variant.isPresent()) {
                        BienTheSanPham bt = variant.get();
                        BigDecimal price = bt.getGiaBan();
                        BigDecimal salePrice = null;
                        try {
                            salePrice = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
                            if (salePrice != null && salePrice.compareTo(price) == 0) {
                                salePrice = null;
                            }
                        } catch (Exception e) {}
                        
                        String imageUrl = null;
                        try {
                            imageUrl = getImageUrl(bt);
                        } catch (Exception e) {}
                        
                        products.add(productDto(product, price, salePrice, imageUrl));
                    }
                });
            }
        }
        
        response.put("products", products);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> layLichSu(jakarta.servlet.http.HttpSession session) {
        org.example.primemobile.dto.auth.SessionKhachHang currentCustomer = 
            (org.example.primemobile.dto.auth.SessionKhachHang) session.getAttribute("CURRENT_CUSTOMER");
        Integer khachHangId = currentCustomer != null ? currentCustomer.getKhachHangId() : null;
        String sessionId = session.getId();

        org.example.primemobile.entity.CuocHoiThoai cuocHoiThoai = chatbotService.layHoacTaoCuocHoiThoai(khachHangId, sessionId);
        List<org.example.primemobile.entity.TinNhanChat> history = chatbotService.layLichSuTinNhan(cuocHoiThoai.getId());

        List<Map<String, Object>> messages = history.stream().map(msg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("vai", msg.getVai());
            map.put("noiDung", msg.getNoiDung());
            return map;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("messages", messages);
        return ResponseEntity.ok(response);
    }

    private Map<String, Object> productDto(SanPham product, BigDecimal price, BigDecimal salePrice, String imageUrl) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", product.getId());
        dto.put("name", product.getTenSanPham());
        dto.put("brand", product.getHangSanXuat() != null ? product.getHangSanXuat().getTenHang() : "PrimeMobile");
        dto.put("price", price);
        dto.put("salePrice", salePrice);
        dto.put("image", imageUrl);
        dto.put("url", "/san-pham/" + product.getId());
        return dto;
    }

    private Optional<BienTheSanPham> minVariant(SanPham product) {
        if (product.getBienTheSanPhams() == null) {
            return Optional.empty();
        }
        return product.getBienTheSanPhams().stream()
                .filter(variant -> {
                    long totalStock = tonKhoRepository.findByBienTheSanPham(variant).stream()
                            .mapToLong(TonKho::getSoLuong)
                            .sum();
                    return totalStock > 0;
                })
                .filter(variant -> variant.getGiaBan() != null && variant.getGiaBan().compareTo(BigDecimal.ZERO) > 0)
                .min(Comparator.comparing(BienTheSanPham::getGiaBan));
    }

    private String getImageUrl(BienTheSanPham bt) {
        List<HinhAnhSanPham> images = hinhAnhSanPhamRepository.findByBienTheSanPhamIdOrderByThuTuAsc(bt.getId());
        if (images == null || images.isEmpty()) return null;
        return images.stream()
                .filter(img -> img.getLaAnhChinh() != null && img.getLaAnhChinh())
                .findFirst()
                .orElse(images.get(0))
                .getDuongDan();
    }

}
