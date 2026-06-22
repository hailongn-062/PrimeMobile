package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.service.IBienTheSanPhamService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Triển khai tối giản cho module Biến thể Sản phẩm.
 * <p>
 * Giai đoạn hiện tại chỉ cần 1 hàm {@code getBienTheSanPham} để phục vụ
 * luồng bán hàng offline. Module này sẽ được mở rộng khi xây dựng
 * giao diện khách hàng (Customer Site) sau này.
 */
@Service
@RequiredArgsConstructor
public class BienTheSanPhamServiceImpl implements IBienTheSanPhamService {

    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    /**
     * {@inheritDoc}
     * <p>
     * Sử dụng {@code @Transactional(readOnly = true)} để tối ưu hiệu năng
     * — Spring sẽ bỏ qua dirty-checking và dùng read-only JDBC connection hint.
     */
    @Override
    @Transactional(readOnly = true)
    public BienTheSanPham getBienTheSanPham(Integer id) {
        return bienTheSanPhamRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy biến thể sản phẩm có ID: " + id));
    }
}
