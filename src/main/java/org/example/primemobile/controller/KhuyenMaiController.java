package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.entity.MaGiamGia;
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
 *   POST /api/admin/khuyen-mai/{id}/sinh-ma             → Sinh mã giảm giá hàng loạt
 * </pre>
 *
 * <h3>Public endpoint (Frontend Checkout):</h3>
 * <pre>
 *   GET  /api/public/khuyen-mai/check-ma?maCode=&tongTien= → Kiểm tra & tính tiền giảm
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
            ChuongTrinhKhuyenMai saved = service.taoMoi(request);
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
            return ResponseEntity.ok(ok("Cập nhật thành công.", service.capNhat(id, request)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        }
    }

    /**
     * Sinh mã giảm giá hàng loạt cho 1 chương trình KM loại "ma_code".
     * <p>
     * Query params: {@code soLuong} (số mã cần sinh), {@code prefix} (tiền tố mã).
     * Ví dụ: {@code POST /api/admin/khuyen-mai/5/sinh-ma?soLuong=50&prefix=SALE24}
     */
    @PostMapping("/api/admin/khuyen-mai/{id}/sinh-ma")
    public ResponseEntity<?> sinhMaGiamGia(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "10")   int    soLuong,
            @RequestParam(defaultValue = "CODE") String prefix,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            List<MaGiamGia> danhSach = service.sinhMaGiamGia(id, soLuong, prefix);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ok("Sinh " + danhSach.size() + " mã giảm giá thành công.", danhSach));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    // PUBLIC ENDPOINT — /api/public/khuyen-mai/check-ma (Frontend Checkout)
    // ═════════════════════════════════════════════════════════════════════

    /**
     * Kiểm tra mã giảm giá hợp lệ và trả về số tiền được giảm.
     * <p>
     * Frontend gọi endpoint này khi khách nhập mã coupon tại trang Checkout.
     * Luôn trả về HTTP 200. Trường {@code hopLe} cho biết mã có dùng được không.
     *
     * <h3>Response thành công:</h3>
     * <pre>{@code
     * { "hopLe": true,  "soTienGiam": 50000, "maCode": "SUMMER24-ABCD1234", "tongTienSauGiam": 950000 }
     * }</pre>
     * <h3>Response thất bại:</h3>
     * <pre>{@code
     * { "hopLe": false, "soTienGiam": 0, "message": "Mã đã hết lượt sử dụng." }
     * }</pre>
     */
    @GetMapping("/api/public/khuyen-mai/check-ma")
    public ResponseEntity<?> checkMa(
            @RequestParam String     maCode,
            @RequestParam BigDecimal tongTien) {

        log.info("[KhuyenMai] Check mã '{}' | tongTien={}", maCode, tongTien);
        try {
            BigDecimal soTienGiam = service.checkMa(maCode, tongTien);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("hopLe",          true);
            response.put("maCode",         maCode.trim().toUpperCase());
            response.put("soTienGiam",     soTienGiam);
            response.put("tongTienSauGiam", tongTien.subtract(soTienGiam));
            response.put("message",        "Mã hợp lệ! Bạn được giảm " +
                    String.format("%,.0f", soTienGiam) + " VNĐ.");
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            // Mã không hợp lệ — vẫn trả HTTP 200 để Frontend xử lý trơn tru
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("hopLe",      false);
            response.put("soTienGiam", BigDecimal.ZERO);
            response.put("message",    e.getMessage());
            return ResponseEntity.ok(response);
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
