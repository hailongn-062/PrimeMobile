package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.TrungTamBaoHanh;
import org.example.primemobile.service.ITrungTamBaoHanhService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/trung-tam-bao-hanh")
@RequiredArgsConstructor
public class TrungTamBaoHanhController {

    private final ITrungTamBaoHanhService service;

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        Page<TrungTamBaoHanh> ketQua = service.timKiem(trangThai, tuKhoa, page, size);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("danhSach",      ketQua.getContent());
        response.put("tongSo",        ketQua.getTotalElements());
        response.put("tongSoTrang",   ketQua.getTotalPages());
        response.put("trangHienTai",  ketQua.getNumber());
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<?> themMoi(
            @RequestBody TrungTamBaoHanh req,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            TrungTamBaoHanh added = service.themMoi(req);
            return ResponseEntity.ok(added);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable Integer id,
            @RequestBody TrungTamBaoHanh req,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            TrungTamBaoHanh updated = service.capNhat(id, req);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            TrungTamBaoHanh updated = service.doiTrangThai(id);
            return ResponseEntity.ok(Map.of("id", updated.getId(), "trangThai", updated.getTrangThai()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
        }
    }
}
