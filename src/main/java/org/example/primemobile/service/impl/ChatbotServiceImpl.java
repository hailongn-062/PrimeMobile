package org.example.primemobile.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.primemobile.entity.CuocHoiThoai;
import org.example.primemobile.entity.KhachHang;
import org.example.primemobile.entity.TinNhanChat;
import org.example.primemobile.repository.CuocHoiThoaiRepository;
import org.example.primemobile.repository.KhachHangRepository;
import org.example.primemobile.repository.TinNhanChatRepository;
import org.example.primemobile.service.IChatbotService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ChatbotServiceImpl implements IChatbotService {

    private final CuocHoiThoaiRepository cuocHoiThoaiRepository;
    private final TinNhanChatRepository tinNhanChatRepository;
    private final KhachHangRepository khachHangRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    @Value("${gemini.api.url}")
    private String geminiApiUrl;

    public ChatbotServiceImpl(CuocHoiThoaiRepository cuocHoiThoaiRepository, 
                              TinNhanChatRepository tinNhanChatRepository,
                              KhachHangRepository khachHangRepository) {
        this.cuocHoiThoaiRepository = cuocHoiThoaiRepository;
        this.tinNhanChatRepository = tinNhanChatRepository;
        this.khachHangRepository = khachHangRepository;
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
    public String guiTinNhan(Integer cuocHoiThoaiId, String message) {
        CuocHoiThoai chat = cuocHoiThoaiRepository.findById(cuocHoiThoaiId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hội thoại"));
        
        // 1. Lưu tin nhắn User
        TinNhanChat userMsg = new TinNhanChat();
        userMsg.setCuocHoiThoai(chat);
        userMsg.setNoiDung(message);
        userMsg.setVai("user");
        tinNhanChatRepository.save(userMsg);

        // 2. Lấy lịch sử 10 tin nhắn gần nhất làm context
        List<TinNhanChat> history = tinNhanChatRepository.findByCuocHoiThoaiIdOrderByThoiGianAsc(cuocHoiThoaiId);
        int startIndex = Math.max(0, history.size() - 10);
        List<TinNhanChat> recentHistory = history.subList(startIndex, history.size());

        // 3. Gọi Gemini
        String aiResponse = callGemini(recentHistory);

        // 4. Lưu tin nhắn Model
        TinNhanChat modelMsg = new TinNhanChat();
        modelMsg.setCuocHoiThoai(chat);
        modelMsg.setNoiDung(aiResponse);
        modelMsg.setVai("model");
        tinNhanChatRepository.save(modelMsg);
        
        // Cập nhật updatedAt
        chat.setUpdatedAt(java.time.LocalDateTime.now());
        cuocHoiThoaiRepository.save(chat);

        return aiResponse;
    }

    private String callGemini(List<TinNhanChat> history) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            
            // System instructions
            ObjectNode systemInstruction = objectMapper.createObjectNode();
            systemInstruction.put("role", "system");
            ArrayNode sysParts = systemInstruction.putArray("parts");
            ObjectNode sysPart = objectMapper.createObjectNode();
            sysPart.put("text", "Bạn là trợ lý ảo của PrimeMobile - Hệ thống bán lẻ điện thoại di động chính hãng tại Việt Nam. Bạn luôn lịch sự, thân thiện và trả lời chuyên nghiệp. Nếu không biết thì trả lời không biết, tuyệt đối không bịa đặt thông tin. Tên bạn là PrimeBot. Bạn chỉ trả lời bằng chữ, không trả lời kèm JSON. Nếu câu trả lời có tính định dạng, dùng Markdown cơ bản.");
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
                            return textNode.asText();
                        }
                    }
                    return "Xin lỗi, hiện tại tôi đang gặp chút vấn đề kết nối. Bạn vui lòng thử lại sau nhé!";
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
                            return "Xin lỗi, AI đang quá tải sau nhiều lần thử. Vui lòng quay lại sau vài phút nhé!";
                        }
                        return "Lỗi kết nối AI (" + e.getStatusCode() + "). Vui lòng thử lại sau.";
                    }
                } catch (Exception e) {
                    System.err.println("[Chatbot] Lỗi không xác định: " + e.getMessage());
                    return "Xin lỗi, hệ thống AI đang bảo trì. Vui lòng thử lại sau.";
                }
            }
            return "Xin lỗi, hệ thống AI hiện không phản hồi. Vui lòng thử lại sau.";
        } catch (Exception e) {
            System.err.println("[Chatbot] Lỗi xử lý JSON hoặc Dữ liệu: " + e.getMessage());
            return "Xin lỗi, hệ thống AI đang bảo trì nội bộ. Vui lòng thử lại sau.";
        }
    }
}
