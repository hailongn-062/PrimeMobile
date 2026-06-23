package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.GioHang;
import org.example.primemobile.service.IGioHangService;
import org.example.primemobile.service.impl.GioHangServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller quản lý Giỏ Hàng Online — đường dẫn công khai, cả khách vãng lai đều dùng được.
 * <p>
 * Base path: {@code /api/public/gio-hang} — KHÔNG bảo vệ bởi AuthInterceptor.
 * <p>
 * Xác định người dùng qua 2 param tùy chọn:
 * <ul>
 *   <li>{@code khachHangId} — Dành cho khách đã đăng nhập.</li>
 *   <li>{@code sessionId}   — Dành cho khách vãng lai (UUID từ localStorage/cookie browser).</li>
 * </ul>
 * Phải truyền ít nhất một trong 2, nếu không sẽ nhận HTTP 400.
 * <p>
 * Endpoints:
 * <pre>
 *   GET    /api/public/gio-hang                   → Lấy giỏ hàng kèm tổng tiền tạm tính
 *   POST   /api/public/gio-hang/them              → Thêm sản phẩm vào giỏ
 *   PUT    /api/public/gio-hang/item/{itemId}     → Cập nhật số lượng một dòng
 *   DELETE /api/public/gio-hang/item/{itemId}     → Xóa một dòng khỏi giỏ
 *   DELETE /api/public/gio-hang                   → Xóa toàn bộ giỏ hàng
 * </pre>
 */
@RestController
@RequestMapping("/api/public/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private static final Logger log = LoggerFactory.getLogger(GioHangController.class);

    private final IGioHangService gioHangService;

    // =========================================================================
    // GET — Lấy giỏ hàng
    // =========================================================================

    /**
     * Lấy toàn bộ giỏ hàng kèm tổng tiền tạm tính.
     * <p>
     * Response trả về wrapper object gồm {@code gioHang} và {@code tongTienTamTinh} (VND).
     * <p>
     * Ví dụ:
     * <ul>
     *   <li>Khách đăng nhập: {@code GET /api/public/gio-hang?khachHangId=1}</li>
     *   <li>Khách vãng lai:  {@code GET /api/public/gio-hang?sessionId=abc-123-xyz}</li>
     * </ul>
     */
    @GetMapping
    public ResponseEntity<?> layGioHang(
            @RequestParam(required = false) Integer khachHangId,
            @RequestParam(required = false) String sessionId) {

        GioHang gioHang = gioHangService.layGioHang(khachHangId, sessionId);

        if (gioHang == null) {
            return ResponseEntity.ok(Map.of(
                    "gioHang", (Object) null,
                    "tongTienTamTinh", BigDecimal.ZERO,
                    "soLuongSanPham", 0
            ));
        }

        BigDecimal tongTien = GioHangServiceImpl.tinhTongTienTamTinh(gioHang);
        int soLuong = gioHang.getChiTietGioHangs() == null ? 0 : gioHang.getChiTietGioHangs().size();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("gioHang", gioHang);
        response.put("tongTienTamTinh", tongTien);
        response.put("soLuongSanPham", soLuong);
        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // POST — Thêm vào giỏ
    // =========================================================================

    /**
     * Thêm sản phẩm vào giỏ hàng (upsert — cộng dồn nếu đã có).
     * <p>
     * Body JSON:
     * <pre>
     * {
     *   "khachHangId": 1,         ← hoặc null
     *   "sessionId": "abc-xyz",   ← hoặc null
     *   "bienTheSanPhamId": 3,
     *   "soLuong": 2
     * }
     * </pre>
     * Sau khi thêm thành công, trả về giỏ hàng kèm tổng tiền tạm tính.
     */
    @PostMapping("/them")
    public ResponseEntity<?> them(@RequestBody ThemVaoGioHangRequest request) {
        log.info("[GioHangController] Thêm vào giỏ — khachHangId={}, sessionId={}, bienTheId={}, soLuong={}",
                request.khachHangId(), request.sessionId(), request.bienTheSanPhamId(), request.soLuong());
        try {
            GioHang gioHang = gioHangService.themVaoGioHang(
                    request.khachHangId(),
                    request.sessionId(),
                    request.bienTheSanPhamId(),
                    request.soLuong()
            );
            BigDecimal tongTien = GioHangServiceImpl.tinhTongTienTamTinh(gioHang);
            return ResponseEntity.ok(Map.of("gioHang", gioHang, "tongTienTamTinh", tongTien));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // PUT — Cập nhật số lượng
    // =========================================================================

    /**
     * Cập nhật số lượng của một dòng sản phẩm trong giỏ.
     * <p>
     * Ví dụ: {@code PUT /api/public/gio-hang/item/5?soLuongMoi=3}
     *
     * @param itemId     ID dòng {@code chi_tiet_gio_hang} cần cập nhật.
     * @param soLuongMoi Số lượng mới (phải > 0 và ≤ tồn kho kho_online).
     */
    @PutMapping("/item/{itemId}")
    public ResponseEntity<?> capNhatSoLuong(
            @PathVariable Integer itemId,
            @RequestParam Integer soLuongMoi) {
        log.info("[GioHangController] Cập nhật số lượng — itemId={}, soLuongMoi={}", itemId, soLuongMoi);
        try {
            GioHang gioHang = gioHangService.capNhatSoLuong(itemId, soLuongMoi);
            BigDecimal tongTien = GioHangServiceImpl.tinhTongTienTamTinh(gioHang);
            return ResponseEntity.ok(Map.of("gioHang", gioHang, "tongTienTamTinh", tongTien));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // DELETE — Xóa
    // =========================================================================

    /**
     * Xóa một dòng sản phẩm khỏi giỏ hàng.
     * <p>
     * Ví dụ: {@code DELETE /api/public/gio-hang/item/5}
     *
     * @param itemId ID dòng {@code chi_tiet_gio_hang} cần xóa.
     */
    @DeleteMapping("/item/{itemId}")
    public ResponseEntity<?> xoaKhoiGioHang(@PathVariable Integer itemId) {
        log.info("[GioHangController] Xóa khỏi giỏ — itemId={}", itemId);
        try {
            gioHangService.xoaKhoiGioHang(itemId);
            return ResponseEntity.ok("Đã xóa sản phẩm khỏi giỏ hàng.");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Xóa toàn bộ giỏ hàng (dùng sau khi đặt hàng thành công hoặc khách tự xóa).
     * <p>
     * Ví dụ: {@code DELETE /api/public/gio-hang?khachHangId=1}
     */
    @DeleteMapping
    public ResponseEntity<?> xoaToanBoGioHang(
            @RequestParam(required = false) Integer khachHangId,
            @RequestParam(required = false) String sessionId) {
        log.info("[GioHangController] Xóa toàn bộ giỏ — khachHangId={}, sessionId={}", khachHangId, sessionId);
        gioHangService.xoaToanBoGioHang(khachHangId, sessionId);
        return ResponseEntity.ok("Đã xóa toàn bộ giỏ hàng.");
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    // =========================================================================
    // REQUEST DTO (Record thay thế class DTO để code gọn)
    // =========================================================================

    /**
     * DTO nhận request thêm vào giỏ hàng.
     * Dùng Java Record để tránh tạo thêm file DTO riêng biệt.
     */
    public record ThemVaoGioHangRequest(
            Integer khachHangId,
            String  sessionId,
            Integer bienTheSanPhamId,
            Integer soLuong
    ) {}
}
