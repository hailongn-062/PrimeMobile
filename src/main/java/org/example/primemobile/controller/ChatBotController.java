package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.repository.SanPhamRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
        
        String aiReply = chatbotService.guiTinNhan(cuocHoiThoai.getId(), rawMessage);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("reply", aiReply);
        
        String message = normalize(rawMessage);
        response.put("products", recommendProducts(message));
        response.put("quickReplies", quickReplies(message));
        return ResponseEntity.ok(response);
    }

    private List<Map<String, Object>> recommendProducts(String message) {
        BigDecimal maxBudget = extractBudget(message);
        String brand = extractBrand(message);
        boolean shoppingIntent = maxBudget != null || brand != null
                || containsAny(message, "camera", "chup anh", "anh dep", "pin", "choi game", "gaming", "man hinh");

        if (!shoppingIntent) {
            return List.of();
        }

        return sanPhamRepository
                .timKiemSanPhamPublic(null, null, null, PageRequest.of(0, 24, Sort.by(Sort.Direction.DESC, "ngayTao")))
                .getContent()
                .stream()
                .filter(product -> matchesBrand(product, brand))
                .map(product -> new java.util.AbstractMap.SimpleEntry<SanPham, Optional<BigDecimal>>(product, minPrice(product)))
                .filter(entry -> entry.getValue().isPresent())
                .filter(entry -> maxBudget == null || entry.getValue().get().compareTo(maxBudget) <= 0)
                .sorted(Comparator.comparing(entry -> entry.getValue().get()))
                .limit(4)
                .map(entry -> productDto(entry.getKey(), entry.getValue().get()))
                .toList();
    }

    private boolean matchesBrand(SanPham product, String brand) {
        if (brand == null) {
            return true;
        }
        String productBrand = product.getHangSanXuat() != null ? normalize(product.getHangSanXuat().getTenHang()) : "";
        String productName = normalize(product.getTenSanPham());
        return productBrand.contains(brand) || productName.contains(brand);
    }

    private Map<String, Object> productDto(SanPham product, BigDecimal price) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", product.getId());
        dto.put("name", product.getTenSanPham());
        dto.put("brand", product.getHangSanXuat() != null ? product.getHangSanXuat().getTenHang() : "PrimeMobile");
        dto.put("price", price);
        dto.put("url", "/san-pham/" + product.getId());
        return dto;
    }

    private Optional<BigDecimal> minPrice(SanPham product) {
        if (product.getBienTheSanPhams() == null) {
            return Optional.empty();
        }
        return product.getBienTheSanPhams().stream()
                .filter(variant -> "con_hang".equals(variant.getTrangThai()))
                .map(BienTheSanPham::getGiaBan)
                .filter(price -> price != null && price.compareTo(BigDecimal.ZERO) > 0)
                .min(BigDecimal::compareTo);
    }

    private BigDecimal extractBudget(String message) {
        Matcher matcher = Pattern.compile("(duoi|tam|khoang|toi da)?\\s*(\\d{1,3})\\s*(trieu|tr)").matcher(message);
        if (matcher.find()) {
            return BigDecimal.valueOf(Long.parseLong(matcher.group(2))).multiply(BigDecimal.valueOf(1_000_000L));
        }
        if (containsAny(message, "re", "gia tot", "tiet kiem")) {
            return BigDecimal.valueOf(10_000_000L);
        }
        return null;
    }

    private String extractBrand(String message) {
        if (message.contains("iphone") || message.contains("apple")) return "apple";
        if (message.contains("samsung")) return "samsung";
        if (message.contains("xiaomi") || message.contains("redmi")) return "xiaomi";
        if (message.contains("oppo")) return "oppo";
        if (message.contains("vivo")) return "vivo";
        return null;
    }

    private List<String> quickReplies(String message) {
        if (containsAny(message, "bao hanh", "doi tra", "dat hang")) {
            return List.of("iPhone dưới 30 triệu", "Máy giá tốt", "Liên hệ cửa hàng");
        }
        return List.of("Máy dưới 10 triệu", "iPhone", "Samsung camera đẹp", "Cách đặt hàng");
    }

    private boolean containsAny(String message, String... words) {
        for (String word : words) {
            if (message.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String normalized = Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
                .replace("đ", "d")
                .replace("Đ", "D")
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).trim();
    }
}
