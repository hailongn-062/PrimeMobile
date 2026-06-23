package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.service.IBanHangOfflineService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller cho phân hệ Bán hàng Offline (tại quầy).
 * <p>
 * Base path: {@code /api/admin/ban-hang} — được {@link org.example.primemobile.interceptor.AuthInterceptor}
 * bảo vệ tự động (pattern {@code /api/admin/**}). Mọi request phải có session hợp lệ.
 * <p>
 * Luồng nghiệp vụ 3 bước (system_rules.md §2.1):
 * <pre>
 *   POST /tao-don    → Tạo đơn nháp "cho_thanh_toan", gắn với nhân viên đang đăng nhập
 *   POST /them-sp    → Thêm SKU vào đơn (kiểm tra Safety Stock ≥ 5 đơn vị)
 *   POST /thanh-toan → Chốt đơn, lưu thanh toán, trừ kho thực tế
 * </pre>
 * <p>
 * {@code @SessionAttribute("CURRENT_ADMIN")} đảm bảo lấy {@link SessionUser} an toàn từ session
 * mà không cần gọi lại DB — {@link org.example.primemobile.interceptor.AuthInterceptor} đã xác
 * minh tính hợp lệ trước đó.
 */
@RestController
@RequestMapping("/api/admin/ban-hang")
@RequiredArgsConstructor
public class BanHangOfflineController {

    private static final Logger log = LoggerFactory.getLogger(BanHangOfflineController.class);

    private final IBanHangOfflineService banHangOfflineService;

    // =========================================================================
    // POST /api/admin/ban-hang/tao-don
    // =========================================================================

    /**
     * Tạo đơn hàng offline mới ở trạng thái nháp {@code "cho_thanh_toan"}.
     * <p>
     * Lấy {@code nhanVienId} trực tiếp từ {@link SessionUser} trong {@code HttpSession}
     * thay vì nhận qua request body — tránh giả mạo ID nhân viên từ phía client.
     *
     * @param sessionUser Đối tượng người dùng đang đăng nhập, lấy tự động từ session
     *                    (key {@code "CURRENT_ADMIN"}, được {@link AuthController} đặt vào sau login).
     * @return HTTP 200 kèm {@link DonHang} vừa tạo nếu thành công;
     *         HTTP 404 nếu {@code nhanVienId} không tồn tại trong DB;
     *         HTTP 400 nếu tham số không hợp lệ.
     */
    @PostMapping("/tao-don")
    public ResponseEntity<?> taoDonHangMoi(
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[BanHangOfflineController] Tạo đơn mới — nhanVienId={}", sessionUser.getId());
        try {
            DonHang donHang = banHangOfflineService.taoDonHangMoi(sessionUser.getId());
            log.info("[BanHangOfflineController] Tạo đơn thành công — donHangId={}", donHang.getId());
            return ResponseEntity.ok(donHang);

        } catch (EntityNotFoundException e) {
            log.error("[BanHangOfflineController] Không tìm thấy nhân viên id={}", sessionUser.getId());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // POST /api/admin/ban-hang/them-sp
    // =========================================================================

    /**
     * Thêm sản phẩm (biến thể SKU) vào đơn hàng đang nháp, hoặc cộng dồn số lượng nếu đã có.
     * <p>
     * Ràng buộc Safety Stock (system_rules.md §3.1): Service sẽ ném
     * {@link IllegalArgumentException} nếu {@code (tồn kho - soLuong) < 5}.
     *
     * @param donHangId          ID đơn hàng đang ở trạng thái {@code "cho_thanh_toan"}.
     * @param bienTheSanPhamId   ID biến thể sản phẩm (SKU) cần thêm.
     * @param soLuong            Số lượng cần thêm (phải > 0).
     * @param sessionUser        Người dùng đang đăng nhập (chỉ dùng để log, không cần truyền xuống Service).
     * @return HTTP 200 kèm {@link DonHang} đã cập nhật;
     *         HTTP 400 nếu vi phạm Safety Stock, soLuong ≤ 0, hoặc đơn không ở đúng trạng thái;
     *         HTTP 404 nếu đơn hàng hoặc biến thể không tồn tại.
     */
    @PostMapping("/them-sp")
    public ResponseEntity<?> themSanPhamVaoDon(
            @RequestParam Integer donHangId,
            @RequestParam Integer bienTheSanPhamId,
            @RequestParam int    soLuong,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[BanHangOfflineController] Thêm sản phẩm — donHangId={}, bienTheSanPhamId={}, soLuong={}, nhanVienId={}",
                donHangId, bienTheSanPhamId, soLuong, sessionUser.getId());
        try {
            DonHang donHang = banHangOfflineService.themSanPhamVaoDon(donHangId, bienTheSanPhamId, soLuong);
            return ResponseEntity.ok(donHang);

        } catch (IllegalArgumentException e) {
            // Vi phạm Safety Stock hoặc soLuong <= 0
            log.warn("[BanHangOfflineController] Vi phạm ràng buộc khi thêm sản phẩm: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            // Đơn hàng không ở trạng thái "cho_thanh_toan"
            log.warn("[BanHangOfflineController] Sai trạng thái đơn hàng id={}: {}", donHangId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (EntityNotFoundException e) {
            log.error("[BanHangOfflineController] Không tìm thấy tài nguyên: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // POST /api/admin/ban-hang/thanh-toan
    // =========================================================================

    /**
     * Hoàn tất thanh toán đơn hàng offline.
     * <p>
     * Thực thi trong một transaction (do Service layer đảm bảo):
     * <ol>
     *   <li>Cập nhật trạng thái đơn → {@code "da_giao"}.</li>
     *   <li>Cập nhật trạng thái thanh toán → {@code "da_thanh_toan"}.</li>
     *   <li>Tạo bản ghi {@code ThanhToan} với {@code trang_thai = "thanh_cong"}.</li>
     *   <li>Trừ trực tiếp {@code ton_kho} tại Kho Tổng cho từng {@code ChiTietDonHang}.</li>
     * </ol>
     *
     * @param donHangId             ID đơn hàng cần chốt thanh toán.
     * @param phuongThucThanhToanId ID phương thức thanh toán (COD, chuyển khoản, v.v.).
     * @param sessionUser           Người dùng đang đăng nhập (dùng để log kiểm soát).
     * @return HTTP 200 kèm {@link DonHang} đã hoàn tất;
     *         HTTP 400 nếu đơn sai trạng thái hoặc tồn kho thực tế không đủ;
     *         HTTP 404 nếu không tìm thấy đơn, phương thức thanh toán, hoặc tồn kho.
     */
    @PostMapping("/thanh-toan")
    public ResponseEntity<?> thanhToanDonHang(
            @RequestParam Integer donHangId,
            @RequestParam Integer phuongThucThanhToanId,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[BanHangOfflineController] Thanh toán — donHangId={}, phuongThucId={}, nhanVienId={}",
                donHangId, phuongThucThanhToanId, sessionUser.getId());
        try {
            DonHang donHang = banHangOfflineService.thanhToanDonHang(donHangId, phuongThucThanhToanId);
            log.info("[BanHangOfflineController] Thanh toán thành công — donHangId={}", donHang.getId());
            return ResponseEntity.ok(donHang);

        } catch (IllegalArgumentException e) {
            // Tồn kho thực tế không đủ khi trừ
            log.warn("[BanHangOfflineController] Lỗi tồn kho khi thanh toán donHangId={}: {}", donHangId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (IllegalStateException e) {
            // Đơn hàng không ở trạng thái "cho_thanh_toan"
            log.warn("[BanHangOfflineController] Sai trạng thái đơn hàng id={}: {}", donHangId, e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());

        } catch (EntityNotFoundException e) {
            log.error("[BanHangOfflineController] Không tìm thấy tài nguyên: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback cho các lỗi không được bắt trong handler)
    // =========================================================================

    /**
     * Bắt {@link IllegalArgumentException} cục bộ — ví dụ vi phạm Safety Stock.
     * Trả về HTTP 400 kèm message lỗi rõ ràng để Postman / Front-end hiển thị.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }

    /**
     * Bắt {@link IllegalStateException} cục bộ — ví dụ đơn hàng sai trạng thái.
     * Trả về HTTP 400 kèm message mô tả vi phạm.
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
