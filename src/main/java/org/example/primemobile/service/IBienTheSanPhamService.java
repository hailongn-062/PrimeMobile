package org.example.primemobile.service;

import org.example.primemobile.entity.BienTheSanPham;

/**
 * Contract tối giản cho module truy xuất thông tin Biến thể Sản phẩm.
 * <p>
 * Mục đích duy nhất trong giai đoạn này: Cung cấp dữ liệu giá bán và thông tin
 * SKU để phục vụ luồng bán hàng offline và online.
 * <p>
 * Tách thành interface riêng theo nguyên tắc Interface Segregation Principle (ISP)
 * — Service bán hàng chỉ cần inject interface này, không bị ràng buộc vào toàn bộ
 * business logic của module sản phẩm khi nó được mở rộng sau này.
 */
public interface IBienTheSanPhamService {

    /**
     * Lấy thông tin một biến thể sản phẩm theo ID.
     * <p>
     * Hàm này là cổng truy xuất dữ liệu giá bán ({@code giaBan}, {@code giaKhuyenMai})
     * và thông tin SKU để tạo dòng {@code ChiTietDonHang}.
     *
     * @param id ID của biến thể cần lấy. Không được null.
     * @return Đối tượng {@link BienTheSanPham} nếu tìm thấy.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy biến thể với ID đã cho.
     */
    BienTheSanPham getBienTheSanPham(Integer id);
}
