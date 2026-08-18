package org.example.primemobile.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.primemobile.entity.CuocHoiThoai;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.entity.TinNhanChat;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.repository.CuocHoiThoaiRepository;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.repository.TinNhanChatRepository;
import org.example.primemobile.repository.SanPhamRepository;
import org.example.primemobile.service.IChatbotService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.entity.TonKho;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatbotServiceImpl implements IChatbotService {

    private final CuocHoiThoaiRepository cuocHoiThoaiRepository;
    private final TinNhanChatRepository tinNhanChatRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamRepository sanPhamRepository;
    private final TonKhoRepository tonKhoRepository;
    private final IKhuyenMaiService khuyenMaiService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public ChatbotServiceImpl(CuocHoiThoaiRepository cuocHoiThoaiRepository, 
                              TinNhanChatRepository tinNhanChatRepository,
                              KhachHangRepository khachHangRepository,
                              SanPhamRepository sanPhamRepository,
                              TonKhoRepository tonKhoRepository,
                              IKhuyenMaiService khuyenMaiService) {
        this.cuocHoiThoaiRepository = cuocHoiThoaiRepository;
        this.tinNhanChatRepository = tinNhanChatRepository;
        this.khachHangRepository = khachHangRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.tonKhoRepository = tonKhoRepository;
        this.khuyenMaiService = khuyenMaiService;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public CuocHoiThoai layHoacTaoCuocHoiThoai(Integer khachHangId, String sessionId) {
        Optional<CuocHoiThoai> existing;
        if (khachHangId != null) {
            List<CuocHoiThoai> list = cuocHoiThoaiRepository.findByKhachHangIdOrderByUpdatedAtDesc(khachHangId);
            existing = list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        } else {
            existing = cuocHoiThoaiRepository.findFirstBySessionIdOrderByUpdatedAtDesc(sessionId);
        }

        if (existing.isPresent()) {
            return existing.get();
        }

        CuocHoiThoai newChat = new CuocHoiThoai();
        if (khachHangId != null) {
            newChat.setKhachHangId(khachHangId);
        }
        newChat.setSessionId(sessionId);
        return cuocHoiThoaiRepository.save(newChat);
    }

    @Override
    public List<TinNhanChat> layLichSuTinNhan(Integer cuocHoiThoaiId) {
        return tinNhanChatRepository.findByCuocHoiThoaiIdOrderByThoiGianAsc(cuocHoiThoaiId);
    }

    @Override
    public org.example.primemobile.dto.ChatbotResponseDto guiTinNhan(Integer cuocHoiThoaiId, String message, Integer khachHangId) {
        CuocHoiThoai chat = cuocHoiThoaiRepository.findById(cuocHoiThoaiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội thoại"));
        
        // 1. Lưu tin nhắn User
        TinNhanChat userMsg = new TinNhanChat();
        userMsg.setCuocHoiThoai(chat);
        userMsg.setNoiDung(message);
        userMsg.setVai("user");
        tinNhanChatRepository.save(userMsg);

        // 2. Lấy lịch sử 20 tin nhắn gần nhất (10 cặp) làm context
        List<TinNhanChat> history = tinNhanChatRepository.findTop20ByCuocHoiThoaiIdOrderByThoiGianDesc(cuocHoiThoaiId);
        // Đảo ngược list để các tin cũ hơn đứng trước
        java.util.Collections.reverse(history);

        // 3. Lấy thông tin khách hàng nếu có
        KhachHang kh = null;
        if (khachHangId != null) {
            kh = khachHangRepository.findById(khachHangId).orElse(null);
        }

        // 4. Lấy dữ liệu sản phẩm để đưa vào context
        String productContext = buildProductContext();
        System.out.println("=== PRODUCT CONTEXT ===");
        System.out.println(productContext);
        System.out.println("=======================");

        // 5. Gọi Gemini
        org.example.primemobile.dto.ChatbotResponseDto aiResponse = callGemini(history, kh, productContext);

        // 6. Lưu tin nhắn Model
        TinNhanChat modelMsg = new TinNhanChat();
        modelMsg.setCuocHoiThoai(chat);
        modelMsg.setNoiDung(aiResponse.getReply());
        modelMsg.setVai("model");
        tinNhanChatRepository.save(modelMsg);
        
        // Cập nhật updatedAt
        chat.setUpdatedAt(java.time.LocalDateTime.now());
        cuocHoiThoaiRepository.save(chat);

        // 7. Xoá tin nhắn cũ
        tinNhanChatRepository.xoaTinNhanCuNhat(cuocHoiThoaiId);

        return aiResponse;
    }

    private String buildProductContext() {
        StringBuilder sb = new StringBuilder();
        sb.append("[Dữ liệu sản phẩm hiện có (Giá hiển thị là giá đã giảm nếu có khuyến mãi)]\n");
        List<SanPham> sanPhams = sanPhamRepository.findAll();
        for (SanPham sp : sanPhams) {
            if (!"dang_ban".equals(sp.getTrangThai())) continue;
            
            BigDecimal minPrice = null;
            if (sp.getBienTheSanPhams() != null) {
                for (BienTheSanPham bt : sp.getBienTheSanPhams()) {
                    // Thay vì dùng trạng thái, ta tính tổng số lượng tồn kho thực tế
                    long totalStock = tonKhoRepository.findByBienTheSanPham(bt).stream()
                            .mapToLong(TonKho::getSoLuong)
                            .sum();
                    if (totalStock <= 0) continue;
                    
                    BigDecimal price = khuyenMaiService.tinhGiaSauKhuyenMai(bt.getId(), null);
                    if (minPrice == null || price.compareTo(minPrice) < 0) {
                        minPrice = price;
                    }
                }
            }
            if (minPrice != null) {
                sb.append("- [ID:").append(sp.getId()).append("] ").append(sp.getTenSanPham())
                  .append(" (Hãng: ").append(sp.getHangSanXuat().getTenHang()).append(")")
                  .append(": từ ").append(String.format("%,.0f", minPrice)).append("đ\n");
            }
        }
        return sb.toString();
    }

    private org.example.primemobile.dto.ChatbotResponseDto callGemini(List<TinNhanChat> history, KhachHang khachHang, String productContext) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // System instructions
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            systemInstruction.put("role", "system");
            ArrayNode sysParts = systemInstruction.putArray("parts");
            ObjectNode sysPart = objectMapper.createObjectNode();
            
            StringBuilder sysPrompt = new StringBuilder();
            sysPrompt.append("Bạn là trợ lý ảo của PrimeMobile - Hệ thống bán lẻ điện thoại di động chính hãng tại Việt Nam.\n");
            sysPrompt.append("Tên bạn là PrimeBot.\n");
            sysPrompt.append("QUY TẮC QUAN TRỌNG:\n");
            sysPrompt.append("1. LUÔN trả lời thật NGẮN GỌN (tối đa 3-4 câu). KHÔNG liệt kê dài dòng. Trả lời trực tiếp vào trọng tâm.\n");
            sysPrompt.append("2. TUYỆT ĐỐI TIN TƯỞNG vào DỮ LIỆU SẢN PHẨM bên dưới. Dù tên sản phẩm có vẻ vô lý (như IPhone 100) hay giá cực cao (100 triệu), BẠN BẮT BUỘC PHẢI TƯ VẤN nếu nó có trong danh sách. Không được dùng kiến thức bên ngoài để từ chối!\n");
            sysPrompt.append("3. Nếu khách hỏi máy 100 triệu, và trong danh sách có máy giá đó, bạn PHẢI TÌM VÀ GỢI Ý MÁY ĐÓ. TUYỆT ĐỐI KHÔNG BỊA ĐẶT GIÁ HOẶC NÓI KHÔNG CÓ.\n\n");
            
            sysPrompt.append("BẠN PHẢI TRẢ LỜI ĐÚNG ĐỊNH DẠNG JSON NHƯ SAU:\n");
            sysPrompt.append("{\n");
            sysPrompt.append("  \"reply\": \"câu trả lời của bạn\",\n");
            sysPrompt.append("  \"productIds\": [1, 2, 3] // Danh sách ID sản phẩm muốn gợi ý (từ danh sách bên dưới). Trống nếu không gợi ý.\n");
            sysPrompt.append("}\n\n");

            sysPrompt.append(productContext).append("\n");
            
            if (khachHang != null) {
                sysPrompt.append("\n[Thông tin Khách hàng đang chat]\n");
                sysPrompt.append("- Họ tên: ").append(khachHang.getHoTen()).append("\n");
                if (khachHang.getSoDienThoai() != null) sysPrompt.append("- SĐT: ").append(khachHang.getSoDienThoai()).append("\n");
                if (khachHang.getEmail() != null) sysPrompt.append("- Email: ").append(khachHang.getEmail()).append("\n");
                sysPrompt.append("Dùng thông tin này nếu khách hỏi về tài khoản của họ.\n");
            }
            
            sysPart.put("text", sysPrompt.toString());
            sysParts.add(sysPart);
            requestBody.set("systemInstruction", systemInstruction);

            // Contents
            ArrayNode contents = requestBody.putArray("contents");
            ObjectNode currentContent = null;
            ArrayNode currentParts = null;
            String lastRole = null;

            for (TinNhanChat msg : history) {
                String role = msg.getVai() != null ? msg.getVai() : "user";
                if ("system".equals(role)) continue;
                
                // Gemini yêu cầu hội thoại phải bắt đầu bằng 'user'
                if (lastRole == null && "model".equals(role)) {
                    continue; 
                }
                
                // Gemini yêu cầu luân phiên role. Nếu trùng role, gộp nội dung lại
                if (role.equals(lastRole) && currentParts != null) {
                    ObjectNode part = objectMapper.createObjectNode();
                    part.put("text", "\n" + msg.getNoiDung());
                    currentParts.add(part);
                } else {
                    currentContent = objectMapper.createObjectNode();
                    currentContent.put("role", role);
                    currentParts = currentContent.putArray("parts");
                    ObjectNode part = objectMapper.createObjectNode();
                    part.put("text", msg.getNoiDung());
                    currentParts.add(part);
                    contents.add(currentContent);
                    lastRole = role;
                }
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(requestBody.toString(), headers);
            String url = geminiApiUrl.trim() + "?key=" + geminiApiKey.trim();

            int maxRetries = 3;
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        JsonNode rootNode = objectMapper.readTree(response.getBody());
                        JsonNode candidates = rootNode.path("candidates");
                        if (candidates.isArray() && candidates.size() > 0) {
                            JsonNode textNode = candidates.get(0).path("content").path("parts").get(0).path("text");
                            String rawText = textNode.asText();
                            
                            // Xóa markdown block ```json ... ``` nếu có
                            if (rawText.startsWith("```json")) {
                                rawText = rawText.substring(7);
                            } else if (rawText.startsWith("```")) {
                                rawText = rawText.substring(3);
                            }
                            if (rawText.endsWith("```")) {
                                rawText = rawText.substring(0, rawText.length() - 3);
                            }
                            
                            try {
                                JsonNode responseJson = objectMapper.readTree(rawText.trim());
                                String reply = responseJson.has("reply") ? responseJson.get("reply").asText() : "";
                                
                                java.util.List<Integer> productIds = new java.util.ArrayList<>();
                                if (responseJson.has("productIds") && responseJson.get("productIds").isArray()) {
                                    for (JsonNode idNode : responseJson.get("productIds")) {
                                        productIds.add(idNode.asInt());
                                    }
                                }
                                
                                return org.example.primemobile.dto.ChatbotResponseDto.builder()
                                        .reply(reply)
                                        .productIds(productIds)
                                        .build();
                            } catch (Exception parseEx) {
                                System.err.println("Gemini trả về không phải JSON: " + rawText);
                                return org.example.primemobile.dto.ChatbotResponseDto.builder()
                                        .reply(rawText.trim())
                                        .productIds(java.util.Collections.emptyList())
                                        .build();
                            }
                        }
                    }
                    return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Xin lỗi, hiện tại tôi đang gặp chút vấn đề kết nối. Bạn vui lòng thử lại sau nhé!").build();
                } catch (org.springframework.web.client.HttpStatusCodeException e) {
                    boolean shouldRetry = e.getStatusCode().is5xxServerError() || e.getStatusCode().value() == 429;
                    if (shouldRetry && attempt < maxRetries) {
                        System.err.println("[Chatbot] Lỗi " + e.getStatusCode() + " từ Gemini API. Thử lại lần " + attempt + "/" + maxRetries + " sau 2s...");
                        try {
                            Thread.sleep(2000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    } else {
                        System.err.println("[Chatbot] Gemini API Error (" + e.getStatusCode() + "): " + e.getResponseBodyAsString());
                        if (shouldRetry) {
                            return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Xin lỗi, AI đang quá tải sau nhiều lần thử. Vui lòng quay lại sau vài phút nhé!").build();
                        }
                        return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Lỗi kết nối AI (" + e.getStatusCode() + "). Vui lòng thử lại sau.").build();
                    }
                } catch (Exception e) {
                    System.err.println("[Chatbot] Lỗi không xác định: " + e.getMessage());
                    return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Xin lỗi, hệ thống AI đang bảo trì. Vui lòng thử lại sau.").build();
                }
            }
            return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Xin lỗi, hệ thống AI hiện không phản hồi. Vui lòng thử lại sau.").build();
        } catch (Exception e) {
            System.err.println("[Chatbot] Lỗi xử lý JSON hoặc Dữ liệu: " + e.getMessage());
            return org.example.primemobile.dto.ChatbotResponseDto.builder().reply("Xin lỗi, hệ thống AI đang bảo trì nội bộ. Vui lòng thử lại sau.").build();
        }
    }
}
