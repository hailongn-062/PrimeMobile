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
 * <li>Mã IMEI là hệ quả phụ thuộc vào số lượng nhập kho trong bảng
 * {@code ton_kho}.</li>
 * <li><b>Chặn thêm thừa:</b> Số bản ghi IMEI có trạng thái {@code trong_kho}
 * của một SKU KHÔNG ĐƯỢC vượt quá số lượng ghi nhận trong bảng {@code ton_kho}.</li>
 * <li><b>Ràng buộc duy nhất:</b> {@code imei1}, {@code imei2}
 * phải duy nhất toàn hệ thống trước khi lưu.</li>
 * <li>Trạng thái mặc định khi nhập mới luôn là {@code "trong_kho"}.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MayDienThoaiServiceImpl implements IMayDienThoaiService {

    private final MayDienThoaiRepository mayDienThoaiRepository;
    private final TonKhoRepository tonKhoRepository;
    private final BienTheSanPhamRepository bienTheSanPhamRepository;

    // =======================================================================
    // PUBLIC METHODS — NGHIỆP VỤ CHÍNH
    // =======================================================================

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
                                "Biến thể chưa được nhập vào kho này.",
                        khoId, bienTheSanPhamId)));

        // Đếm số bản ghi IMEI đang 'trong_kho' của SKU này (toàn hệ thống)
        long soLuongDaDinhDanh = mayDienThoaiRepository
                .countTrongKhoByBienThe(bienTheSanPhamId);

        int chenh_lech = soLuongTonKho - (int) soLuongDaDinhDanh;

        log.debug("[MayDienThoaiService] tinhSoLuongImeiCanThem — tonKho={}, daDinhDanh={}, canThem={}",
                soLuongTonKho, soLuongDaDinhDanh, chenh_lech);

        return Math.max(chenh_lech, 0);
    }

    @Override
    @Transactional
    public void nhapDanhSachImei(Integer khoId,
                                 Integer bienTheSanPhamId,
                                 List<ThemImeiRequest> danhSachImei) {

        log.info("[MayDienThoaiService] nhapDanhSachImei — khoId={}, bienTheId={}, soLuong={}",
                khoId, bienTheSanPhamId, danhSachImei == null ? 0 : danhSachImei.size());

        // Bước 1: Validate danh sách đầu vào không rỗng
        if (danhSachImei == null || danhSachImei.isEmpty()) {
            throw new IllegalArgumentException("Danh sách IMEI không được rỗng.");
        }

        // Bước 2: Ràng buộc đối khớp — Chặn thêm thừa (system_rules.md §3.3)
        int soLuongCanThem = tinhSoLuongImeiCanThem(khoId, bienTheSanPhamId);

        if (danhSachImei.size() > soLuongCanThem) {
            throw new IllegalArgumentException(String.format(
                    "Số lượng IMEI nhập vào vượt quá số lượng tồn kho chưa được định danh. " +
                            "Số lượng cần thêm: %d, Số lượng bạn nhập: %d.",
                    soLuongCanThem, danhSachImei.size()));
        }

        // Bước 3: Ràng buộc duy nhất — Fail-Fast kiểm tra trùng toàn bộ trước khi lưu
        for (int i = 0; i < danhSachImei.size(); i++) {
            ThemImeiRequest req = danhSachImei.get(i);
            int stt = i + 1;

            if (req.getImei1() == null || req.getImei1().isBlank()) {
                throw new IllegalArgumentException(
                        String.format("IMEI 1 tại dòng %d không được để trống.", stt));
            }

            if (mayDienThoaiRepository.existsByImei1(req.getImei1())) {
                throw new IllegalArgumentException(
                        String.format("IMEI 1 '%s' (dòng %d) đã tồn tại trong hệ thống.",
                                req.getImei1(), stt));
            }

            if (req.getImei2() != null && !req.getImei2().isBlank()) {
                if (mayDienThoaiRepository.existsByImei2(req.getImei2())) {
                    throw new IllegalArgumentException(
                            String.format("IMEI 2 '%s' (dòng %d) đã tồn tại trong hệ thống.",
                                    req.getImei2(), stt));
                }
            }
        }

        // Bước 4: Load entity BienTheSanPham
        BienTheSanPham bienTheSanPham = bienTheSanPhamRepository
                .findById(bienTheSanPhamId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy biến thể sản phẩm có ID: " + bienTheSanPhamId));

        // Bước 5: Build và lưu tất cả bản ghi MayDienThoai
        LocalDateTime ngayNhapKho = LocalDateTime.now();
        List<MayDienThoai> danhSachMay = new ArrayList<>(danhSachImei.size());

        for (ThemImeiRequest req : danhSachImei) {
            MayDienThoai may = MayDienThoai.builder()
                    .bienTheSanPham(bienTheSanPham)
                    .imei1(req.getImei1().trim())
                    .imei2(req.getImei2() != null && !req.getImei2().isBlank()
                            ? req.getImei2().trim()
                            : null)
                    .tinhTrang("trong_kho")
                    .ngayNhapKho(ngayNhapKho)
                    .build();

            danhSachMay.add(may);
        }

        mayDienThoaiRepository.saveAll(danhSachMay);

        log.info("[MayDienThoaiService] nhapDanhSachImei thành công — đã lưu {} bản ghi IMEI " +
                        "cho bienTheId={} tại khoId={}",
                danhSachMay.size(), bienTheSanPhamId, khoId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MayDienThoai> layDanhSachTheoBienTheVaTrangThai(Integer bienTheSanPhamId, String tinhTrang) {
        log.debug("[MayDienThoaiService] layDanhSachTheoBienTheVaTrangThai — bienTheId={}, tinhTrang={}",
                bienTheSanPhamId, tinhTrang);

        if (bienTheSanPhamId != null && !bienTheSanPhamRepository.existsById(bienTheSanPhamId)) {
            throw new EntityNotFoundException(
                    "Không tìm thấy biến thể sản phẩm có ID: " + bienTheSanPhamId);
        }

        List<MayDienThoai> danhSach;
        if (bienTheSanPhamId != null) {
            if (tinhTrang == null || tinhTrang.isBlank()) {
                danhSach = mayDienThoaiRepository.findByBienTheSanPhamId(bienTheSanPhamId);
            } else {
                danhSach = mayDienThoaiRepository.findByBienTheSanPhamIdAndTinhTrang(bienTheSanPhamId, tinhTrang);
            }
        } else {
            if (tinhTrang == null || tinhTrang.isBlank()) {
                danhSach = mayDienThoaiRepository.findAll();
            } else {
                danhSach = mayDienThoaiRepository.findByTinhTrang(tinhTrang);
            }
        }

        log.debug("[MayDienThoaiService] layDanhSachTheoBienTheVaTrangThai — tìm thấy {} bản ghi",
                danhSach.size());
        return danhSach;
    }
}