package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IFlashSaleService;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller công khai — Khuyến mãi & Flash Sale cho Frontend (Khách vãng lai).
 * <p>
 * Base path: {@code /api/public} — KHÔNG được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET /api/public/khuyen-mai
 *       → Lấy các chương trình khuyến mãi đang diễn ra (dùng hiển thị banner, coupon).
 * 
 *   GET /api/public/flash-sale/active
 *       → Lấy các chương trình Flash Sale hiện tại đang diễn ra.
 * </pre>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class KhuyenMaiPublicController {

    private final IKhuyenMaiService khuyenMaiService;
    private final IFlashSaleService flashSaleService;

    /**
     * Lấy danh sách các chương trình khuyến mãi (không phải flash sale) đang hoạt động.
     * <p>
     * Dùng để Frontend hiển thị banner hoặc danh sách mã giảm giá.
     *
     * @return HTTP 200 kèm danh sách {@code ChuongTrinhKhuyenMai}.
     */
    @GetMapping("/khuyen-mai")
    public ResponseEntity<?> layKhuyenMaiDangDienRa() {
        log.info("[KhuyenMaiPublicController] GET /api/public/khuyen-mai");
        List<ChuongTrinhKhuyenMai> danhSach = khuyenMaiService.layKhuyenMaiDangDienRa();
        return ResponseEntity.ok(buildSuccessResponse("Lấy danh sách khuyến mãi thành công.", danhSach));
    }

    /**
     * Lấy chương trình Flash Sale đang hoạt động.
     * <p>
     * Dùng để Frontend hiển thị section Flash Sale ở trang chủ cùng danh sách sản phẩm.
     * Trả về danh sách (thường chỉ có 1 phần tử) Flash Sale đang diễn ra kèm chi tiết.
     *
     * @return HTTP 200 kèm danh sách {@code ChuongTrinhKhuyenMai} loại flash_sale.
     */
    @GetMapping("/flash-sale/active")
    public ResponseEntity<?> layFlashSaleDangDienRa() {
        log.info("[KhuyenMaiPublicController] GET /api/public/flash-sale/active");
        List<ChuongTrinhKhuyenMai> danhSach = flashSaleService.layFlashSaleDangDienRa();
        return ResponseEntity.ok(buildSuccessResponse("Lấy Flash Sale thành công.", danhSach));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Map<String, Object> buildSuccessResponse(String msg, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        return r;
    }
}
