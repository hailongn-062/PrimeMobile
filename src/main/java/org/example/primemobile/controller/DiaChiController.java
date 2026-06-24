package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.DiaChiKhachHang;
import org.example.primemobile.service.IDiaChiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller Địa Chỉ Khách Hàng (Public — khách hàng tự quản lý).
 * Base path: {@code /api/public/dia-chi}
 * <p>
 * ⚠️ Không dùng Session Admin. Xác thực khách hàng qua {@code khachHangId}
 * trong URL/body (sẽ tích hợp session khách hàng khi có module Auth KH).
 * Service Layer đã bảo vệ IDOR: kiểm tra địa chỉ phải thuộc đúng khách hàng.
 *
 * <pre>
 *   GET    /api/public/dia-chi/{khachHangId}                    → Danh sách địa chỉ
 *   POST   /api/public/dia-chi/{khachHangId}                    → Thêm địa chỉ mới (tối đa 5)
 *   PUT    /api/public/dia-chi/{khachHangId}/{diaChiId}         → Sửa địa chỉ
 *   PATCH  /api/public/dia-chi/{khachHangId}/{diaChiId}/mac-dinh → Set địa chỉ mặc định
 *   DELETE /api/public/dia-chi/{khachHangId}/{diaChiId}         → Xóa địa chỉ
 * </pre>
 */
@RestController
@RequestMapping("/api/public/dia-chi")
@RequiredArgsConstructor
public class DiaChiController {

    private static final Logger log = LoggerFactory.getLogger(DiaChiController.class);
    private final IDiaChiService service;

    /** Lấy danh sách địa chỉ của khách hàng (mặc định lên đầu). */
    @GetMapping("/{khachHangId}")
    public ResponseEntity<?> layDanhSach(@PathVariable Integer khachHangId) {
        try {
            List<DiaChiKhachHang> danhSach = service.layDanhSach(khachHangId);
            return ResponseEntity.ok(danhSach);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    /**
     * Thêm địa chỉ mới cho khách hàng.
     * <p>
     * Request body phải bao gồm đầy đủ 2 nhóm thông tin địa chỉ:
     * ID (gửi GHN) và Tên (hiển thị UI).
     *
     * <h3>Request body mẫu:</h3>
     * <pre>{@code
     * {
     *   "loaiDiaChi": "nha_rieng",
     *   "hoTenNguoiNhan": "Nguyễn Văn A",
     *   "soDienThoaiNguoiNhan": "0912345678",
     *   "diaChiChiTiet": "123 Đường ABC, Phường XYZ",
     *   "tinhThanhId": 202,
     *   "quanHuyenId": 1442,
     *   "phuongXaCode": "21012",
     *   "tinhThanhTen": "TP. Hồ Chí Minh",
     *   "quanHuyenTen": "Quận 1",
     *   "phuongXaTen": "Phường Bến Nghé"
     * }
     * }</pre>
     */
    @PostMapping("/{khachHangId}")
    public ResponseEntity<?> them(
            @PathVariable Integer khachHangId,
            @RequestBody DiaChiKhachHang request) {
        try {
            DiaChiKhachHang saved = service.them(khachHangId, request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ok("Thêm địa chỉ thành công.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    /** Sửa thông tin địa chỉ (chỉ cập nhật các field được gửi lên). */
    @PutMapping("/{khachHangId}/{diaChiId}")
    public ResponseEntity<?> sua(
            @PathVariable Integer khachHangId,
            @PathVariable Integer diaChiId,
            @RequestBody DiaChiKhachHang request) {
        try {
            DiaChiKhachHang saved = service.sua(diaChiId, khachHangId, request);
            return ResponseEntity.ok(ok("Cập nhật địa chỉ thành công.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    /**
     * Đặt địa chỉ làm mặc định.
     * Service Layer tự động bỏ mặc định của địa chỉ cũ và set địa chỉ này.
     */
    @PatchMapping("/{khachHangId}/{diaChiId}/mac-dinh")
    public ResponseEntity<?> setMacDinh(
            @PathVariable Integer khachHangId,
            @PathVariable Integer diaChiId) {
        try {
            DiaChiKhachHang saved = service.setMacDinh(diaChiId, khachHangId);
            return ResponseEntity.ok(ok("Đã đặt làm địa chỉ mặc định.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    /**
     * Xóa địa chỉ.
     * Không được xóa địa chỉ duy nhất còn lại.
     * Nếu xóa địa chỉ mặc định → tự động chuyển mặc định cho địa chỉ kế tiếp.
     */
    @DeleteMapping("/{khachHangId}/{diaChiId}")
    public ResponseEntity<?> xoa(
            @PathVariable Integer khachHangId,
            @PathVariable Integer diaChiId) {
        try {
            service.xoa(diaChiId, khachHangId);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa địa chỉ thành công."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    private Map<String, Object> ok(String msg, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true); r.put("message", msg); r.put("data", data); return r;
    }
    private Map<String, Object> err(String msg) { return Map.of("success", false, "message", msg); }
}
