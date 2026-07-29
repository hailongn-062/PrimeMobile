package org.example.primemobile.controller;

import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.baohanh.TaoYeuCauBaoHanhRequest;
import org.example.primemobile.entity.PhieuBaoHanh;
import org.example.primemobile.entity.YeuCauBaoHanh;
import org.example.primemobile.service.IBaoHanhService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/bao-hanh")
@RequiredArgsConstructor
public class BaoHanhController {

    private final IBaoHanhService baoHanhService;

    @GetMapping("/tra-cuu")
    public ResponseEntity<?> traCuuPhieuBaoHanh(@RequestParam(required = false) String sdt, @RequestParam(required = false) String imei) {
        try {
            if (sdt != null && !sdt.trim().isEmpty()) {
                var danhSach = baoHanhService.traCuuTheoSoDienThoai(sdt);
                return ResponseEntity.ok(danhSach);
            } else if (imei != null && !imei.trim().isEmpty()) {
                PhieuBaoHanh p = baoHanhService.traCuuPhieuBaoHanh(imei);
                org.example.primemobile.dto.baohanh.TraCuuBaoHanhResponse response = org.example.primemobile.dto.baohanh.TraCuuBaoHanhResponse.builder()
                        .idPhieu(p.getId())
                        .tenSanPham(p.getMayDienThoai().getBienTheSanPham().getSanPham().getTenSanPham())
                        .tenBienThe(p.getMayDienThoai().getBienTheSanPham().getMauSac() + " - " + 
                                    p.getMayDienThoai().getBienTheSanPham().getRamGb() + "GB/" + 
                                    p.getMayDienThoai().getBienTheSanPham().getLuuTruGb() + "GB")
                        .imei(p.getMayDienThoai().getImei1())
                        .tenKhachHang(p.getKhachHang().getHoTen())
                        .soDienThoai(p.getKhachHang().getSoDienThoai())
                        .ngayBatDau(p.getNgayBatDau())
                        .ngayHetHan(p.getNgayHetHan())
                        .build();
                return ResponseEntity.ok(java.util.List.of(response));
            }
            return ResponseEntity.badRequest().body(Map.of("message", "Vui lòng cung cấp số điện thoại hoặc IMEI"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/goi-y")
    public ResponseEntity<List<String>> goiY(@RequestParam String loai, @RequestParam String tuKhoa) {
        if ("sdt".equals(loai)) {
            return ResponseEntity.ok(baoHanhService.goiYSoDienThoai(tuKhoa));
        } else if ("imei".equals(loai)) {
            return ResponseEntity.ok(baoHanhService.goiYImei(tuKhoa));
        }
        return ResponseEntity.ok(List.of());
    }

    @PostMapping("/yeu-cau")
    public ResponseEntity<?> taoYeuCauBaoHanh(@RequestBody TaoYeuCauBaoHanhRequest request) {
        try {
            // Giả lập lấy nhân viên hiện tại từ Session/JWT (Hardcode ID 2: Nhân viên bán hàng)
            Integer nguoiTiepNhanId = 2;
            YeuCauBaoHanh yeuCau = baoHanhService.taoYeuCauBaoHanh(nguoiTiepNhanId, request);
            // Chỉ trả về các field cần thiết để tránh circular reference JSON
            return ResponseEntity.ok(Map.of(
                "id", yeuCau.getId(),
                "maYeuCau", yeuCau.getMaYeuCau(),
                "trangThai", yeuCau.getTrangThai()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/yeu-cau/{id}/gui-ttbh")
    public ResponseEntity<?> guiTTBH(@PathVariable Integer id) {
        try {
            YeuCauBaoHanh yeuCau = baoHanhService.capNhatTrangThaiYeuCau(id, "da_gui_ttbh", null);
            return ResponseEntity.ok(Map.of(
                "id", yeuCau.getId(),
                "maYeuCau", yeuCau.getMaYeuCau(),
                "trangThai", yeuCau.getTrangThai()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/yeu-cau/{id}/nhan-lai")
    public ResponseEntity<?> nhanLaiTuTTBH(@PathVariable Integer id, @RequestBody(required = false) Map<String, String> payload) {
        try {
            String ketQua = payload != null ? payload.get("ketQua") : null;
            YeuCauBaoHanh yeuCau = baoHanhService.capNhatTrangThaiYeuCau(id, "da_nhan_lai_ttbh", ketQua);
            return ResponseEntity.ok(Map.of(
                "id", yeuCau.getId(),
                "maYeuCau", yeuCau.getMaYeuCau(),
                "trangThai", yeuCau.getTrangThai()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/yeu-cau/{id}/tra-khach")
    public ResponseEntity<?> traKhach(@PathVariable Integer id) {
        try {
            YeuCauBaoHanh yeuCau = baoHanhService.capNhatTrangThaiYeuCau(id, "da_tra_khach", null);
            return ResponseEntity.ok(Map.of(
                "id", yeuCau.getId(),
                "maYeuCau", yeuCau.getMaYeuCau(),
                "trangThai", yeuCau.getTrangThai()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/yeu-cau")
    public ResponseEntity<?> layDanhSach(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String tuKhoa,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) Integer trungTamBhId,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate tuNgay,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate denNgay) {
        try {
            java.time.LocalDateTime startDateTime = tuNgay != null ? tuNgay.atStartOfDay() : null;
            java.time.LocalDateTime endDateTime = denNgay != null ? denNgay.atTime(23, 59, 59) : null;
            String finalTuKhoa = (tuKhoa != null && !tuKhoa.trim().isEmpty()) ? tuKhoa.trim() : null;
            String finalTrangThai = (trangThai != null && !trangThai.trim().isEmpty()) ? trangThai.trim() : null;
            Integer finalTrungTamBhId = (trungTamBhId != null && trungTamBhId > 0) ? trungTamBhId : null;

            // Lấy toàn bộ với JOIN FETCH để tránh circular JSON, rồi phân trang thủ công
            var allList = baoHanhService.timKiemVaLoc(finalTuKhoa, finalTrangThai, finalTrungTamBhId, startDateTime, endDateTime);
            int total = allList.size();
            int from = page * size;
            int to = Math.min(from + size, total);
            var pageContent = (from > total) ? java.util.List.of() : allList.subList(from, to);

            return ResponseEntity.ok(java.util.Map.of(
                "content", pageContent,
                "totalElements", total,
                "totalPages", (int) Math.ceil((double) total / size),
                "number", page
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("message", e.getMessage()));
        }
    }
}
