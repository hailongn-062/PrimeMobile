package org.example.primemobile.service;

import org.example.primemobile.entity.MauSac;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IMauSacService {
    
    // Lấy tất cả màu sắc (thường dùng cho dropdown)
    List<MauSac> layTatCa();
    
    // Lấy danh sách có phân trang và tìm kiếm (cho trang quản lý)
    Page<MauSac> layDanhSachPhanTrang(String tuKhoa, Pageable pageable);
    
    // Lấy màu sắc theo ID
    MauSac layTheoId(Integer id);
    
    // Thêm mới màu sắc
    MauSac them(MauSac mauSac);
    
    // Cập nhật màu sắc
    MauSac capNhat(Integer id, MauSac mauSac);
}
