package org.example.primemobile.service;

import org.example.primemobile.entity.SanPham;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Contract cho phân hệ Quản lý Sản phẩm (Model điện thoại).
 * <p>
 * Mỗi SanPham là một dòng máy (ví dụ: "iPhone 15 Pro Max").
 * Các SKU cụ thể (màu sắc, RAM, ROM) được quản lý ở {@link IBienTheSanPhamService}.
 * <p>
 * Trạng thái hợp lệ (CHECK constraint DB):
 * <ul>
 *   <li>{@code "dang_ban"}  — Đang kinh doanh, hiển thị trên web.</li>
 *   <li>{@code "ngung_ban"} — Ngừng bán, ẩn khỏi web nhưng vẫn giữ dữ liệu.</li>
 *   <li>{@code "sap_ra_mat"} — Sắp ra mắt, hiển thị trang đặt trước.</li>
 * </ul>
 * Không xóa vật lý để bảo toàn lịch sử đơn hàng và tồn kho liên quan.
 */
public interface ISanPhamService {

    /**
     * Lấy danh sách sản phẩm có phân trang và lọc theo danh mục / hãng.
     * <p>
     * Tham số lọc nếu {@code null} = bỏ qua điều kiện đó (trả về tất cả).
     * <b>Dùng cho Admin Dashboard</b> — bao gồm cả sản phẩm ngừng bán.
     *
     * @param danhMucId      ID danh mục để lọc. Null = tất cả danh mục.
     * @param hangSanXuatId  ID hãng sản xuất để lọc. Null = tất cả hãng.
     * @param pageable       Thông tin phân trang và sắp xếp.
     * @return Trang {@link SanPham} phù hợp với điều kiện lọc.
     */
    Page<SanPham> layDanhSach(String tuKhoa, Integer danhMucId, Integer hangSanXuatId, Pageable pageable);

    /**
     * Lấy danh sách sản phẩm <b>công khai</b> dành cho Frontend (Khách vãng lai / KhachHang).
     * <p>
     * Chỉ trả về sản phẩm có {@code trangThai = 'dang_ban'}.
     * Sản phẩm {@code ngung_ban} và {@code sap_ra_mat} được ẩn khỏi kết quả.
     *
     * @param tuKhoa         Từ khóa tìm kiếm (theo tên hoặc mã). Null = bỏ qua.
     * @param danhMucId      ID danh mục để lọc. Null = tất cả danh mục.
     * @param hangSanXuatId  ID hãng sản xuất để lọc. Null = tất cả hãng.
     * @param pageable       Thông tin phân trang và sắp xếp.
     * @return Trang {@link SanPham} chỉ chứa sản phẩm đang bán.
     */
    Page<SanPham> layDanhSachCongKhai(String tuKhoa, Integer danhMucId, Integer hangSanXuatId, Pageable pageable);

    /**
     * Lấy thông tin chi tiết một sản phẩm theo ID.
     *
     * @param id ID sản phẩm.
     * @return {@link SanPham} tương ứng.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     */
    SanPham layTheoId(Integer id);

    /**
     * Thêm sản phẩm mới vào hệ thống.
     * <p>
     * Yêu cầu: {@code danhMucId} và {@code hangSanXuatId} trong body phải hợp lệ.
     * Body cần truyền nested object: {@code {"danhMuc": {"id": 1}, "hangSanXuat": {"id": 2}}}.
     *
     * @param sanPham Dữ liệu sản phẩm mới (chưa có ID).
     * @return {@link SanPham} đã được lưu với ID được gán.
     * @throws IllegalArgumentException                    nếu {@code maSanPham} đã tồn tại.
     * @throws jakarta.persistence.EntityNotFoundException nếu danh mục hoặc hãng không tồn tại.
     */
    SanPham them(SanPham sanPham);

    /**
     * Cập nhật thông tin sản phẩm (tên, mô tả, năm ra mắt, bảo hành...).
     * <p>
     * Không cho phép đổi {@code maSanPham} nếu đã trùng với sản phẩm khác.
     *
     * @param id      ID sản phẩm cần cập nhật.
     * @param sanPham Dữ liệu mới.
     * @return {@link SanPham} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException                    nếu {@code maSanPham} mới trùng với sản phẩm khác.
     */
    SanPham capNhat(Integer id, SanPham sanPham);

    /**
     * Đổi trạng thái sản phẩm.
     * <p>
     * Trạng thái hợp lệ: {@code "dang_ban"} | {@code "ngung_ban"} | {@code "sap_ra_mat"}.
     *
     * @param id         ID sản phẩm.
     * @param trangThai  Trạng thái mới.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException                    nếu giá trị trạng thái không hợp lệ.
     */
    SanPham doiTrangThai(Integer id, String trangThai);
}
