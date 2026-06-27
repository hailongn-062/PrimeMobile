package org.example.primemobile.service;

import org.example.primemobile.entity.BienTheSanPham;

import java.util.List;

/**
 * Contract đầy đủ cho phân hệ Quản lý Biến thể Sản phẩm (SKU).
 * <p>
 * Mỗi biến thể = 1 SKU cụ thể (màu sắc + RAM + ROM).
 * Ví dụ: iPhone 15 Pro Max – Titan Đen – 8GB – 256GB.
 * <p>
 * <b>Luật nghiệp vụ quan trọng — Khởi tạo Tồn kho (system_rules.md §3):</b><br>
 * Ngay khi một biến thể mới được lưu thành công, hệ thống PHẢI tự động
 * tạo bản ghi {@code ton_kho} với {@code so_luong = 0} cho biến thể này
 * tại TẤT CẢ các kho đang hoạt động ({@code kichHoat = true}).
 * Điều này đảm bảo mọi SKU luôn có thể tra cứu tồn kho ngay sau khi được tạo.
 * <p>
 * Trạng thái hợp lệ (CHECK constraint DB):
 * <ul>
 *   <li>{@code "con_hang"}          — Còn hàng để bán.</li>
 *   <li>{@code "het_hang"}          — Hết hàng, đang chờ nhập thêm.</li>
 *   <li>{@code "ngung_kinh_doanh"}  — Ngừng kinh doanh SKU này.</li>
 * </ul>
 */
public interface IBienTheSanPhamService {

    /**
     * Lấy thông tin một biến thể sản phẩm theo ID.
     * <p>
     * Dùng cho luồng bán hàng offline và online để lấy giá bán / thông tin SKU.
     *
     * @param id ID biến thể. Không được null.
     * @return {@link BienTheSanPham} nếu tìm thấy.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy.
     */
    BienTheSanPham getBienTheSanPham(Integer id);

    /**
     * Lấy danh sách tất cả biến thể của một sản phẩm theo ID sản phẩm.
     *
     * @param sanPhamId ID sản phẩm cha.
     * @return Danh sách {@link BienTheSanPham} thuộc sản phẩm đó, có thể rỗng.
     * @throws jakarta.persistence.EntityNotFoundException nếu sanPhamId không tồn tại.
     */
    List<BienTheSanPham> layTheoSanPhamId(Integer sanPhamId);

    /**
     * Thêm biến thể mới cho một sản phẩm.
     * <p>
     * <b>Luật bắt buộc:</b> Sau khi lưu biến thể thành công, tự động khởi tạo
     * bản ghi {@code ton_kho} với {@code so_luong = 0} tại tất cả kho đang hoạt động.
     * Toàn bộ thao tác được bao trong một {@code @Transactional} để rollback
     * nếu bất kỳ bước nào thất bại.
     *
     * @param sanPhamId    ID sản phẩm cha.
     * @param bienTheSanPham Dữ liệu biến thể mới (chưa có ID).
     * @return {@link BienTheSanPham} đã được lưu.
     * @throws jakarta.persistence.EntityNotFoundException nếu sanPhamId không tồn tại.
     * @throws IllegalArgumentException nếu {@code maSku} đã tồn tại.
     */
    BienTheSanPham them(Integer sanPhamId, BienTheSanPham bienTheSanPham);

    /**
     * Cập nhật thông tin biến thể (giá bán, giá khuyến mãi, trọng lượng, pin...).
     *
     * @param id             ID biến thể cần cập nhật.
     * @param bienTheSanPham Dữ liệu mới.
     * @return {@link BienTheSanPham} sau khi cập nhật.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException nếu mã SKU mới trùng với biến thể khác.
     */
    BienTheSanPham capNhat(Integer id, BienTheSanPham bienTheSanPham);

    /**
     * Đổi trạng thái biến thể.
     * <p>
     * Trạng thái hợp lệ: {@code "con_hang"} | {@code "het_hang"} | {@code "ngung_kinh_doanh"}.
     *
     * @param id        ID biến thể.
     * @param trangThai Trạng thái mới.
     * @return {@link BienTheSanPham} sau khi cập nhật trạng thái.
     * @throws jakarta.persistence.EntityNotFoundException nếu ID không tồn tại.
     * @throws IllegalArgumentException nếu giá trị trạng thái không hợp lệ.
     */
    BienTheSanPham doiTrangThai(Integer id, String trangThai);
}
