package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.kho.ThemImeiRequest;
import org.example.primemobile.service.IMayDienThoaiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho phân hệ Kiểm Soát IMEI Máy Điện Thoại Vật Lý.
 * <p>
 * Base path: {@code /api/admin/imei} — được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (pattern {@code /api/admin/**}).
 *
 * <h3>Quy tắc nghiệp vụ (system_rules.md §3.3):</h3>
 * <ul>
 *   <li>Mỗi chiếc điện thoại vật lý được định danh qua cụm (imei1, imei2, serial).</li>
 *   <li><b>Chặn thêm thừa:</b> Số IMEI {@code trong_kho} của 1 SKU KHÔNG ĐƯỢC vượt quá
 *       tổng tồn kho (kho_tong + kho_online) của SKU đó.</li>
 *   <li>imei1, imei2, serial phải duy nhất toàn hệ thống.</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   POST /api/admin/imei/nhap                               → Nhập danh sách IMEI mới cho 1 SKU
 *   GET  /api/admin/imei/can-them/{khoId}/{bienTheId}       → Xem số IMEI còn cần nhập thêm
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/imei")
@RequiredArgsConstructor
public class MayDienThoaiController {

    private static final Logger log = LoggerFactory.getLogger(MayDienThoaiController.class);

    private final IMayDienThoaiService mayDienThoaiService;

    // =========================================================================
    // POST /api/admin/imei/nhap — Nhập danh sách IMEI cho 1 SKU tại 1 kho
    // =========================================================================

    /**
     * Nhập danh sách IMEI máy vật lý mới vào hệ thống cho 1 biến thể SKU.
     *
     * <h3>Request Body mẫu:</h3>
     * <pre>{@code
     * [
     *   { "imei1": "123456789012345", "imei2": "123456789012346", "serial": "SN-A001" },
     *   { "imei1": "987654321098765", "imei2": null,              "serial": "SN-A002" }
     * ]
     * }</pre>
     *
     * <h3>Luồng nghiệp vụ (system_rules.md §3.3):</h3>
     * <ol>
     *   <li>Kiểm tra số IMEI còn thiếu của SKU tại kho.</li>
     *   <li><b>Chặn thêm thừa:</b> Nếu số lượng gửi lên > số còn thiếu → HTTP 400.</li>
     *   <li>Kiểm tra trùng lặp imei1/imei2/serial trong DB.</li>
     *   <li>Lưu tất cả với tinhTrang = {@code "trong_kho"}.</li>
     * </ol>
     *
     * @param khoId            ID kho chứa hàng (dùng để tra cứu tồn kho làm mốc so sánh).
     * @param bienTheSanPhamId ID biến thể SKU cần gắn IMEI.
     * @param danhSachImei     Danh sách IMEI cần nhập (imei1 bắt buộc).
     * @param sessionUser      Nhân viên đang đăng nhập.
     * @return HTTP 201 Created nếu thành công.
     *         HTTP 400 nếu số lượng vượt quá, imei1 rỗng, hoặc trùng lặp.
     *         HTTP 404 nếu không tìm thấy kho hoặc biến thể.
     */
    @PostMapping("/nhap")
    public ResponseEntity<?> nhapDanhSachImei(
            @RequestParam Integer khoId,
            @RequestParam Integer bienTheSanPhamId,
            @RequestBody List<ThemImeiRequest> danhSachImei,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[IMEI] ▶ Nhập IMEI — khoId={}, bienTheId={}, soLuong={}, nhanVienId={}",
                khoId, bienTheSanPhamId,
                danhSachImei == null ? 0 : danhSachImei.size(),
                sessionUser.getId());
        try {
            mayDienThoaiService.nhapDanhSachImei(khoId, bienTheSanPhamId, danhSachImei);

            log.info("[IMEI] ✅ Nhập thành công {} IMEI cho bienTheId={}", danhSachImei.size(), bienTheSanPhamId);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("message", String.format(
                    "Nhập thành công %d mã IMEI cho biến thể ID %d.", danhSachImei.size(), bienTheSanPhamId));
            response.put("soLuongDaNhap", danhSachImei.size());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // Số lượng vượt tồn kho, imei trống, hoặc trùng lặp
            log.warn("[IMEI] ❌ Lỗi nghiệp vụ (400): {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            // Không tìm thấy kho hoặc biến thể
            log.warn("[IMEI] ❌ Không tìm thấy entity (404): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("[IMEI] ❌ Lỗi không mong đợi: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("Lỗi hệ thống. Vui lòng thử lại."));
        }
    }

    // =========================================================================
    // GET /api/admin/imei/can-them/{khoId}/{bienTheId} — Số IMEI cần nhập thêm
    // =========================================================================

    /**
     * Tính và trả về số lượng IMEI còn thiếu (cần nhập thêm) cho 1 SKU tại 1 kho.
     * <p>
     * Công thức: {@code max(0, ton_kho.so_luong - COUNT(IMEI trong_kho của SKU))}.
     * <p>
     * Hệ thống sử dụng endpoint này để:
     * <ul>
     *   <li>Báo cáo danh sách SKU thiếu IMEI cho nhân viên cần nhập thêm.</li>
     *   <li>Validate trước khi nhân viên submit form nhập IMEI.</li>
     * </ul>
     *
     * @param khoId    ID kho cần kiểm tra.
     * @param bienTheId ID biến thể SKU cần kiểm tra.
     * @return HTTP 200 kèm số lượng IMEI cần nhập thêm.
     *         HTTP 404 nếu SKU chưa có trong kho này.
     */
    @GetMapping("/can-them/{khoId}/{bienTheId}")
    public ResponseEntity<?> xemSoImeiCanThem(
            @PathVariable Integer khoId,
            @PathVariable Integer bienTheId,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[IMEI] Xem số cần thêm — khoId={}, bienTheId={}", khoId, bienTheId);
        try {
            int soCanThem = mayDienThoaiService.tinhSoLuongImeiCanThem(khoId, bienTheId);

            Map<String, Object> response = new LinkedHashMap<>();
            response.put("khoId", khoId);
            response.put("bienTheSanPhamId", bienTheId);
            response.put("soImeiCanNhapThem", soCanThem);
            response.put("message", soCanThem == 0
                    ? "SKU này đã được định danh đủ IMEI. Không cần nhập thêm."
                    : String.format("SKU còn thiếu %d mã IMEI. Vui lòng nhập thêm.", soCanThem));
            return ResponseEntity.ok(response);

        } catch (EntityNotFoundException e) {
            log.warn("[IMEI] ❌ Không tìm thấy tồn kho khoId={}, bienTheId={}: {}", khoId, bienTheId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // Exception Handler cục bộ
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[IMEI] Handler toàn cục — IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}
