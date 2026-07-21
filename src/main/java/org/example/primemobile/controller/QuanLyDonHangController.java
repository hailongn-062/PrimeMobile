package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.dto.kho.ImeiDto;
import org.example.primemobile.dto.order.ChiTietDonHangDto;
import org.example.primemobile.dto.order.DonHangChiTietDto;
import org.example.primemobile.dto.request.XacNhanDonHangImeiRequest;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.ChiTietDonHang;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.SanPham;
import org.example.primemobile.service.IQuanLyDonHangService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST Controller cho phân hệ Quản lý Đơn Hàng (dành cho Nhân viên / Admin).
 * <p>
 * Base path: {@code /api/admin/don-hang} — được bảo vệ tự động bởi
 * {@link org.example.primemobile.interceptor.AuthInterceptor} (pattern {@code /api/admin/**}).
 * Mọi request phải có session hợp lệ với vai trò {@code NhanVien} hoặc {@code Admin}.
 *
 * <h3>Endpoints:</h3>
 * <pre>
 *   GET  /api/admin/don-hang              → Danh sách đơn hàng (phân trang, lọc)
 *   GET  /api/admin/don-hang/{id}         → Chi tiết 1 đơn hàng kèm sản phẩm
 *   POST /api/admin/don-hang/{id}/xac-nhan      → Xác nhận đơn (trừ kho_tong)
 *   POST /api/admin/don-hang/{id}/xac-nhan-imei → Xác nhận đơn có chọn IMEI
 *   PUT  /api/admin/don-hang/{id}/trang-thai    → Cập nhật lộ trình giao hàng
 *   POST /api/admin/don-hang/{id}/huy           → Hủy đơn (kèm hoàn kho nếu cần)
 *   PATCH /api/admin/don-hang/{id}/thanh-toan   → Xác nhận đã thanh toán (COD)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin/don-hang")
@RequiredArgsConstructor
public class QuanLyDonHangController {

    private static final Logger log = LoggerFactory.getLogger(QuanLyDonHangController.class);

    private final IQuanLyDonHangService quanLyDonHangService;

    // =========================================================================
    // GET /api/admin/don-hang — Danh sách đơn hàng
    // =========================================================================

    /**
     * Lấy danh sách đơn hàng có phân trang, hỗ trợ lọc đa tiêu chí.
     *
     * <h3>Query params:</h3>
     * <ul>
     *   <li>{@code trangThai}   — Lọc theo trạng thái (null = tất cả)</li>
     *   <li>{@code maDonHang}   — Tìm LIKE theo mã đơn (null = bỏ qua)</li>
     *   <li>{@code soDienThoai} — Tìm LIKE theo SĐT khách (null = bỏ qua)</li>
     *   <li>{@code page}        — Số trang (0-indexed, mặc định = 0)</li>
     *   <li>{@code size}        — Kích thước trang (mặc định = 20)</li>
     * </ul>
     *
     * Ví dụ: {@code GET /api/admin/don-hang?trangThai=cho_xac_nhan&page=0&size=10}
     */
    @GetMapping
    public ResponseEntity<?> layDanhSachDonHang(
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String maDonHang,
            @RequestParam(required = false) String soDienThoai,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] Lấy danh sách — nhanVienId={}, trangThai={}, page={}/{}",
                sessionUser.getId(), trangThai, page, size);

        // Sắp xếp mặc định: đơn hàng mới nhất lên đầu
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "ngayDat"));

        Page<DonHang> ketQua = quanLyDonHangService.layDanhSachDonHang(
                trangThai, maDonHang, soDienThoai, pageable);

        // Build response wrapper với metadata phân trang
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("danhSachDonHang", ketQua.getContent());
        response.put("tongSoDonHang",   ketQua.getTotalElements());
        response.put("tongSoTrang",     ketQua.getTotalPages());
        response.put("trangHienTai",    ketQua.getNumber());
        response.put("kichThuocTrang",  ketQua.getSize());

        return ResponseEntity.ok(response);
    }

    // =========================================================================
    // GET /api/admin/don-hang/{id} — Chi tiết đơn hàng
    // =========================================================================

    /**
     * Lấy chi tiết 1 đơn hàng kèm toàn bộ danh sách sản phẩm bên trong (FETCH JOIN).
     * <p>
     * Trả về DTO {@link DonHangChiTietDto} thay vì entity {@link DonHang}
     * để tránh vòng lặp vô hạn khi serialize JSON (do quan hệ 1-N với ChiTietDonHang).
     *
     * @param id ID của đơn hàng cần xem.
     * @return HTTP 200 kèm {@link DonHangChiTietDto} đã load đầy đủ.
     *         HTTP 404 nếu không tìm thấy.
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> layChiTietDonHang(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] Xem chi tiết — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.layChiTietDonHang(id);
            DonHangChiTietDto dto = mapToDonHangChiTietDto(donHang);
            return ResponseEntity.ok(dto);

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] Không tìm thấy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/xac-nhan — Xác nhận đơn hàng
    // =========================================================================

    /**
     * Xác nhận đơn hàng online — bước nghiệp vụ quan trọng nhất.
     * <p>
     * Thực hiện trong 1 transaction:
     * <ol>
     *   <li>Kiểm tra trạng thái phải là {@code "cho_xac_nhan"}.</li>
     *   <li>Fail-Fast: validate toàn bộ SKU xem kho_tong có đủ (Safety Stock §3.1).</li>
     *   <li>Trừ thực tế vào ton_kho của kho_tong.</li>
     *   <li>Chuyển trạng thái đơn → {@code "da_xac_nhan"}.</li>
     * </ol>
     *
     * @param id          ID đơn hàng cần xác nhận.
     * @param sessionUser Nhân viên đang đăng nhập (lấy từ session, không cần truyền body).
     * @return HTTP 200 kèm đơn hàng đã xác nhận.
     *         HTTP 400 nếu sai trạng thái hoặc vi phạm Safety Stock.
     *         HTTP 404 nếu đơn hoặc kho không tồn tại.
     */
    @PostMapping("/{id}/xac-nhan")
    public ResponseEntity<?> xacNhanDonHang(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Xác nhận đơn — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.xacNhanDonHang(id, sessionUser.getId());
            log.info("[QuanLyDonHang] ✅ Xác nhận thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Xác nhận đơn hàng thành công. kho t?ng đã được trừ.", donHang));

        } catch (IllegalArgumentException e) {
            // Sai trạng thái hoặc vi phạm Safety Stock §3.1
            log.warn("[QuanLyDonHang] ❌ Xác nhận thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy tài nguyên khi xác nhận: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/xac-nhan-imei — Xác nhận đơn với IMEI
    // =========================================================================

    /**
     * Xác nhận đơn hàng online với danh sách IMEI được nhân viên chọn.
     * <p>
     * Thực hiện trong 1 transaction:
     * <ol>
     *   <li>Kiểm tra trạng thái phải là {@code "cho_xac_nhan"}.</li>
     *   <li>Fail-Fast: Validate toàn bộ IMEI (tồn tại, trong kho, ở kho t?ng, đúng biến thể).</li>
     *   <li>Kiểm tra tồn kho kho t?ng đủ (Safety Stock §3.1).</li>
     *   <li>Cập nhật IMEI → {@code "da_ban"}, gán đơn hàng.</li>
     *   <li>Trừ thực tế vào ton_kho của kho_tong.</li>
     *   <li>Chuyển trạng thái đơn → {@code "da_xac_nhan"}.</li>
     * </ol>
     *
     * @param id          ID đơn hàng cần xác nhận.
     * @param request     DTO chứa danh sách IMEI cho từng chi tiết đơn hàng.
     * @param sessionUser Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã xác nhận.
     *         HTTP 400 nếu sai trạng thái, IMEI không hợp lệ, hoặc vi phạm Safety Stock.
     *         HTTP 404 nếu đơn, chi tiết đơn, hoặc IMEI không tồn tại.
     */
    @PostMapping("/{id}/xac-nhan-imei")
    public ResponseEntity<?> xacNhanDonHangVoiImei(
            @PathVariable Integer id,
            @RequestBody XacNhanDonHangImeiRequest request,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Xác nhận đơn với IMEI — donHangId={}, nhanVienId={}, {} selections",
                id, sessionUser.getId(),
                request.getSelections() == null ? 0 : request.getSelections().size());

        try {
            // Chuyển đổi từ XacNhanDonHangImeiRequest.ImeiSelection sang IQuanLyDonHangService.ImeiSelection
            List<IQuanLyDonHangService.ImeiSelection> imeiSelections = request.getSelections().stream()
                    .map(sel -> {
                        IQuanLyDonHangService.ImeiSelection s = new IQuanLyDonHangService.ImeiSelection();
                        s.setChiTietDonHangId(sel.getChiTietDonHangId());
                        s.setImeiList(sel.getImeiList());
                        return s;
                    })
                    .collect(Collectors.toList());

            DonHang donHang = quanLyDonHangService.xacNhanDonHangVoiImei(
                    id, sessionUser.getId(), imeiSelections);

            log.info("[QuanLyDonHang] ✅ Xác nhận với IMEI thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Xác nhận đơn hàng thành công. Đã cập nhật IMEI và trừ kho t?ng.", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Xác nhận với IMEI thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy tài nguyên khi xác nhận IMEI: {}", e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // PATCH /api/admin/don-hang/{id}/thanh-toan — Xác nhận đã thanh toán (COD)
    // =========================================================================

    /**
     * Xác nhận đã thanh toán cho đơn hàng COD (Cash on Delivery).
     * <p>
     * Chỉ áp dụng khi đơn hàng đã ở trạng thái {@code da_hoan_thanh}
     * và trạng thái thanh toán {@code chua_thanh_toan}.
     * <p>
     * Ví dụ: {@code PATCH /api/admin/don-hang/5/thanh-toan}
     *
     * @param id          ID đơn hàng cần xác nhận thanh toán.
     * @param sessionUser Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã cập nhật.
     *         HTTP 400 nếu đơn không ở trạng thái {@code da_hoan_thanh} hoặc đã thanh toán.
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PatchMapping("/{id}/thanh-toan")
    public ResponseEntity<?> xacNhanThanhToan(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Xác nhận thanh toán — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());

        try {
            DonHang donHang = quanLyDonHangService.xacNhanThanhToan(id);
            log.info("[QuanLyDonHang] ✅ Xác nhận thanh toán thành công — maDonHang={}",
                    donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Xác nhận thanh toán thành công.", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Xác nhận thanh toán thất bại donHangId={}: {}",
                    id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy đơn hàng id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // PUT /api/admin/don-hang/{id}/trang-thai — Cập nhật lộ trình giao hàng
    // =========================================================================

    /**
     * Cập nhật lộ trình giao hàng theo chiều tiến: da_xac_nhan → dang_giao → da_hoan_thanh.
     * <p>
     * Ví dụ: {@code PUT /api/admin/don-hang/5/trang-thai?trangThaiMoi=dang_giao}
     *
     * @param id           ID đơn hàng cần cập nhật.
     * @param trangThaiMoi Trạng thái mới muốn chuyển sang ("dang_giao" hoặc "da_hoan_thanh").
     * @param sessionUser  Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã cập nhật.
     *         HTTP 400 nếu luồng chuyển trạng thái không hợp lệ.
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<?> capNhatTrangThai(
            @PathVariable  Integer id,
            @RequestParam  String  trangThaiMoi,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Cập nhật trạng thái — donHangId={}, trangThaiMoi={}, nhanVienId={}",
                id, trangThaiMoi, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.capNhatTrangThai(id, trangThaiMoi);
            log.info("[QuanLyDonHang] ✅ Cập nhật thành công — maDonHang={}, trangThai={}",
                    donHang.getMaDonHang(), donHang.getTrangThai());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Cập nhật trạng thái giao hàng thành công.", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Cập nhật trạng thái thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/huy — Hủy đơn hàng
    // =========================================================================

    /**
     * Hủy đơn hàng kèm hoàn kho tự động nếu cần (system_rules.md §2.2.7).
     * <p>
     * Body JSON (optional):
     * <pre>{@code
     * {
     *   "lyDoHuy": "Khách yêu cầu hủy qua điện thoại"
     * }
     * }</pre>
     * Nếu không truyền body, lý do hủy mặc định là "Nhân viên hủy đơn".
     *
     * @param id          ID đơn hàng cần hủy.
     * @param body        Map chứa field {@code lyDoHuy} (optional).
     * @param sessionUser Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã hủy.
     *         HTTP 400 nếu đơn không thể hủy (đã giao hoặc đã hủy trước đó).
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PostMapping("/{id}/huy")
    public ResponseEntity<?> huyDonHang(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        String lyDoHuy = (body != null && body.containsKey("lyDoHuy"))
                ? body.get("lyDoHuy")
                : "Nhân viên hủy đơn (ID: " + sessionUser.getId() + ")";

        log.info("[QuanLyDonHang] ▶ Hủy đơn — donHangId={}, nhanVienId={}, lyDo={}",
                id, sessionUser.getId(), lyDoHuy);
        try {
            DonHang donHang = quanLyDonHangService.huyDonHang(id, lyDoHuy);
            log.info("[QuanLyDonHang] ✅ Hủy thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Hủy đơn hàng thành công. kho t?ng đã được hoàn trả (nếu cần).", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Không thể hủy đơn hàng id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy tài nguyên khi hủy id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // POST /api/admin/don-hang/{id}/xac-nhan-hoan-tien — Xác nhận hoàn tiền
    // =========================================================================

    /**
     * Xác nhận hoàn tiền cho đơn hàng (đã hủy và chờ hoàn tiền).
     *
     * @param id          ID đơn hàng cần xác nhận hoàn tiền.
     * @param sessionUser Nhân viên đang đăng nhập.
     * @return HTTP 200 kèm đơn hàng đã cập nhật.
     *         HTTP 400 nếu sai trạng thái.
     *         HTTP 404 nếu đơn hàng không tồn tại.
     */
    @PostMapping("/{id}/xac-nhan-hoan-tien")
    public ResponseEntity<?> xacNhanHoanTien(
            @PathVariable Integer id,
            @SessionAttribute("CURRENT_ADMIN") SessionUser sessionUser) {

        log.info("[QuanLyDonHang] ▶ Xác nhận hoàn tiền — donHangId={}, nhanVienId={}",
                id, sessionUser.getId());
        try {
            DonHang donHang = quanLyDonHangService.xacNhanHoanTien(id, sessionUser.getId());
            log.info("[QuanLyDonHang] ✅ Xác nhận hoàn tiền thành công — maDonHang={}", donHang.getMaDonHang());
            return ResponseEntity.ok(buildSuccessResponse(
                    "Xác nhận hoàn tiền thành công. Trạng thái đã chuyển thành Đã hủy và Đã hoàn tiền.", donHang));

        } catch (IllegalArgumentException e) {
            log.warn("[QuanLyDonHang] ❌ Xác nhận hoàn tiền thất bại donHangId={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));

        } catch (EntityNotFoundException e) {
            log.warn("[QuanLyDonHang] ❌ Không tìm thấy đơn hàng khi xác nhận hoàn tiền id={}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    // =========================================================================
    // Exception Handler cục bộ (fallback)
    // =========================================================================

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("[QuanLyDonHang] Handler toàn cục — IllegalArgumentException: {}", e.getMessage());
        return ResponseEntity.badRequest().body(buildErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        log.warn("[QuanLyDonHang] Handler toàn cục — IllegalStateException: {}", e.getMessage());
        return ResponseEntity.internalServerError().body(buildErrorResponse(e.getMessage()));
    }

    // =========================================================================
    // PRIVATE HELPERS
    // =========================================================================

    /**
     * Chuyển đổi entity {@link DonHang} sang DTO {@link DonHangChiTietDto}.
     * <p>
     * Mục đích: Tránh vòng lặp vô hạn khi Jackson serialize entity
     * (do quan hệ 1-N giữa DonHang và ChiTietDonHang).
     * Đồng thời kiểm soát dữ liệu trả về API, chỉ expose các trường cần thiết.
     *
     * @param donHang Entity đơn hàng (đã load chi tiết).
     * @return DTO đơn hàng chi tiết.
     */
    private DonHangChiTietDto mapToDonHangChiTietDto(DonHang donHang) {
        if (donHang == null) {
            return null;
        }

        // Lấy danh sách IMEI đã gán cho đơn hàng (trạng thái 'da_ban') trước
        List<org.example.primemobile.entity.MayDienThoai> tempMayDienThoaiList = new java.util.ArrayList<>();
        try {
            tempMayDienThoaiList = quanLyDonHangService.layDanhSachImeiTheoDonHang(donHang.getId());
        } catch (Exception e) {
            log.warn("[QuanLyDonHang] Không thể lấy danh sách IMEI cho đơn hàng {}: {}", donHang.getId(), e.getMessage());
        }
        final List<org.example.primemobile.entity.MayDienThoai> mayDienThoaiList = tempMayDienThoaiList;

        // Map danh sách chi tiết đơn hàng
        List<ChiTietDonHangDto> chiTietDtos = donHang.getChiTietDonHangs().stream()
                .map(ct -> mapChiTietToDto(ct, mayDienThoaiList))
                .collect(Collectors.toList());

        // Lấy thông tin khách hàng
        String tenKhachHang = donHang.getKhachHang() != null
                ? donHang.getKhachHang().getHoTen()
                : donHang.getHoTenNguoiNhan();

        String soDienThoaiKhach = donHang.getKhachHang() != null
                ? donHang.getKhachHang().getSoDienThoai()
                : donHang.getSdtNguoiNhan();

        String emailKhach = null;
        if (donHang.getKhachHang() != null) {
            emailKhach = donHang.getKhachHang().getEmail();
            if ((emailKhach == null || emailKhach.trim().isEmpty()) && donHang.getKhachHang().getNguoiDung() != null) {
                emailKhach = donHang.getKhachHang().getNguoiDung().getEmail();
            }
        }
        if (emailKhach == null || emailKhach.trim().isEmpty()) {
            emailKhach = donHang.getEmailNguoiNhan();
        }
        if (emailKhach != null && emailKhach.trim().isEmpty()) {
            emailKhach = null;
        }

        // Lấy tên nhân viên xử lý
        String tenNguoiXuLy = donHang.getNguoiXuLy() != null
                ? donHang.getNguoiXuLy().getHoTen()
                : null;

        List<ImeiDto> imeiList = mayDienThoaiList.stream()
                .map(m -> new ImeiDto(m.getId(), m.getImei1(), m.getImei2(), m.getTinhTrang()))
                .collect(Collectors.toList());

        return DonHangChiTietDto.builder()
                .id(donHang.getId())
                .maDonHang(donHang.getMaDonHang())
                .ngayDat(donHang.getNgayDat())
                .trangThai(donHang.getTrangThai())
                .trangThaiThanhToan(donHang.getTrangThaiThanhToan())
                .kenhBan(donHang.getKenhBan())
                .tenKhachHang(tenKhachHang)
                .soDienThoaiKhach(soDienThoaiKhach)
                .emailKhach(emailKhach)
                .hoTenNguoiNhan(donHang.getHoTenNguoiNhan())
                .sdtNguoiNhan(donHang.getSdtNguoiNhan())
                .diaChiGiaoCuThe(donHang.getDiaChiGiaCuThe())
                .phuongXaGiao(donHang.getPhuongXaGiao())
                .quanHuyenGiao(donHang.getQuanHuyenGiao())
                .tinhThanhGiao(donHang.getTinhThanhGiao())
                .ngayGiaoDuKien(donHang.getNgayGiaoDuKien())
                .ngayGiaoThucTe(donHang.getNgayGiaoThucTe())
                .tenNguoiXuLy(tenNguoiXuLy)
                .tongTienHang(donHang.getTongTienHang())
                .tienGiamGia(donHang.getTienGiamGia())
                .phiShip(donHang.getPhiShip())
                .tongThanhToan(donHang.getTongThanhToan())
                .ghiChu(donHang.getGhiChu())
                .imeiList(imeiList)
                .chiTietDonHangs(chiTietDtos)
                .build();
    }

    /**
     * Chuyển đổi entity {@link ChiTietDonHang} sang DTO {@link ChiTietDonHangDto}.
     */
    private ChiTietDonHangDto mapChiTietToDto(ChiTietDonHang chiTiet, List<org.example.primemobile.entity.MayDienThoai> mayDienThoaiList) {
        BienTheSanPham bt = chiTiet.getBienTheSanPham();
        SanPham sp = (bt != null) ? bt.getSanPham() : null;

        List<ImeiDto> ctImeis = new java.util.ArrayList<>();
        if (mayDienThoaiList != null && bt != null) {
            ctImeis = mayDienThoaiList.stream()
                    .filter(m -> m.getBienTheSanPham() != null && m.getBienTheSanPham().getId().equals(bt.getId()))
                    .map(m -> new ImeiDto(m.getId(), m.getImei1(), m.getImei2(), m.getTinhTrang()))
                    .collect(Collectors.toList());
        }

        return ChiTietDonHangDto.builder()
                .id(chiTiet.getId())
                .soLuong(chiTiet.getSoLuong())
                .donGiaBan(chiTiet.getDonGiaBan())
                .giaGoc(bt != null && bt.getGiaBan() != null ? bt.getGiaBan() : chiTiet.getDonGiaBan())
                .thanhTien(chiTiet.getThanhTien())
                .bienTheSanPhamId(bt != null ? bt.getId() : null)
                .maSku(bt != null ? bt.getMaSku() : null)
                .mauSac(bt != null ? bt.getMauSac() : null)
                .ramGb(bt != null ? bt.getRamGb() : null)
                .luuTruGb(bt != null ? bt.getLuuTruGb() : null)
                .tenSanPham(sp != null ? sp.getTenSanPham() : null)
                .imeiList(ctImeis)
                .build();
    }

    private Map<String, Object> buildSuccessResponse(String message, DonHang donHang) {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", true);
        res.put("message", message);
        if (donHang != null) {
            res.put("donHangId", donHang.getId());
        }
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
