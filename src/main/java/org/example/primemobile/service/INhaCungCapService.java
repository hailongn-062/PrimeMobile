package org.example.primemobile.service;

import org.example.primemobile.entity.NhaCungCap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/** Nghiệp vụ Nhà Cung Cấp (Admin). */
public interface INhaCungCapService {

    /** Danh sách NCC có phân trang + tìm kiếm. */
    Page<NhaCungCap> layDanhSach(String trangThai, String tuKhoa, Pageable pageable);

    /** Danh sách NCC đang_hop_tac — dùng đổ ComboBox khi tạo Phiếu Nhập. */
    List<NhaCungCap> layDangHopTac();

    NhaCungCap taoMoi(NhaCungCap request);

    NhaCungCap capNhat(Integer id, NhaCungCap request);

    /** Đổi trạng thái hợp tác của NCC (dang_hop_tac ↔ ngung_hop_tac). */
    NhaCungCap doiTrangThai(Integer id, String trangThaiMoi);

    /** Lấy chi tiết NCC theo ID */
    NhaCungCap layTheoId(Integer id);
}
