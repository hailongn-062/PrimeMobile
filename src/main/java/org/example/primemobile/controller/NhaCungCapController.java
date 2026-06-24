package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.NhaCungCap;
import org.example.primemobile.service.INhaCungCapService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST Controller Nhà Cung Cấp (Admin).
 * Base path: {@code /api/admin/nha-cung-cap}
 *
 * <pre>
 *   GET  /api/admin/nha-cung-cap                    → Danh sách phân trang + tìm kiếm
 *   GET  /api/admin/nha-cung-cap/dang-hop-tac       → ComboBox khi tạo Phiếu Nhập Kho
 *   POST /api/admin/nha-cung-cap                    → Tạo mới NCC
 *   PUT  /api/admin/nha-cung-cap/{id}               → Cập nhật thông tin NCC
 *   PATCH /api/admin/nha-cung-cap/{id}/trang-thai   → Đổi trạng thái hợp tác
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/nha-cung-cap")
@RequiredArgsConstructor
public class NhaCungCapController {

    private static final Logger log = LoggerFactory.getLogger(NhaCungCapController.class);
    private final INhaCungCapService service;

    /** Danh sách NCC có phân trang + lọc trangThai + tìm kiếm. */
    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("tenNcc").ascending());
        Page<NhaCungCap> ketQua = service.layDanhSach(trangThai, tuKhoa, pageable);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("danhSach",      ketQua.getContent());
        response.put("tongSo",        ketQua.getTotalElements());
        response.put("tongSoTrang",   ketQua.getTotalPages());
        response.put("trangHienTai",  ketQua.getNumber());
        return ResponseEntity.ok(response);
    }

    /**
     * Danh sách NCC đang hợp tác — đổ vào ComboBox khi tạo Phiếu Nhập Kho.
     * Không cần phân trang vì số NCC hợp tác thường nhỏ.
     */
    @GetMapping("/dang-hop-tac")
    public ResponseEntity<?> layDangHopTac(
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        return ResponseEntity.ok(service.layDangHopTac());
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(
            @RequestBody NhaCungCap request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            NhaCungCap saved = service.taoMoi(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ok("Tạo nhà cung cấp thành công.", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(err(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable Integer id,
            @RequestBody NhaCungCap request,
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
     * Đổi trạng thái hợp tác của NCC.
     * Body JSON: {@code { "trangThaiMoi": "ngung_hop_tac" }}
     */
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            String trangThaiMoi = body.get("trangThaiMoi");
            return ResponseEntity.ok(ok("Đổi trạng thái thành công.", service.doiTrangThai(id, trangThaiMoi)));
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
    private Map<String, Object> err(String msg) { return Map.of("success", false, "message", msg); }
}
