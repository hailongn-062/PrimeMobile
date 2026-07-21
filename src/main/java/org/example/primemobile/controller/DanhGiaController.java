package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.DanhGiaSanPham;
import org.example.primemobile.service.IDanhGiaService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DanhGiaController {

    private final IDanhGiaService danhGiaService;

    // =========================================================================
    // API Khách Hàng
    // =========================================================================

    /**
     * Kiểm tra điều kiện đánh giá sản phẩm.
     */
    @GetMapping("/api/khach-hang/danh-gia/kiem-tra-dieu-kien")
    public ResponseEntity<?> kiemTraDieuKien(@RequestParam Integer sanPhamId, HttpSession session) {
        SessionKhachHang currentCustomer = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (currentCustomer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of(
                    "hopLe", false,
                    "message", "Vui lòng đăng nhập để đánh giá."
            ));
        }
        
        IDanhGiaService.KiemTraDieuKienDto result = danhGiaService.kiemTraDieuKienDanhGia(currentCustomer.getKhachHangId(), sanPhamId);
        return ResponseEntity.ok(result);
    }

    /**
     * Tạo đánh giá sản phẩm (chỉ dành cho khách hàng đã đăng nhập).
     */
    @PostMapping("/api/khach-hang/danh-gia")
    public ResponseEntity<?> taoDanhGia(@RequestBody IDanhGiaService.DanhGiaRequest request, HttpSession session) {
        SessionKhachHang currentCustomer = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (currentCustomer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để đánh giá.");
        }

        try {
            DanhGiaSanPham danhGia = danhGiaService.taoDanhGia(currentCustomer.getKhachHangId(), request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi đánh giá thành công. Đánh giá của bạn đang chờ duyệt.",
                    "danhGiaId", danhGia.getId()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // =========================================================================
    // API Public
    // =========================================================================

    /**
     * Lấy danh sách đánh giá ĐÃ DUYỆT của 1 sản phẩm.
     */
    @GetMapping("/api/public/san-pham/{sanPhamId}/danh-gia")
    public ResponseEntity<?> layDanhGiaTheoSanPham(@PathVariable Integer sanPhamId) {
        List<DanhGiaSanPham> list = danhGiaService.layDanhGiaTheoSanPham(sanPhamId);
        
        // Transform ra format gọn hơn cho Frontend
        List<Map<String, Object>> response = list.stream().map(dg -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", dg.getId());
            map.put("hoTen", dg.getKhachHang().getHoTen());
            map.put("sao", dg.getSao());
            map.put("tieuDe", dg.getTieuDe());
            map.put("noiDung", dg.getNoiDung());
            map.put("hinhAnh", dg.getHinhAnhJson());
            map.put("ngayTao", dg.getNgayTao());
            return map;
        }).toList();

        return ResponseEntity.ok(response);
    }

    /**
     * Lấy sao trung bình và tổng số lượng đánh giá của sản phẩm.
     */
    @GetMapping("/api/public/san-pham/{sanPhamId}/sao-trung-binh")
    public ResponseEntity<?> laySaoTrungBinh(@PathVariable Integer sanPhamId) {
        return ResponseEntity.ok(danhGiaService.tinhSaoTrungBinh(sanPhamId));
    }

    // =========================================================================
    // API Admin/Nhân Viên
    // =========================================================================

    /**
     * Lấy danh sách toàn bộ đánh giá (dành cho Admin quản lý).
     * Yêu cầu xác thực AdminInterceptor nhưng Controller REST không render view,
     * nên interceptor /api/admin/** sẽ đảm bảo bảo mật.
     */
    @GetMapping("/api/admin/danh-gia")
    public ResponseEntity<Page<DanhGiaSanPham>> layTatCaDanhGia(Pageable pageable) {
        return ResponseEntity.ok(danhGiaService.layTatCaDanhGia(pageable));
    }

    /**
     * Duyệt đánh giá
     */
    @PutMapping("/api/admin/danh-gia/{id}/duyet")
    public ResponseEntity<?> duyetDanhGia(@PathVariable Integer id) {
        try {
            danhGiaService.duyetDanhGia(id);
            return ResponseEntity.ok("Đã duyệt đánh giá.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Ẩn đánh giá
     */
    @PutMapping("/api/admin/danh-gia/{id}/an")
    public ResponseEntity<?> anDanhGia(@PathVariable Integer id) {
        try {
            danhGiaService.anDanhGia(id);
            return ResponseEntity.ok("Đã ẩn đánh giá.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
