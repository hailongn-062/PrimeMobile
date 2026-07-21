package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionKhachHang;
import org.example.primemobile.entity.YeuThich;
import org.example.primemobile.service.IYeuThichService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller quản lý Danh sách yêu thích của Khách Hàng.
 * <p>
 * Yêu cầu: Khách hàng phải đăng nhập (có SessionKhachHang trong session).
 */
@RestController
@RequestMapping("/api/khach-hang/yeu-thich")
@RequiredArgsConstructor
public class YeuThichController {

    private static final Logger log = LoggerFactory.getLogger(YeuThichController.class);

    private final IYeuThichService yeuThichService;

    /**
     * Lấy danh sách sản phẩm yêu thích của khách hàng đang đăng nhập.
     */
    @GetMapping
    public ResponseEntity<?> layDanhSachYeuThich(HttpSession session) {
        SessionKhachHang currentCustomer = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (currentCustomer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để xem danh sách yêu thích.");
        }

        List<YeuThich> danhSach = yeuThichService.layDanhSach(currentCustomer.getKhachHangId());
        
        // Transform thành response gọn hơn (tùy nhu cầu UI, có thể mapping sang DTO sau)
        List<Map<String, Object>> responseList = danhSach.stream().map(yt -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("sanPhamId", yt.getSanPham().getId());
            map.put("tenSanPham", yt.getSanPham().getTenSanPham());
            map.put("ngayThem", yt.getNgayThem());
            
            // Xử lý ảnh chính nếu có
            if (yt.getSanPham().getBienTheSanPhams() != null && !yt.getSanPham().getBienTheSanPhams().isEmpty()) {
                var bienThe = yt.getSanPham().getBienTheSanPhams().get(0);
                if (bienThe.getHinhAnhSanPhams() != null && !bienThe.getHinhAnhSanPhams().isEmpty()) {
                    map.put("hinhAnh", bienThe.getHinhAnhSanPhams().get(0).getDuongDan());
                }
            }
            return map;
        }).toList();

        return ResponseEntity.ok(responseList);
    }

    /**
     * Toggle thêm/xóa sản phẩm khỏi danh sách yêu thích.
     * 
     * @param sanPhamId ID sản phẩm cần thêm/xóa
     */
    @PostMapping("/{sanPhamId}")
    public ResponseEntity<?> toggleYeuThich(@PathVariable Integer sanPhamId, HttpSession session) {
        SessionKhachHang currentCustomer = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (currentCustomer == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Vui lòng đăng nhập để thêm vào yêu thích.");
        }

        try {
            boolean isAdded = yeuThichService.toggleYeuThich(currentCustomer.getKhachHangId(), sanPhamId);
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("success", true);
            response.put("isAdded", isAdded);
            response.put("message", isAdded ? "Đã thêm vào danh sách yêu thích." : "Đã gỡ khỏi danh sách yêu thích.");
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * Kiểm tra trạng thái yêu thích của một sản phẩm.
     * Dùng để hiển thị trạng thái nút (đã thả tim chưa) trên trang chi tiết sản phẩm.
     */
    @GetMapping("/check/{sanPhamId}")
    public ResponseEntity<?> kiemTraDaYeuThich(@PathVariable Integer sanPhamId, HttpSession session) {
        SessionKhachHang currentCustomer = (SessionKhachHang) session.getAttribute(SessionKhachHang.SESSION_KEY);
        if (currentCustomer == null) {
            return ResponseEntity.ok(Map.of("isLiked", false));
        }

        boolean isLiked = yeuThichService.kiemTraDaYeuThich(currentCustomer.getKhachHangId(), sanPhamId);
        return ResponseEntity.ok(Map.of("isLiked", isLiked));
    }
}
