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

    @PostMapping({"", "/ask"})
    @Transactional(readOnly = true)
    public ResponseEntity<?> tuVan(@RequestBody Map<String, String> request) {
        String rawMessage = request.getOrDefault("message", "");
        String message = normalize(rawMessage);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", true);
        response.put("reply", buildReply(message));
        response.put("products", recommendProducts(message));
        response.put("quickReplies", quickReplies(message));
        return ResponseEntity.ok(response);
    }

    private String buildReply(String message) {
        if (message.isBlank() || containsAny(message, "xin chao", "hello", "chao")) {
            return "Chào bạn, mình là trợ lý PrimeMobile. Bạn có thể hỏi theo hãng, ngân sách hoặc nhu cầu như camera, pin, chơi game.";
        }
        if (containsAny(message, "bao hanh", "warranty")) {
            return "Sản phẩm chính hãng được bảo hành theo chính sách hãng. Bạn có thể xem chi tiết tại trang Chính sách bảo hành hoặc liên hệ hotline 1900 1234.";
        }
        if (containsAny(message, "doi tra", "tra hang", "hoan hang")) {
            return "PrimeMobile hỗ trợ đổi trả khi sản phẩm đủ điều kiện chính sách. Máy cần còn đầy đủ hộp, phụ kiện, hóa đơn và không hư hỏng do người dùng.";
        }
        if (containsAny(message, "dat hang", "mua hang", "thanh toan", "checkout")) {
            return "Bạn chọn sản phẩm, thêm vào giỏ, đăng nhập, chọn địa chỉ nhận hàng rồi bấm đặt hàng. Sau đó có thể theo dõi tại mục Đơn hàng.";
        }
        if (containsAny(message, "gio hang", "cart")) {
            return "Bạn có thể xem sản phẩm đã chọn tại Giỏ hàng, chỉnh số lượng rồi chuyển sang thanh toán khi đã sẵn sàng.";
        }
        if (containsAny(message, "lien he", "hotline", "dia chi")) {
            return "PrimeMobile hỗ trợ qua hotline 1900 1234, email support@primemobile.vn hoặc tại 123 Đường Láng, Đống Đa, Hà Nội.";
        }
        if (containsAny(message, "re", "gia tot", "duoi", "tam", "trieu", "ngan sach")) {
            return "Mình đã lọc vài mẫu phù hợp ngân sách cho bạn. Bạn có thể bấm xem chi tiết để chọn màu và dung lượng.";
        }
        if (containsAny(message, "camera", "chup anh", "anh dep")) {
            return "Nếu ưu tiên camera, bạn nên xem các dòng cao cấp như iPhone Pro Max, Galaxy S Ultra hoặc Xiaomi flagship.";
        }
        if (containsAny(message, "pin", "choi game", "gaming", "man hinh")) {
            return "Nếu cần pin tốt, màn hình lớn hoặc chơi game, bạn nên ưu tiên máy có RAM cao, pin lớn và dòng hiệu năng tốt.";
        }
        if (containsAny(message, "iphone", "apple", "samsung", "xiaomi", "oppo", "vivo")) {
            return "Mình đã tìm các mẫu theo hãng bạn quan tâm. Bạn có thể bấm xem chi tiết để kiểm tra giá và cấu hình.";
        }
        return "Mình có thể tư vấn theo hãng, tầm giá hoặc nhu cầu. Ví dụ: \"iPhone dưới 30 triệu\", \"máy pin tốt\", \"Samsung camera đẹp\".";
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
                .timKiemSanPhamPublic(null, null, PageRequest.of(0, 24, Sort.by(Sort.Direction.DESC, "ngayTao")))
                .getContent()
                .stream()
                .filter(product -> matchesBrand(product, brand))
                .map(product -> Map.entry(product, minPrice(product)))
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
