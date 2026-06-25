package org.example.primemobile.controller;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.auth.SessionUser;
import org.example.primemobile.entity.NguoiDung;
import org.example.primemobile.service.INhanVienService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller UI (Thymeleaf) cho phân hệ Quản lý Nhân Viên — Admin only.
 * Base path: {@code /admin/nhan-vien}
 *
 * <p>Tách biệt hoàn toàn với {@link NhanVienController} (REST API).
 * Controller này chỉ render HTML — mọi thao tác nghiệp vụ (thêm, sửa,
 * khóa tài khoản) được thực hiện qua JavaScript fetch() gọi đến REST API.
 *
 * <p>Ghi chú (system_rules.md §1):
 * <ul>
 *   <li>Không dùng Spring Security. Phân quyền Admin kiểm soát bởi AuthInterceptor.</li>
 *   <li>Session key: {@code "CURRENT_ADMIN"} — chứa {@link SessionUser}.</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/admin/nhan-vien")
@RequiredArgsConstructor
public class NhanVienUIController {

    private static final String SESSION_KEY = "CURRENT_ADMIN";

    private final INhanVienService nhanVienService;

    // =========================================================================
    // GET /admin/nhan-vien — Trang danh sách nhân viên
    // =========================================================================

    /**
     * Render trang danh sách nhân viên.
     * <p>
     * Tải sẵn danh sách nhân viên từ Service để render server-side bằng Thymeleaf.
     * Giao diện cũng hỗ trợ fetch() để làm mới dữ liệu sau khi thêm/sửa/khóa.
     *
     * @param model   Model truyền dữ liệu sang Thymeleaf template.
     * @param session HTTP session để lấy thông tin người dùng đang đăng nhập.
     * @return View path: {@code admin/nhan-vien/danh-sach}.
     */
    @GetMapping
    public String danhSach(Model model, HttpSession session) {
        SessionUser currentUser = (SessionUser) session.getAttribute(SESSION_KEY);
        log.info("[NhanVienUI] Danh sách nhân viên — user={}",
                currentUser != null ? currentUser.getEmail() : "?");

        List<NguoiDung> danhSach = nhanVienService.layDanhSachNhanVien();

        model.addAttribute("currentUser",      currentUser);
        model.addAttribute("pageTitle",        "Quản lý Nhân viên");
        model.addAttribute("activePage",       "nhan-vien");
        model.addAttribute("danhSachNhanVien", danhSach);
        model.addAttribute("tongSoNhanVien",   danhSach.size());

        return "admin/nhan-vien/danh-sach";
    }
}
