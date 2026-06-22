package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.kho.ThemImeiRequest;
import org.example.primemobile.entity.BienTheSanPham;
import org.example.primemobile.entity.MayDienThoai;
import org.example.primemobile.repository.BienTheSanPhamRepository;
import org.example.primemobile.repository.MayDienThoaiRepository;
import org.example.primemobile.repository.TonKhoRepository;
import org.example.primemobile.service.IMayDienThoaiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Triển khai nghiệp vụ Quản lý IMEI máy điện thoại vật lý cho PrimeMobile.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md §3.3):</h2>
 * <ul>
 *   <li>Mã IMEI là hệ quả phụ thuộc vào số lượng nhập kho trong bảng {@code ton_kho}.</li>
 *   <li><b>Chặn thêm thừa:</b> Số bản ghi IMEI có trạng thái {@code trong_kho} của một SKU
 *       KHÔNG ĐƯỢC vượt quá số lượng ghi nhận trong bảng {@code ton_kho}.</li>
 *   <li><b>Ràng buộc duy nhất:</b> {@code imei1}, {@code imei2}, {@code serial} phải duy nhất
 *       toàn hệ thống trước khi lưu.</li>
 *   <li>Trạng thái mặc định khi nhập mới luôn là {@code "trong_kho"}.</li>
 * </ul>
 *
 * <h2>Lưu ý kiến trúc dữ liệu:</h2>
 * Bảng {@code may_dien_thoai} trong schema SQL không có cột {@code kho_id}.
 * IMEI chỉ gắn với {@code bien_the_san_pham_id}, không gắn trực tiếp với kho.
 * Tham số {@code khoId} trong các hàm được dùng để tra cứu số lượng tồn kho
 * ({@code ton_kho.so_luong}) làm mốc so sánh, không để gắn vào bản ghi IMEI.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MayDienThoaiServiceImpl implements IMayDienThoaiService {

    // -----------------------------------------------------------------------
    // DEPENDENCIES (inject qua constructor nhờ @RequiredArgsConstructor)
    // -----------------------------------------------------------------------

    private final MayDienThoaiRepository   mayDienThoaiRepository;
    private final TonKhoRepository         tonKhoRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    // =======================================================================
    // PUBLIC METHODS — NGHIỆP VỤ CHÍNH
    // =======================================================================

    /**
     * {@inheritDoc}
     *
     * <h3>Cách tính:</h3>
     * <pre>
     *   soLuongTonKho    = ton_kho.so_luong  (của bienTheSanPhamId tại khoId)
     *   soLuongDaDinhDanh = COUNT(may_dien_thoai có tinhTrang = 'trong_kho')
     *                        của bienTheSanPhamId trong toàn hệ thống
     *   kết quả          = soLuongTonKho − soLuongDaDinhDanh
     * </pre>
     *
     * Nếu kết quả âm (ví dụ: có nhiều IMEI hơn số tồn kho — trường hợp bất thường),
     * trả về {@code 0} để không gây ảnh hưởng tiêu cực đến luồng nghiệp vụ.
     */
    @Override
    @Transactional(readOnly = true)
    public int tinhSoLuongImeiCanThem(Integer khoId, Integer bienTheSanPhamId) {

        log.debug("[MayDienThoaiService] tinhSoLuongImeiCanThem — khoId={}, bienTheId={}",
                khoId, bienTheSanPhamId);

        // Lấy số lượng tồn kho chính thức từ bảng ton_kho
        int soLuongTonKho = tonKhoRepository
                .findByKhoIdAndBienTheId(khoId, bienTheSanPhamId)
                .map(tk -> tk.getSoLuong())
                .orElseThrow(() -> new EntityNotFoundException(String.format(
                        "Không tìm thấy bản ghi tồn kho — khoId=%d, bienTheId=%d. " +
                        "Biến thể chưa được nhập vào kho này.", khoId, bienTheSanPhamId)));

        // Đếm số bản ghi IMEI đang 'trong_kho' của SKU này trong toàn hệ thống
        long soLuongDaDinhDanh = mayDienThoaiRepository
                .countTrongKhoByBienThe(bienTheSanPhamId);

        int chenh_lech = soLuongTonKho - (int) soLuongDaDinhDanh;

        log.debug("[MayDienThoaiService] tinhSoLuongImeiCanThem — tonKho={}, daDinhDanh={}, canThem={}",
                soLuongTonKho, soLuongDaDinhDanh, chenh_lech);

        // Trả về 0 nếu chênh lệch âm (bất thường, không nên ném lỗi tại đây)
        return Math.max(chenh_lech, 0);
    }

    /**
     * {@inheritDoc}
     *
     * <h3>Luồng thực thi:</h3>
     * <ol>
     *   <li>Validate danh sách đầu vào không rỗng.</li>
     *   <li>Gọi {@link #tinhSoLuongImeiCanThem} — nếu {@code danhSachImei.size() > soCanThem},
     *       ném {@link IllegalArgumentException} ngay (Fail-Fast).</li>
     *   <li>Kiểm tra từng bộ (imei1, imei2, serial) xem có trùng lặp không.</li>
     *   <li>Load entity {@link BienTheSanPham}, build và lưu tất cả bản ghi {@link MayDienThoai}.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void nhapDanhSachImei(Integer khoId,
                                 Integer bienTheSanPhamId,
                                 List<ThemImeiRequest> danhSachImei) {

        log.info("[MayDienThoaiService] nhapDanhSachImei — khoId={}, bienTheId={}, soLuong={}",
                khoId, bienTheSanPhamId, danhSachImei == null ? 0 : danhSachImei.size());

        // ------------------------------------------------------------------
        // Bước 1: Validate danh sách đầu vào không rỗng
        // ------------------------------------------------------------------
        if (danhSachImei == null || danhSachImei.isEmpty()) {
            throw new IllegalArgumentException("Danh sách IMEI không được rỗng.");
        }

        // ------------------------------------------------------------------
        // Bước 2: Ràng buộc đối khớp — Chặn thêm thừa (system_rules.md §3.3)
        // ------------------------------------------------------------------
        int soLuongCanThem = tinhSoLuongImeiCanThem(khoId, bienTheSanPhamId);

        if (danhSachImei.size() > soLuongCanThem) {
            throw new IllegalArgumentException(String.format(
                    "Số lượng IMEI nhập vào vượt quá số lượng tồn kho chưa được định danh. " +
                    "Số lượng cần thêm: %d, Số lượng bạn nhập: %d.",
                    soLuongCanThem, danhSachImei.size()));
        }

        // ------------------------------------------------------------------
        // Bước 3: Ràng buộc duy nhất — Kiểm tra trùng lặp toàn bộ danh sách
        // Chiến lược Fail-Fast: kiểm tra hết trước, không lưu gì cho đến khi
        // tất cả đều pass để đảm bảo tính nhất quán dữ liệu.
        // ------------------------------------------------------------------
        for (int i = 0; i < danhSachImei.size(); i++) {
            ThemImeiRequest req = danhSachImei.get(i);
            int stt = i + 1; // 1-indexed cho message lỗi thân thiện

            // Validate imei1 bắt buộc không rỗng
            if (req.getImei1() == null || req.getImei1().isBlank()) {
                throw new IllegalArgumentException(
                        String.format("IMEI 1 tại dòng %d không được để trống.", stt));
            }

            // Kiểm tra trùng imei1
            if (mayDienThoaiRepository.existsByImei1(req.getImei1())) {
                throw new IllegalArgumentException(
                        String.format("IMEI 1 '%s' (dòng %d) đã tồn tại trong hệ thống.",
                                req.getImei1(), stt));
            }

            // Kiểm tra trùng imei2 (chỉ khi có giá trị)
            if (req.getImei2() != null && !req.getImei2().isBlank()) {
                if (mayDienThoaiRepository.existsByImei2(req.getImei2())) {
                    throw new IllegalArgumentException(
                            String.format("IMEI 2 '%s' (dòng %d) đã tồn tại trong hệ thống.",
                                    req.getImei2(), stt));
                }
            }

            // Kiểm tra trùng serial (chỉ khi có giá trị)
            if (req.getSerial() != null && !req.getSerial().isBlank()) {
                if (mayDienThoaiRepository.existsBySerial(req.getSerial())) {
                    throw new IllegalArgumentException(
                            String.format("Serial '%s' (dòng %d) đã tồn tại trong hệ thống.",
                                    req.getSerial(), stt));
                }
            }
        }

        // ------------------------------------------------------------------
        // Bước 4: Load entity BienTheSanPham (1 lần duy nhất, tránh N+1 query)
        // ------------------------------------------------------------------
        BienTheSanPham bienTheSanPham = bienTheSanPhamRepository
                .findById(bienTheSanPhamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy biến thể sản phẩm có ID: " + bienTheSanPhamId));

        // ------------------------------------------------------------------
        // Bước 5: Build và lưu tất cả bản ghi MayDienThoai
        // ------------------------------------------------------------------
        LocalDateTime ngayNhapKho = LocalDateTime.now();
        List<MayDienThoai> danhSachMay = new ArrayList<>(danhSachImei.size());

        for (ThemImeiRequest req : danhSachImei) {
            MayDienThoai may = MayDienThoai.builder()
                    .bienTheSanPham(bienTheSanPham)
                    .imei1(req.getImei1().trim())
                    // Lưu null thay vì chuỗi rỗng để đảm bảo UNIQUE constraint hoạt động đúng
                    .imei2(req.getImei2() != null && !req.getImei2().isBlank()
                            ? req.getImei2().trim() : null)
                    .serial(req.getSerial() != null && !req.getSerial().isBlank()
                            ? req.getSerial().trim() : null)
                    .tinhTrang("trong_kho")   // Gán cứng theo system_rules.md §3.3
                    .ngayNhapKho(ngayNhapKho)
                    .build();

            danhSachMay.add(may);
        }

        mayDienThoaiRepository.saveAll(danhSachMay);

        log.info("[MayDienThoaiService] nhapDanhSachImei thành công — đã lưu {} bản ghi IMEI " +
                        "cho bienTheId={} tại khoId={}",
                danhSachMay.size(), bienTheSanPhamId, khoId);
    }
}
