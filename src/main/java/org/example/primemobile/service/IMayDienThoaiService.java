package org.example.primemobile.service;

import org.example.primemobile.dto.kho.ThemImeiRequest;

import java.util.List;

/**
 * Contract (interface) cho module Quản lý IMEI máy điện thoại vật lý.
 * <p>
 * Toàn bộ nghiệp vụ bám sát system_rules.md §3.3:
 * <ul>
 *   <li>Mã IMEI là hệ quả phụ thuộc số lượng tồn kho – không được vượt quá.</li>
 *   <li>Mỗi bộ (imei1, imei2, serial) phải duy nhất toàn hệ thống.</li>
 *   <li>Trạng thái mặc định khi nhập mới là {@code "trong_kho"}.</li>
 * </ul>
 */
public interface IMayDienThoaiService {

    /**
     * Tính số lượng IMEI còn thiếu (chưa được định danh) của một SKU tại một kho.
     * <p>
     * Công thức: {@code soLuong(ton_kho)} − {@code COUNT(may_dien_thoai có tinhTrang = 'trong_kho') của SKU đó}.
     * <p>
     * Nếu kết quả = 0 → SKU đã đủ IMEI, không cần nhập thêm.
     * Nếu kết quả > 0 → còn thiếu N bộ IMEI.
     *
     * @param khoId              ID kho cần kiểm tra tồn kho.
     * @param bienTheSanPhamId   ID biến thể SKU cần kiểm tra.
     * @return Số lượng IMEI còn thiếu (≥ 0). Trả về 0 nếu đã đủ hoặc dư.
     * @throws jakarta.persistence.EntityNotFoundException nếu không tìm thấy bản ghi tồn kho.
     */
    int tinhSoLuongImeiCanThem(Integer khoId, Integer bienTheSanPhamId);

    /**
     * Nhập danh sách IMEI máy vật lý vào hệ thống cho một SKU tại một kho.
     * <p>
     * Nghiệp vụ thực thi (system_rules.md §3.3):
     * <ol>
     *   <li><b>Ràng buộc đối khớp (Chặn thêm thừa):</b> Nếu {@code danhSachImei.size()} lớn hơn
     *       số lượng IMEI còn thiếu, ném {@link IllegalArgumentException}.</li>
     *   <li><b>Ràng buộc duy nhất:</b> Kiểm tra từng imei1, imei2, serial xem đã tồn tại trong
     *       database chưa. Nếu trùng, ném {@link IllegalArgumentException}.</li>
     *   <li><b>Lưu:</b> Tạo các bản ghi {@code MayDienThoai} mới với trạng thái {@code "trong_kho"}
     *       và lưu tất cả vào database trong 1 transaction.</li>
     * </ol>
     *
     * @param khoId              ID kho nhập hàng (dùng để tính chênh lệch tồn kho).
     * @param bienTheSanPhamId   ID biến thể SKU cần gắn IMEI.
     * @param danhSachImei       Danh sách bộ (imei1, imei2, serial) cần nhập.
     * @throws IllegalArgumentException    Nếu số lượng IMEI vượt quá mức cần thiết, hoặc bị trùng lặp.
     * @throws jakarta.persistence.EntityNotFoundException Nếu biến thể SKU không tồn tại.
     */
    void nhapDanhSachImei(Integer khoId, Integer bienTheSanPhamId, List<ThemImeiRequest> danhSachImei);
}
