package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest;
import org.example.primemobile.dto.phieunhap.ChiTietPhieuNhapDto;
import org.example.primemobile.dto.phieunhap.PhieuNhapKhoDto;
import org.example.primemobile.entity.PhieuNhapKho;
import org.example.primemobile.service.IKhoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller cho phân hệ Phiếu Nhập Kho (Inbound).
 * <p>
 * Base path: {@code /api/admin/phieu-nhap} — được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (pattern
 * {@code /api/admin/**}).
 *
 * <h3>Quy tắc nghiệp vụ (system_rules.md §3.2):</h3>
 * <ul>
 * <li>Nhập hàng từ NCC luôn đi thẳng vào <b>Kho Tổng</b> (loai =
 * 'kho_tong').</li>
 * <li>Phiếu nhập <b>CHỐT LUÔN</b> khi tạo — không qua bước chờ duyệt.</li>
 * <li>Tồn kho tại kho_tong được cộng trực tiếp ngay khi lưu phiếu.</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 * 
 * <pre>
 *   POST /api/admin/phieu-nhap        → Tạo phiếu nhập kho mới (cộng kho_tong ngay)
 *   GET  /api/admin/phieu-nhap        → Danh sách phiếu nhập (phân trang, mới nhất lên đầu)
 *   GET  /api/admin/phieu-nhap/{id}   → Chi tiết 1 phiếu nhập (kèm danh sách dòng)
 *   GET  /api/admin/phieu-nhap/ton-kho/{khoId}  → Xem tồn kho của 1 kho
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/phieu-nhap")
@RequiredArgsConstructor
public class PhieuNhapKhoController {

    private static final Logger log = LoggerFactory.getLogger(PhieuNhapKhoController.class);

    private final IKhoService khoService;

    // =========================================================================
    // POST /api/admin/phieu-nhap — Tạo phiếu nhập kho mới
    // =========================================================================

    /**
     * Tạo mới phiếu nhập kho và cộng tồn kho trực tiếp vào Kho Tổng (chốt luôn).
     *
     * <h3>Request Body mẫu:</h3>
     * <pre>{@code
     * {
     * "khoId": 1,
     * "nhaCungCapId": 3,
     * "ghiChu": "Nhập lô iPhone 16 từ FPT Trading",
     * "chiTiets": [
     * { "bienTheSanPhamId": 5, "soLuong": 20, "donGiaNhap": 22000000 },
     * { "bienTheSanPhamId": 8, "soLuong": 10, "donGiaNhap": 18500000 }
     * ]
     * }
     * }</pre>
     *
     * @param request DTO chứa thông tin phiếu nhập.
     * @param sessionUser Nhân viên đang đăng nhập (lấy từ session).
     * @return HTTP 201 Created kèm phiếu nhập vừa tạo.
     * HTTP 400 nếu kho không phải kho_tong, soLuong <= 0, hoặc danh sách rỗng.
     * HTTP 404 nếu không tìm thấy kho, biến thể, hoặc NCC.
     */
    @PostMapping
    public ResponseEntity<?> taoPhieuNhapKho(
            @RequestBody TaoPhieuNhapKhoRequest request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuNhapKho] ▶ Tạo phiếu nhập — khoId={}, NCC={}, nhanVienId={}, {} dòng CT",
                request.getKhoId(), request.getNhaCungCapId(),
                sessionUser.getId(),
                request.getChiTiets() == null ? 0 : request.getChiTiets().size());
        try {
            PhieuNhapKho phieu = khoService.taoPhieuNhapKho(request, sessionUser.getId());

            log.info("[PhieuNhapKho] ✅ Tạo thành công — maPhieu={}, tongTien={}",
                    phieu.getMaPhieu(), phieu.getTongTien());
            // Vẫn trả về entity cho endpoint tạo phiếu (không có vấn đề serialize vì không
            // có vòng lặp sâu)
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(buildSuccessResponse("Tạo phiếu nhập kho thành công. Tồn kho đã được cộng.", phieu));

        } catch (IllegalArgumentException e) {
            log.warn("[PhieuNhapKho] ❌ Lỗi nghiệp vụ (400): {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[PhieuNhapKho] ❌ Không tìm thấy entity (404): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("[PhieuNhapKho] ❌ Lỗi không mong đợi: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("Lỗi hệ thống. Vui lòng thử lại."));
        }
    }

    // =========================================================================
    // GET /api/admin/phieu-nhap — Danh sách phiếu nhập (phân trang)
    // =========================================================================

    /**
     * Lấy danh sách phiếu nhập kho có phân trang — dùng cho màn hình lịch sử nhập
     * kho.
     * <p>
     * Ví dụ:
     * <ul>
     * <li>{@code GET /api/admin/phieu-nhap?page=0&size=10} — Trang đầu, 10 bản
     * ghi</li>
     * <li>{@code GET /api/admin/phieu-nhap?page=1&size=20&sort=ngayNhap,asc}</li>
     * </ul>
     *
     * <p>
     * <b>Lưu ý:</b> Trả về DTO {@link PhieuNhapKhoDto} để tránh lỗi serialize
     * Hibernate proxy.
     *
     * @param page        Số trang, bắt đầu từ 0 (mặc định 0).
     * @param size        Số bản ghi mỗi trang (mặc định 10).
     * @param sort        Sắp xếp — ví dụ {@code "ngayNhap,desc"} (mặc định mới nhất
     *                    lên đầu).
     * @param sessionUser NhanVien/Admin đang đăng nhập (xác thực qua session).
     * @return HTTP 200 kèm Page&lt;PhieuNhapKhoDto&gt;.
     */
    @GetMapping
    public ResponseEntity<?> layDanhSachPhieuNhap(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayNhap,desc") String sort,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuNhapKho] Lấy danh sách — page={}, size={}, nhanVienId={}",
                page, size, sessionUser.getId());

        Sort sortObj = buildSort(sort);
        Page<PhieuNhapKho> entityPage = khoService.layDanhSachPhieuNhap(
                PageRequest.of(page, size, sortObj));

        // MAP sang DTO để tránh lỗi serialize Hibernate proxy
        Page<PhieuNhapKhoDto> dtoPage = entityPage.map(this::mapToDto);

        return ResponseEntity.ok(dtoPage);
    }

    // =========================================================================
    // GET /api/admin/phieu-nhap/{id} — Chi tiết phiếu nhập
    // =========================================================================

    /**
     * Lấy chi tiết một phiếu nhập kho kèm toàn bộ dòng chi tiết sản phẩm.
     *
     * <p>
     * <b>Lưu ý:</b> Trả về DTO {@link PhieuNhapKhoDto} để tránh lỗi serialize
     * Hibernate proxy.
     *
     * @param id          ID phiếu nhập kho cần xem.
     * @param sessionUser NhanVien/Admin đang đăng nhập.
     * @return HTTP 200 kèm {@link PhieuNhapKhoDto} chi tiết; HTTP 404 nếu không tìm
     *         thấy.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTietPhieuNhap(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuNhapKho] Lấy chi tiết — id={}, nhanVienId={}", id, sessionUser.getId());
        try {
            PhieuNhapKho entity = khoService.layChiTietPhieuNhap(id);
            PhieuNhapKhoDto dto = mapToDto(entity);
            return ResponseEntity.ok(buildSuccessResponse("OK", dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/admin/phieu-nhap/ton-kho/{khoId} — Xem tồn kho của 1 kho
    // =========================================================================

    /**
     * Lấy toàn bộ danh sách tồn kho của một kho cụ thể (eager-load biến thể).
     * Hữu ích để nhân viên kiểm tra trước khi lập phiếu chuyển kho.
     *
     * @param khoId ID kho cần xem tồn kho.
     * @return HTTP 200 kèm danh sách TonKho.
     *         HTTP 404 nếu không tìm thấy kho.
     */
    @GetMapping("/ton-kho/{khoId}")
    public ResponseEntity<?> xemTonKhoTheoKho(
            @PathVariable Integer khoId,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuNhapKho] Xem tồn kho — khoId={}, nhanVienId={}", khoId, sessionUser.getId());
        try {
            return ResponseEntity.ok(khoService.getTonKhoByKho(khoId));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // Exception Handler cục bộ
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Parse chuỗi sort "field,direction" thành {@link Sort}.
     * Mặc định sort theo {@code ngayNhap DESC} nếu format không hợp lệ.
     */
    private Sort buildSort(String sortParam) {
        try {
            String[] parts = sortParam.split(",");
            Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return Sort.by(dir, parts[0].trim());
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, "ngayNhap");
        }
    }

    /**
     * Chuyển đổi Entity {@link PhieuNhapKho} sang DTO {@link PhieuNhapKhoDto}.
     * <p>
     * Mục đích: Tránh lỗi serialize khi Jackson gặp Hibernate proxy
     * (ByteBuddyInterceptor)
     * do lazy loading. DTO chỉ chứa các trường đơn giản, không có proxy.
     *
     * @param entity Entity phiếu nhập kho cần chuyển đổi.
     * @return DTO phiếu nhập kho.
     */
    private PhieuNhapKhoDto mapToDto(PhieuNhapKho entity) {
        if (entity == null) {
            return null;
        }

        PhieuNhapKhoDto.PhieuNhapKhoDtoBuilder builder = PhieuNhapKhoDto.builder()
                .id(entity.getId())
                .maPhieu(entity.getMaPhieu())
                .tenKho(entity.getKho() != null ? entity.getKho().getTenKho() : "N/A")
                .tenNhaCungCap(entity.getNhaCungCap() != null ? entity.getNhaCungCap().getTenNcc() : null)
                .tenNguoiTao(entity.getNguoiTao() != null ? entity.getNguoiTao().getHoTen() : null)
                .ngayNhap(entity.getNgayNhap())
                .tongTien(entity.getTongTien())
                .ghiChu(entity.getGhiChu());

        // Map danh sách chi tiết nếu có
        if (entity.getChiTietPhieuNhaps() != null && !entity.getChiTietPhieuNhaps().isEmpty()) {
            List<ChiTietPhieuNhapDto> chiTietDtos = entity.getChiTietPhieuNhaps().stream()
                    .map(ct -> ChiTietPhieuNhapDto.builder()
                            .id(ct.getId())
                            .maSku(ct.getBienTheSanPham() != null ? ct.getBienTheSanPham().getMaSku() : "N/A")
                            .tenSanPham(ct.getBienTheSanPham() != null && ct.getBienTheSanPham().getSanPham() != null
                                    ? ct.getBienTheSanPham().getSanPham().getTenSanPham()
                                    : null)
                            .soLuong(ct.getSoLuong())
                            .donGiaNhap(ct.getDonGiaNhap())
                            .thanhTien(ct.getThanhTien())
                            .build())
                    .collect(Collectors.toList());
            builder.chiTiets(chiTietDtos);
        }

        return builder.build();
    }

    /**
     * Tạo response body thành công theo chuẩn thống nhất.
     */
    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", message);
        res.put("data", data);
        return res;
    }

    /**
     * Tạo response body lỗi theo chuẩn thống nhất.
     */
    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}