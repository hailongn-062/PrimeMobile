package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IKhuyenMaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller Khuyến Mãi & Mã Giảm Giá.
 *
 * <h3>Admin endpoints (yêu cầu session):</h3>
 * <pre>
 *   GET  /api/admin/khuyen-mai                          → Danh sách chương trình KM
 *   POST /api/admin/khuyen-mai                          → Tạo mới chương trình KM
 *   PUT  /api/admin/khuyen-mai/{id}                     → Cập nhật chương trình KM
 * </pre>
 */
@RestController
@RequiredArgsConstructor
public class KhuyenMaiController {

    private static final Logger log = LoggerFactory.getLogger(KhuyenMaiController.class);
    private final IKhuyenMaiService service;

    // ═════════════════════════════════════════════════════════════════════
    // ADMIN ENDPOINTS — /api/admin/khuyen-mai
    // ═════════════════════════════════════════════════════════════════════

    @GetMapping("/api/admin/khuyen-mai")
    public ResponseEntity<List<ChuongTrinhKhuyenMai>> layDanhSach(
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        return ResponseEntity.ok(service.layDanhSach());
    }

    @PostMapping("/api/admin/khuyen-mai")
    public ResponseEntity<?> taoMoi(
            @RequestBody ChuongTrinhKhuyenMai request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            ChuongTrinhKhuyenMai saved = service.luu(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ok("Tạo chương trình khuyến mãi thành công.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        }
    }

    @PutMapping("/api/admin/khuyen-mai/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable Integer id,
            @RequestBody ChuongTrinhKhuyenMai request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            request.setId(id);
            return ResponseEntity.ok(ok("Cập nhật thành công.", service.luu(request)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        }
    }


    private Map<String, Object> ok(String msg, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", msg); r.put("data", data); return r;
    }
    private Map<String, Object> err(String msg) {
        return Map.of("success", false, "message", msg);
    }
}
