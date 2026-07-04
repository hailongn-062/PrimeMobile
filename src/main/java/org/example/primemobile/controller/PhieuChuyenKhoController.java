package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.kho.TaoPhieuChuyenKhoRequest;
import org.example.primemobile.dto.kho.TonKhoDto;
import org.example.primemobile.dto.phieuchuyen.ChiTietChuyenKhoDto;
import org.example.primemobile.dto.phieuchuyen.PhieuChuyenKhoDto;
import org.example.primemobile.entity.PhieuChuyenKho;
import org.example.primemobile.entity.TonKho;
import org.example.primemobile.service.IKhoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller cho phân hệ Phiếu Chuyển Kho (Transfer).
 * <p>
 * Base path: {@code /api/admin/phieu-chuyen} — được bảo vệ bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (pattern
 * {@code /api/admin/**}).
 *
 * <h3>Quy tắc nghiệp vụ (system_rules.md §3.1, §3.2):</h3>
 * <ul>
 * <li>Luồng tiêu chuẩn: <b>Kho Tổng → Kho Online</b> (bổ sung hàng cho đơn
 * online).</li>
 * <li><b>Safety Stock Rule §3.1:</b> Sau khi trừ, tồn kho kho nguồn KHÔNG ĐƯỢC
 * dưới 5 đơn vị/SKU.</li>
 * <li>Phiếu chuyển <b>CHỐT LUÔN</b> khi tạo — không qua bước duyệt.</li>
 * <li>Tồn kho 2 bên được cập nhật ngay lập tức khi lưu phiếu.</li>
 * <li><b>Hỗ trợ chọn IMEI cụ thể khi chuyển kho:</b> Nếu request có danh sách IMEI,
 * hệ thống sẽ cập nhật kho_id của các IMEI đó sang kho đích (số lượng IMEI phải bằng soLuong).</li>
 * </ul>
 *
 * <h3>Endpoints:</h3>
 *
 * <pre>
 *   POST /api/admin/phieu-chuyen       → Tạo phiếu chuyển kho (kiểm tra Safety Stock, trừ nguồn, cộng đích)
 *   GET  /api/admin/phieu-chuyen       → Danh sách phiếu chuyển (phân trang, mới nhất lên đầu)
 *   GET  /api/admin/phieu-chuyen/{id}  → Chi tiết 1 phiếu chuyển (kèm danh sách dòng)
 *   GET  /api/admin/phieu-chuyen/ton-kho/{khoId}/{bienTheId}  → Xem tồn kho 1 SKU tại 1 kho
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/phieu-chuyen")
@RequiredArgsConstructor
public class PhieuChuyenKhoController {

    private static final Logger log = LoggerFactory.getLogger(PhieuChuyenKhoController.class);

    private final IKhoService khoService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // =========================================================================
    // POST /api/admin/phieu-chuyen — Tạo phiếu chuyển kho
    // =========================================================================

    /**
     * Tạo phiếu chuyển kho, kiểm tra Safety Stock, trừ kho nguồn và cộng kho đích
     * (chốt luôn). Hỗ trợ chọn IMEI cụ thể để cập nhật kho_id.
     *
     * <h3>Request Body mẫu (Kho Tổng → Kho Online) với IMEI:</h3>
     * <pre>{@code
     * {
     *   "khoNguonId": 1,
     *   "khoDichId": 2,
     *   "lyDo": "Bổ sung hàng kho online cho mùa Flash Sale",
     *   "chiTiets": [
     *     {
     *       "bienTheSanPhamId": 5,
     *       "soLuong": 2,
     *       "imeiList": ["IMEI_001", "IMEI_002"]
     *     },
     *     {
     *       "bienTheSanPhamId": 8,
     *       "soLuong": 1,
     *       "imeiList": ["IMEI_003"]
     *     }
     *   ]
     * }
     * }</pre>
     *
     * @param request DTO chứa thông tin phiếu chuyển (có thể bao gồm danh sách IMEI).
     * @param sessionUser Nhân viên đang đăng nhập (lấy từ session).
     * @return HTTP 201 Created kèm phiếu chuyển vừa tạo.
     * HTTP 400 nếu vi phạm Safety Stock, khoNguon = khoDich, soLuong <= 0,
     * hoặc IMEI không hợp lệ.
     * HTTP 404 nếu không tìm thấy kho, biến thể, tồn kho, hoặc IMEI.
     */
    @PostMapping
    public ResponseEntity<?> taoPhieuChuyenKho(
            @RequestBody TaoPhieuChuyenKhoRequest request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        // Đếm tổng số IMEI trong request (để log)
        int totalImei = 0;
        if (request.getChiTiets() != null) {
            totalImei = request.getChiTiets().stream()
                    .filter(ct -> ct.getImeiList() != null)
                    .mapToInt(ct -> ct.getImeiList().size())
                    .sum();
        }

        log.info("[PhieuChuyenKho] ▶ Tạo phiếu chuyển — nguonId={}, dichId={}, nhanVienId={}, {} dòng CT, {} IMEI",
                request.getKhoNguonId(), request.getKhoDichId(),
                sessionUser.getId(),
                request.getChiTiets() == null ? 0 : request.getChiTiets().size(),
                totalImei);

        try {
            PhieuChuyenKho phieu = khoService.taoPhieuChuyenKho(request, sessionUser.getId());

            log.info("[PhieuChuyenKho] ✅ Tạo thành công — maPhieu={}", phieu.getMaPhieu());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(buildSuccessResponse(
                            "Tạo phiếu chuyển kho thành công. Tồn kho hai đầu đã được cập nhật.", phieu));

        } catch (IllegalArgumentException e) {
            // Vi phạm Safety Stock §3.1, khoNguon = khoDich, soLuong <= 0, hoặc IMEI không hợp lệ
            log.warn("[PhieuChuyenKho] ❌ Lỗi nghiệp vụ (400): {}", e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            // Không tìm thấy kho, biến thể, tồn kho, hoặc IMEI
            log.warn("[PhieuChuyenKho] ❌ Không tìm thấy entity (404): {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));

        } catch (Exception e) {
            log.error("[PhieuChuyenKho] ❌ Lỗi không mong đợi: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(buildErrorResponse("Lỗi hệ thống. Vui lòng thử lại."));
        }
    }

    // =========================================================================
    // GET /api/admin/phieu-chuyen — Danh sách phiếu chuyển (phân trang)
    // =========================================================================

    /**
     * Lấy danh sách phiếu chuyển kho có phân trang — dùng cho màn hình lịch sử
     * chuyển kho.
     * <p>
     * Ví dụ:
     * <ul>
     * <li>{@code GET /api/admin/phieu-chuyen?page=0&size=10}</li>
     * <li>{@code GET /api/admin/phieu-chuyen?page=0&size=20&sort=ngayChuyen,asc}</li>
     * </ul>
     * <p>
     * <b>Lưu ý:</b> Trả về DTO {@link PhieuChuyenKhoDto} để tránh lỗi serialize Hibernate proxy.
     *
     * @param page        Số trang, bắt đầu từ 0 (mặc định 0).
     * @param size        Số bản ghi mỗi trang (mặc định 10).
     * @param sort        Sắp xếp — ví dụ {@code "ngayChuyen,desc"} (mặc định mới
     *                    nhất lên đầu).
     * @param sessionUser NhanVien/Admin đang đăng nhập.
     * @return HTTP 200 kèm Page&lt;PhieuChuyenKhoDto&gt;.
     */
    @GetMapping
    public ResponseEntity<?> layDanhSachPhieuChuyen(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "ngayChuyen,desc") String sort,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuChuyenKho] Lấy danh sách — page={}, size={}, nhanVienId={}",
                page, size, sessionUser.getId());

        Sort sortObj = buildSort(sort, "ngayChuyen");
        Page<PhieuChuyenKho> entityPage = khoService.layDanhSachPhieuChuyen(
                PageRequest.of(page, size, sortObj));

        // MAP sang DTO để tránh lỗi serialize Hibernate proxy
        Page<PhieuChuyenKhoDto> dtoPage = entityPage.map(this::mapToDto);

        return ResponseEntity.ok(dtoPage);
    }

    // =========================================================================
    // GET /api/admin/phieu-chuyen/{id} — Chi tiết phiếu chuyển
    // =========================================================================

    /**
     * Lấy chi tiết một phiếu chuyển kho kèm toàn bộ dòng chi tiết sản phẩm.
     * <p>
     * <b>Lưu ý:</b> Trả về DTO {@link PhieuChuyenKhoDto} để tránh lỗi serialize Hibernate proxy.
     *
     * @param id          ID phiếu chuyển kho cần xem.
     * @param sessionUser NhanVien/Admin đang đăng nhập.
     * @return HTTP 200 kèm {@link PhieuChuyenKhoDto} chi tiết; HTTP 404 nếu không tìm
     *         thấy.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTietPhieuChuyen(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuChuyenKho] Lấy chi tiết — id={}, nhanVienId={}", id, sessionUser.getId());
        try {
            PhieuChuyenKho entity = khoService.layChiTietPhieuChuyen(id);
            PhieuChuyenKhoDto dto = mapToDto(entity);
            return ResponseEntity.ok(buildSuccessResponse("OK", dto));
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(buildErrorResponse(e.getMessage()));
        }
    }

    // =========================================================================
    // GET /api/admin/phieu-chuyen/ton-kho/{khoId}/{bienTheId} — Kiểm tra tồn kho
    // =========================================================================

    /**
     * Kiểm tra tồn kho của 1 SKU cụ thể tại 1 kho cụ thể.
     * Hỗ trợ nhân viên kiểm tra trước khi quyết định số lượng chuyển.
     * <p>
     * <b>Lưu ý:</b> Trả về DTO {@link TonKhoDto} để tránh lỗi serialize Hibernate
     * proxy.
     *
     * @param khoId       ID kho cần kiểm tra.
     * @param bienTheId   ID biến thể SKU cần kiểm tra.
     * @param sessionUser NhanVien/Admin đang đăng nhập.
     * @return HTTP 200 kèm thông tin {@link TonKhoDto}.
     *         HTTP 404 nếu không tìm thấy bản ghi tồn kho.
     */
    @GetMapping("/ton-kho/{khoId}/{bienTheId}")
    public ResponseEntity<?> xemTonKhoChiTiet(
            @PathVariable Integer khoId,
            @PathVariable Integer bienTheId,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[PhieuChuyenKho] Xem tồn kho — khoId={}, bienTheId={}", khoId, bienTheId);
        try {
            TonKho tonKho = khoService.getTonKhoChiTiet(khoId, bienTheId);
            TonKhoDto dto = mapToDto(tonKho);
            return ResponseEntity.ok(dto);
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
     * Fallback về {@code defaultField DESC} nếu format không hợp lệ.
     */
    private Sort buildSort(String sortParam, String defaultField) {
        try {
            String[] parts = sortParam.split(",");
            Sort.Direction dir = (parts.length > 1 && "asc".equalsIgnoreCase(parts[1]))
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            return Sort.by(dir, parts[0].trim());
        } catch (Exception e) {
            return Sort.by(Sort.Direction.DESC, defaultField);
        }
    }

    /**
     * Chuyển đổi Entity {@link PhieuChuyenKho} sang DTO {@link PhieuChuyenKhoDto}.
     * <p>
     * Mục đích: Tránh lỗi serialize khi Jackson gặp Hibernate proxy (ByteBuddyInterceptor)
     * do lazy loading. DTO chỉ chứa các trường đơn giản, không có proxy.
     *
     * @param entity Entity phiếu chuyển kho cần chuyển đổi.
     * @return DTO phiếu chuyển kho.
     */
    private PhieuChuyenKhoDto mapToDto(PhieuChuyenKho entity) {
        if (entity == null) {
            return null;
        }

        PhieuChuyenKhoDto.PhieuChuyenKhoDtoBuilder builder = PhieuChuyenKhoDto.builder()
                .id(entity.getId())
                .maPhieu(entity.getMaPhieu())
                .tenKhoNguon(entity.getKhoNguon() != null ? entity.getKhoNguon().getTenKho() : "N/A")
                .tenKhoDich(entity.getKhoDich() != null ? entity.getKhoDich().getTenKho() : "N/A")
                .lyDo(entity.getLyDo())
                .ngayChuyen(entity.getNgayChuyen());

        // Map danh sách chi tiết nếu có
        if (entity.getChiTietChuyenKhos() != null && !entity.getChiTietChuyenKhos().isEmpty()) {
            List<ChiTietChuyenKhoDto> chiTietDtos = entity.getChiTietChuyenKhos().stream()
                    .map(ct -> ChiTietChuyenKhoDto.builder()
                            .id(ct.getId())
                            .maSku(ct.getBienTheSanPham() != null ? ct.getBienTheSanPham().getMaSku() : "N/A")
                            .tenSanPham(ct.getBienTheSanPham() != null && ct.getBienTheSanPham().getSanPham() != null
                                    ? ct.getBienTheSanPham().getSanPham().getTenSanPham()
                                    : null)
                            .soLuong(ct.getSoLuong())
                            .build())
                    .collect(Collectors.toList());
            builder.chiTiets(chiTietDtos);
        }

        return builder.build();
    }

    /**
     * Chuyển đổi Entity {@link TonKho} sang DTO {@link TonKhoDto}.
     * <p>
     * Mục đích: Tránh lỗi serialize khi Jackson gặp Hibernate proxy
     * (ByteBuddyInterceptor)
     * do lazy loading. DTO chỉ chứa các trường đơn giản, không có proxy.
     *
     * @param entity Entity tồn kho cần chuyển đổi.
     * @return DTO tồn kho.
     */
    private TonKhoDto mapToDto(TonKho entity) {
        if (entity == null) {
            return null;
        }

        return TonKhoDto.builder()
                .tonKhoId(entity.getId())
                .khoId(entity.getKho() != null ? entity.getKho().getId() : null)
                .tenKho(entity.getKho() != null ? entity.getKho().getTenKho() : "N/A")
                .bienTheSanPhamId(entity.getBienTheSanPham() != null ? entity.getBienTheSanPham().getId() : null)
                .maSku(entity.getBienTheSanPham() != null ? entity.getBienTheSanPham().getMaSku() : "N/A")
                .tenSanPham(entity.getBienTheSanPham() != null && entity.getBienTheSanPham().getSanPham() != null
                        ? entity.getBienTheSanPham().getSanPham().getTenSanPham()
                        : null)
                .soLuong(entity.getSoLuong())
                .updatedAt(entity.getUpdatedAt() != null
                        ? entity.getUpdatedAt().format(DATE_FORMATTER)
                        : null)
                .build();
    }

    private Map<String, Object> buildSuccessResponse(String message, Object data) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", message);
        res.put("data", data);
        return res;
    }

    private Map<String, Object> buildErrorResponse(String message) {
        Map<String, Object> err = new LinkedHashMap<>();
        err.put("success", false);
        err.put("message", message);
        return err;
    }
}