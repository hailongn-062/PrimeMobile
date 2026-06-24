package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.ThongSoKyThuat;
import org.example.primemobile.service.IThongSoKyThuatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller cho phân hệ Thông Số Kỹ Thuật (Admin).
 * Base path: {@code /api/admin/thong-so}
 *
 * <pre>
 *   GET    /api/admin/thong-so/{sanPhamId}          → Lấy toàn bộ thông số của sản phẩm
 *   POST   /api/admin/thong-so/{sanPhamId}           → Thêm 1 thông số mới
 *   PUT    /api/admin/thong-so/chi-tiet/{id}         → Sửa 1 thông số
 *   DELETE /api/admin/thong-so/chi-tiet/{id}         → Xóa 1 thông số
 *   PUT    /api/admin/thong-so/{sanPhamId}/toan-bo   → Thay thế toàn bộ (bulk upsert)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/thong-so")
@RequiredArgsConstructor
public class ThongSoKyThuatController {

    private static final Logger log = LoggerFactory.getLogger(ThongSoKyThuatController.class);
    private final IThongSoKyThuatService service;

    @GetMapping("/{sanPhamId}")
    public ResponseEntity<?> layTheoSanPham(
            @PathVariable Integer sanPhamId,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            return ResponseEntity.ok(service.layTheoSanPham(sanPhamId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    @PostMapping("/{sanPhamId}")
    public ResponseEntity<?> them(
            @PathVariable Integer sanPhamId,
            @RequestBody ThongSoKyThuat request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            ThongSoKyThuat saved = service.them(sanPhamId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ok("Thêm thông số thành công.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    @PutMapping("/chi-tiet/{id}")
    public ResponseEntity<?> sua(
            @PathVariable Integer id,
            @RequestBody ThongSoKyThuat request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            return ResponseEntity.ok(ok("Cập nhật thông số thành công.", service.sua(id, request)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    @DeleteMapping("/chi-tiet/{id}")
    public ResponseEntity<?> xoa(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            service.xoa(id);
            return ResponseEntity.ok(Map.of("success", true, "message", "Xóa thông số thành công."));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(err(e.getMessage()));
        }
    }

    /**
     * Thay thế toàn bộ thông số kỹ thuật của 1 sản phẩm.
     * Dùng cho tính năng "Lưu tất cả" trên trang chỉnh sửa sản phẩm.
     */
    @PutMapping("/{sanPhamId}/toan-bo")
    public ResponseEntity<?> capNhatToanBo(
            @PathVariable Integer sanPhamId,
            @RequestBody List<ThongSoKyThuat> danhSach,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            List<ThongSoKyThuat> saved = service.capNhatToanBo(sanPhamId, danhSach);
            return ResponseEntity.ok(ok("Cập nhật toàn bộ thông số thành công. Đã lưu " + saved.size() + " dòng.", saved));
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
    private Map<String, Object> err(String msg) {
        return Map.of("success", false, "message", msg);
    }
}
