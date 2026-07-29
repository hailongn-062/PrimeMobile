package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IKhuyenMaiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller công khai — Khuyến mãi cho Frontend (Khách vãng lai).
 * <p>
 * Base path: {@code /api/public} — KHÔNG được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor}.
 * <p>
 * Endpoints:
 * <pre>
 *   GET /api/public/khuyen-mai
 *       → Lấy các chương trình khuyến mãi đang diễn ra (dùng hiển thị banner, coupon).
 * </pre>
 */
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
@Slf4j
public class KhuyenMaiPublicController {

    private final IKhuyenMaiService khuyenMaiService;

    /**
     * Lấy danh sách các chương trình khuyến mãi đang hoạt động.
     * <p>
     * Dùng để Frontend hiển thị banner hoặc danh sách mã giảm giá.
     *
     * @return HTTP 200 kèm danh sách {@code ChuongTrinhKhuyenMai}.
     */
    @GetMapping("/khuyen-mai")
    public ResponseEntity<?> layKhuyenMaiDangDienRa() {
        log.info("[KhuyenMaiPublicController] GET /api/public/khuyen-mai");
        List<ChuongTrinhKhuyenMai> danhSach = khuyenMaiService.layKhuyenMaiDangDienRa();
        return ResponseEntity.ok(buildSuccessResponse("Lấy danh sách khuyến mãi đang diễn ra thành công.", danhSach));
    }
    /**
     * Lấy danh sách các chương trình khuyến mãi loại 'theo_don_hang' và đang diễn ra.
     * Dùng cho dropdown khi khách hàng checkout (chọn mã).
     */
    @GetMapping("/khuyen-mai/don-hang")
    public ResponseEntity<?> layKhuyenMaiChoChonDonHang() {
        log.info("[KhuyenMaiPublicController] GET /api/public/khuyen-mai/don-hang");
        List<ChuongTrinhKhuyenMai> danhSach = khuyenMaiService.layDanhSachChoChonDonHang();
        return ResponseEntity.ok(buildSuccessResponse("Lấy danh sách khuyến mãi đơn hàng thành công.", danhSach));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Tính toán số tiền được giảm cho khách hàng khi chọn mã.
     * <p>
     * @param tongTien Tổng tiền đơn hàng
     * @param ctkmId ID chương trình khuyến mãi do khách chọn
     * @return Kết quả tính toán
     */
    @GetMapping("/khuyen-mai/tinh-toan")
    public ResponseEntity<?> tinhToanKhuyenMai(
            @org.springframework.web.bind.annotation.RequestParam java.math.BigDecimal tongTien,
            @org.springframework.web.bind.annotation.RequestParam Integer ctkmId) {
        log.info("[KhuyenMaiPublicController] GET /api/public/khuyen-mai/tinh-toan?tongTien={}&ctkmId={}", tongTien, ctkmId);
        
        try {
            org.example.primemobile.dto.KhuyenMaiResult result = khuyenMaiService.apDungCtkmTheoId(ctkmId, tongTien);
            // Dùng Map để trả về dữ liệu dễ dùng cho frontend
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("ctkmId", result.getCtkmId());
            data.put("tenCtkm", result.getTenCtkm());
            data.put("giaTriUuDai", result.getGiaTriUuDai());
            data.put("tienGiam", result.getTienGiam());
            
            return ResponseEntity.ok(buildSuccessResponse("Tính toán thành công", data));
        } catch (IllegalArgumentException e) {
            log.warn("[KhuyenMaiPublicController] Khách hàng chọn mã không hợp lệ (ctkmId={}): {}", ctkmId, e.getMessage());
            // Trả về lỗi kèm message để frontend hiển thị alert
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("success", false);
            r.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(r);
        }
    }
    // =========================================================================

    private Map<String, Object> buildSuccessResponse(String msg, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        return r;
    }
}
