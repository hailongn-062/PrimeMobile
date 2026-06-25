package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.flashsale.TaoFlashSaleRequest;
import org.example.primemobile.entity.ChuongTrinhKhuyenMai;
import org.example.primemobile.service.IFlashSaleService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/flash-sale")
@RequiredArgsConstructor
@Slf4j
public class FlashSaleController {

    private final IFlashSaleService flashSaleService;

    @GetMapping
    public ResponseEntity<?> layDanhSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        log.info("[FlashSaleController] layDanhSach page={}, size={}", page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayBatDau"));
        Page<ChuongTrinhKhuyenMai> ketQua = flashSaleService.layDanhSachFlashSale(pageable);
        return ResponseEntity.ok(buildSuccessResponse("OK", ketQua));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTiet(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            return ResponseEntity.ok(buildSuccessResponse("OK", flashSaleService.layChiTietFlashSale(id)));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> taoMoi(
            @RequestBody TaoFlashSaleRequest request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            ChuongTrinhKhuyenMai saved = flashSaleService.taoFlashSale(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(buildSuccessResponse("Tạo Flash Sale thành công", saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(
            @PathVariable Integer id,
            @RequestBody TaoFlashSaleRequest request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            ChuongTrinhKhuyenMai updated = flashSaleService.capNhatFlashSale(id, request);
            return ResponseEntity.ok(buildSuccessResponse("Cập nhật Flash Sale thành công", updated));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {
        try {
            String trangThai = body.get("trangThaiMoi");
            ChuongTrinhKhuyenMai updated = flashSaleService.doiTrangThai(id, trangThai);
            return ResponseEntity.ok(buildSuccessResponse("Đổi trạng thái thành công", updated));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    private Map<String, Object> buildSuccessResponse(String msg, Object data) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);
        r.put("message", msg);
        r.put("data", data);
        return r;
    }

    private Map<String, Object> buildErrorResponse(String msg) {
        return Map.of("success", false, "message", msg);
    }
}
