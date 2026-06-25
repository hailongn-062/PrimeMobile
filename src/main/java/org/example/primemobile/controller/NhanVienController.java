package org.example.primemobile.controller;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.service.INhanVienService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST API Controller cho phân hệ Quản lý Nhân Viên — Admin only.
 *
 * <p>Base path: {@code /api/admin/nhan-vien}
 *
 * <p>Tách biệt hoàn toàn với {@link NhanVienUIController} (Thymeleaf).
 * Controller này chỉ xử lý REST JSON — mọi thao tác nghiệp vụ được
 * ủy quyền cho {@link INhanVienService}.
 *
 * <p>Ghi chú (system_rules.md §1):
 * <ul>
 *   <li>Không dùng Spring Security. Phân quyền Admin được kiểm soát bởi
 *       {@code AuthInterceptor} ở tầng request.</li>
 *   <li>Mọi endpoint đều bọc try-catch và trả về {@link ResponseEntity} rõ ràng.</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final INhanVienService nhanVienService;

    // =========================================================================
    // GET /api/admin/nhan-vien — Danh sách nhân viên
    // =========================================================================

    /**
     * Lấy danh sách toàn bộ nhân viên.
     * <p>
     * Query trên bảng nguoi_dung với điều kiện vai_tro = 'NhanVien'.
     *
     * @return 200 OK + danh sách nhân viên | 500 nếu lỗi hệ thống.
     */
    @GetMapping
    public ResponseEntity<?> danhSach() {
        try {
            List<NguoiDung> danhSach = nhanVienService.layDanhSachNhanVien();
            log.info("[NhanVienAPI] GET /api/admin/nhan-vien — trả về {} bản ghi", danhSach.size());
            return ResponseEntity.ok(danhSach);
        } catch (Exception e) {
            log.error("[NhanVienAPI] Lỗi lấy danh sách nhân viên: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // =========================================================================
    // POST /api/admin/nhan-vien — Thêm mới nhân viên
    // =========================================================================

    /**
     * Tạo mới một tài khoản nhân viên.
     * <p>
     * Nghiệp vụ bắt buộc (thực thi ở Service):
     * vaiTro được gán cứng = "NhanVien" — bất kể giá trị client gửi lên.
     *
     * @param nhanVien Body JSON chứa: hoTen, email, soDienThoai, matKhau.
     * @return 201 Created + entity đã lưu | 400 nếu vi phạm nghiệp vụ | 500 nếu lỗi hệ thống.
     */
    @PostMapping
    public ResponseEntity<?> themMoi(@RequestBody NguoiDung nhanVien) {
        try {
            log.info("[NhanVienAPI] POST /api/admin/nhan-vien — email={}", nhanVien.getEmail());
            NguoiDung saved = nhanVienService.themMoi(nhanVien);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            log.warn("[NhanVienAPI] Thêm nhân viên thất bại — vi phạm nghiệp vụ: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[NhanVienAPI] Lỗi hệ thống khi thêm nhân viên: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // =========================================================================
    // PUT /api/admin/nhan-vien/{id} — Cập nhật thông tin nhân viên
    // =========================================================================

    /**
     * Cập nhật thông tin hồ sơ nhân viên (hoTen, email, soDienThoai).
     * <p>
     * vaiTro và matKhau được bảo vệ — không thể cập nhật qua endpoint này.
     *
     * @param id       Path variable — ID nhân viên cần cập nhật.
     * @param nhanVien Body JSON chứa thông tin mới.
     * @return 200 OK + entity đã cập nhật | 400 nếu vi phạm | 404 nếu không tìm thấy | 500 lỗi hệ thống.
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> capNhat(@PathVariable Integer id,
                                     @RequestBody NguoiDung nhanVien) {
        try {
            log.info("[NhanVienAPI] PUT /api/admin/nhan-vien/{} — email={}", id, nhanVien.getEmail());
            NguoiDung updated = nhanVienService.capNhat(id, nhanVien);
            return ResponseEntity.ok(updated);
        } catch (EntityNotFoundException e) {
            log.warn("[NhanVienAPI] Cập nhật thất bại — không tìm thấy id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("[NhanVienAPI] Cập nhật thất bại — vi phạm nghiệp vụ id={}: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[NhanVienAPI] Lỗi hệ thống khi cập nhật nhân viên id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }

    // =========================================================================
    // PATCH /api/admin/nhan-vien/{id}/trang-thai — Khóa / Mở khóa tài khoản
    // =========================================================================

    /**
     * Đổi trạng thái tài khoản nhân viên (Toggle Khóa/Mở khóa).
     * <p>
     * Logic toggle:
     * <ul>
     *   <li>{@code hoat_dong} → {@code khoa}</li>
     *   <li>{@code khoa} → {@code hoat_dong}</li>
     * </ul>
     *
     * @param id Path variable — ID nhân viên cần thay đổi trạng thái.
     * @return 200 OK + entity sau khi đổi trạng thái | 404 nếu không tìm thấy | 500 lỗi hệ thống.
     */
    @PatchMapping("/{id}/trang-thai")
    public ResponseEntity<?> doiTrangThai(@PathVariable Integer id) {
        try {
            log.info("[NhanVienAPI] PATCH /api/admin/nhan-vien/{}/trang-thai", id);
            NguoiDung updated = nhanVienService.doiTrangThai(id);
            String message = "khoa".equals(updated.getTrangThai())
                    ? "Đã khóa tài khoản nhân viên thành công."
                    : "Đã mở khóa tài khoản nhân viên thành công.";
            return ResponseEntity.ok(Map.of(
                    "message",    message,
                    "trangThai",  updated.getTrangThai(),
                    "nhanVien",   updated
            ));
        } catch (EntityNotFoundException e) {
            log.warn("[NhanVienAPI] Đổi trạng thái thất bại — không tìm thấy id={}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("[NhanVienAPI] Lỗi hệ thống khi đổi trạng thái nhân viên id={}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi hệ thống: " + e.getMessage()));
        }
    }
}
