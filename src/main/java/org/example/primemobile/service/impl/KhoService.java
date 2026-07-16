package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest;
import org.example.primemobile.dto.kho.TaoPhieuNhapKhoRequest.ChiTietNhapRequest;
import org.example.primemobile.entity.*;
import org.example.primemobile.repository.*;
import org.example.primemobile.service.IKhoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service xử lý toàn bộ nghiệp vụ Kho hàng cho PrimeMobile.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md §3.1, §3.2):</h2>
 * <ul>
 *   <li>Hệ thống có <b>1 kho duy nhất</b> với ID = 1.</li>
 *   <li>Nhập hàng: NCC → kho ID=1.</li>
 *   <li><b>Safety Stock Rule</b>: Sau bất kỳ thao tác trừ kho nào, tồn kho còn lại
 *       TUYỆT ĐỐI KHÔNG được dưới {@value #TON_KHO_TOI_THIEU} đơn vị / SKU / kho.</li>
 *   <li>Phiếu nhập được <b>chốt ngay</b> (hoan_thanh) khi tạo, không qua bước chờ duyệt.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KhoService implements IKhoService {

    // -----------------------------------------------------------------------
    // HẰNG SỐ NGHIỆP VỤ
    // -----------------------------------------------------------------------

    /** Mức tồn kho tối thiểu bắt buộc theo system_rules.md §3.1. */
    private static final int TON_KHO_TOI_THIEU = 5;

    /** ID kho duy nhất trong hệ thống. */
    private static final Integer KHO_DUY_NHAT_ID = 1;

    /** Pattern sinh mã phiếu: PNK-YYYYMM-&lt;millis 6 chữ số cuối&gt;. */
    private static final DateTimeFormatter MA_PHIEU_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    // -----------------------------------------------------------------------
    // DEPENDENCIES (inject qua constructor nhờ @RequiredArgsConstructor)
    // -----------------------------------------------------------------------

    private final KhoRepository               khoRepository;
    private final TonKhoRepository            tonKhoRepository;
    private final PhieuNhapKhoRepository      phieuNhapKhoRepository;
    private final ChiTietPhieuNhapRepository  chiTietPhieuNhapRepository;
    private final BienTheSanPhamRepository    bienTheSanPhamRepository;
    private final NhaCungCapRepository        nhaCungCapRepository;
    private final NguoiDungRepository         nguoiDungRepository;
    private final MayDienThoaiRepository      mayDienThoaiRepository;

    // =======================================================================
    // PUBLIC METHODS — NGHIỆP VỤ CHÍNH
    // =======================================================================

    // -----------------------------------------------------------------------
    // HÀM 1: TẠO PHIẾU NHẬP KHO
    // -----------------------------------------------------------------------

    /**
     * Tạo mới phiếu nhập kho và cộng tồn kho trực tiếp vào kho duy nhất.
     *
     * <h3>Luồng xử lý:</h3>
     * <ol>
     *   <li>Validate kho đích tồn tại và đang hoạt động (phải là kho ID=1).</li>
     *   <li>Validate từng dòng chi tiết (biến thể tồn tại, số lượng &gt; 0).</li>
     *   <li>Build và lưu {@link PhieuNhapKho} cùng các {@link ChiTietPhieuNhap}.</li>
     *   <li>Với mỗi dòng chi tiết, gọi {@link #congTonKho} để cập nhật bảng ton_kho.</li>
     * </ol>
     *
     * @param request    Thông tin phiếu nhập.
     * @param nguoiTaoId ID nhân viên / admin tạo phiếu.
     * @return Phiếu nhập kho đã được lưu.
     */
    @Override
    @Transactional
    public PhieuNhapKho taoPhieuNhapKho(TaoPhieuNhapKhoRequest request, Integer nguoiTaoId) {

        log.info("[KhoService] Bắt đầu tạo phiếu nhập kho — khoId={}, nguoiTaoId={}", request.getKhoId(), nguoiTaoId);

        // ------------------------------------------------------------------
        // Bước 1: Validate Kho — BẮT BUỘC phải là kho ID = 1
        // ------------------------------------------------------------------
        Kho kho = khoRepository.findById(request.getKhoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy kho có ID: " + request.getKhoId()));

        if (!KHO_DUY_NHAT_ID.equals(kho.getId())) {
            throw new IllegalArgumentException(
                    "Vi phạm quy tắc nghiệp vụ: Chỉ được phép nhập hàng vào kho ID=1. " +
                            "Kho [" + kho.getTenKho() + "] không hợp lệ.");
        }

        if (Boolean.FALSE.equals(kho.getKichHoat())) {
            throw new IllegalArgumentException("Kho [" + kho.getTenKho() + "] đang bị vô hiệu hóa.");
        }

        // ------------------------------------------------------------------
        // Bước 2: Validate danh sách chi tiết không rỗng
        // ------------------------------------------------------------------
        if (request.getChiTiets() == null || request.getChiTiets().isEmpty()) {
            throw new IllegalArgumentException("Phiếu nhập kho phải có ít nhất 1 dòng chi tiết.");
        }

        // ------------------------------------------------------------------
        // Bước 3: Load các entity phụ (người tạo, NCC tùy chọn)
        // ------------------------------------------------------------------
        NguoiDung nguoiTao = nguoiDungRepository.findById(nguoiTaoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy người dùng có ID: " + nguoiTaoId));

        NhaCungCap nhaCungCap = null;
        if (request.getNhaCungCapId() != null) {
            nhaCungCap = nhaCungCapRepository.findById(request.getNhaCungCapId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy nhà cung cấp có ID: " + request.getNhaCungCapId()));
        }

        // ------------------------------------------------------------------
        // Bước 4: Xây dựng đối tượng PhieuNhapKho
        // ------------------------------------------------------------------
        String maPhieu = sinhMaPhieu("PNK");
        PhieuNhapKho phieuNhapKho = PhieuNhapKho.builder()
                .maPhieu(maPhieu)
                .kho(kho)
                .nhaCungCap(nhaCungCap)
                .nguoiTao(nguoiTao)
                .ngayNhap(LocalDateTime.now())
                .tongTien(BigDecimal.ZERO)   // Sẽ tính và cập nhật ở bước 5
                .trangThai("hoan_thanh")     // Chốt ngay — không qua duyệt (system_rules.md §3.2)
                .ghiChu(request.getGhiChu())
                .build();

        // Lưu header trước để có ID cho cascade
        phieuNhapKho = phieuNhapKhoRepository.save(phieuNhapKho);

        // ------------------------------------------------------------------
        // Bước 5: Xây dựng và lưu từng dòng ChiTietPhieuNhap
        // ------------------------------------------------------------------
        BigDecimal tongTien = BigDecimal.ZERO;
        List<ChiTietPhieuNhap> danhSachChiTiet = new ArrayList<>();

        for (ChiTietNhapRequest dtoChiTiet : request.getChiTiets()) {

            // Validate số lượng
            if (dtoChiTiet.getSoLuong() == null || dtoChiTiet.getSoLuong() <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng nhập phải lớn hơn 0 (biến thể ID: " + dtoChiTiet.getBienTheSanPhamId() + ").");
            }

            // Validate đơn giá
            if (dtoChiTiet.getDonGiaNhap() == null || dtoChiTiet.getDonGiaNhap().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Đơn giá nhập không được âm (biến thể ID: " + dtoChiTiet.getBienTheSanPhamId() + ").");
            }

            BienTheSanPham bienThe = bienTheSanPhamRepository.findById(dtoChiTiet.getBienTheSanPhamId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy biến thể sản phẩm ID: " + dtoChiTiet.getBienTheSanPhamId()));

            ChiTietPhieuNhap chiTiet = ChiTietPhieuNhap.builder()
                    .phieuNhapKho(phieuNhapKho)
                    .bienTheSanPham(bienThe)
                    .soLuong(dtoChiTiet.getSoLuong())
                    .donGiaNhap(dtoChiTiet.getDonGiaNhap())
                    .build();

            danhSachChiTiet.add(chiTiet);
            
            // Nếu có đính kèm danh sách IMEI từ Excel, lưu vào MayDienThoai
            if (dtoChiTiet.getImeiRecords() != null && !dtoChiTiet.getImeiRecords().isEmpty()) {
                List<MayDienThoai> mayDienThoais = new java.util.ArrayList<>();
                for (org.example.primemobile.dto.kho.ImeiImportRecord imeiRecord : dtoChiTiet.getImeiRecords()) {
                    MayDienThoai mayDienThoai = MayDienThoai.builder()
                            .bienTheSanPham(bienThe)
                            .imei1(imeiRecord.getImei1())
                            .imei2(imeiRecord.getImei2() != null && !imeiRecord.getImei2().isEmpty() ? imeiRecord.getImei2() : null)
                            .tinhTrang("trong_kho")
                            .ngayNhapKho(LocalDateTime.now())
                            .ghiChu("Import từ Excel, Phiếu nhập: " + maPhieu)
                            .build();
                    mayDienThoais.add(mayDienThoai);
                }
                mayDienThoaiRepository.saveAll(mayDienThoais);
            }

            // Cộng vào tổng tiền (thanhTien là computed column nên tính manual ở Service)
            tongTien = tongTien.add(
                    dtoChiTiet.getDonGiaNhap().multiply(BigDecimal.valueOf(dtoChiTiet.getSoLuong()))
            );
        }

        chiTietPhieuNhapRepository.saveAll(danhSachChiTiet);

        // Cập nhật tổng tiền lên header
        phieuNhapKho.setTongTien(tongTien);
        phieuNhapKhoRepository.save(phieuNhapKho);

        // ------------------------------------------------------------------
        // Bước 6: CỘNG TỒN KHO — Cập nhật bảng ton_kho
        // ------------------------------------------------------------------
        for (ChiTietPhieuNhap chiTiet : danhSachChiTiet) {
            congTonKho(kho, chiTiet.getBienTheSanPham(), chiTiet.getSoLuong());
        }

        log.info("[KhoService] Tạo phiếu nhập kho thành công — mã phiếu={}, tổng tiền={}", maPhieu, tongTien);
        return phieuNhapKho;
    }

    // -----------------------------------------------------------------------
    // HÀM QUERY
    // -----------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TonKho> getTonKhoByKho(Integer khoId) {
        khoRepository.findById(khoId)
                .orElseThrow(() -> new EntityNotFoundException("Không tìm thấy kho ID: " + khoId));
        return tonKhoRepository.findByKhoIdWithDetails(khoId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TonKho getTonKhoChiTiet(Integer khoId, Integer bienTheSanPhamId) {
        return tonKhoRepository.findByKhoIdAndBienTheId(khoId, bienTheSanPhamId)
                .orElseThrow(() -> new EntityNotFoundException(String.format(
                        "Không tìm thấy tồn kho — khoId=%d, bienTheId=%d.", khoId, bienTheSanPhamId)));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PhieuNhapKho> layDanhSachPhieuNhap(
            org.springframework.data.domain.Pageable pageable) {
        log.debug("[KhoService] layDanhSachPhieuNhap — page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return phieuNhapKhoRepository.layDanhSachPhanTrang(pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PhieuNhapKho layChiTietPhieuNhap(Integer id) {
        log.debug("[KhoService] layChiTietPhieuNhap — id={}", id);
        return phieuNhapKhoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phiếu nhập kho có ID: " + id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<MayDienThoai> layDanhSachImeiTrongKho(Integer bienTheSanPhamId) {

        if (!bienTheSanPhamRepository.existsById(bienTheSanPhamId)) {
            throw new EntityNotFoundException("Không tìm thấy biến thể sản phẩm có ID: " + bienTheSanPhamId);
        }

        return mayDienThoaiRepository.findTrongKhoByBienTheId(bienTheSanPhamId);
    }

    // =======================================================================
    // PRIVATE HELPER METHODS
    // =======================================================================

    /**
     * Cộng thêm số lượng vào tồn kho của một SKU tại một kho.
     * Nếu bản ghi tồn kho chưa tồn tại, tự động khởi tạo mới.
     *
     * @param kho            Kho nhận hàng.
     * @param bienTheSanPham Biến thể SKU.
     * @param soLuong        Số lượng cần cộng thêm (phải &gt; 0).
     */
    private void congTonKho(Kho kho, BienTheSanPham bienTheSanPham, int soLuong) {
        TonKho tonKho = tonKhoRepository.findByKhoAndBienTheSanPham(kho, bienTheSanPham)
                .orElse(TonKho.builder()
                        .kho(kho)
                        .bienTheSanPham(bienTheSanPham)
                        .soLuong(0)
                        .updatedAt(LocalDateTime.now())
                        .build());

        tonKho.setSoLuong(tonKho.getSoLuong() + soLuong);
        tonKho.setUpdatedAt(LocalDateTime.now());
        tonKhoRepository.save(tonKho);

        log.debug("[KhoService] congTonKho: kho=[{}] sku=[{}] +{} → tồn={}", kho.getTenKho(),
                bienTheSanPham.getMaSku(), soLuong, tonKho.getSoLuong());
    }

    /**
     * Sinh mã phiếu tự động theo format: {@code PREFIX-YYYYMM-&lt;6 chữ số cuối millis&gt;}.
     *
     * @param prefix Tiền tố mã phiếu (ví dụ: "PNK").
     * @return Chuỗi mã phiếu duy nhất.
     */
    private String sinhMaPhieu(String prefix) {
        String datePart  = LocalDateTime.now().format(MA_PHIEU_DATE_FMT);
        String randPart  = String.format("%06d", System.currentTimeMillis() % 1_000_000L);
        return prefix + "-" + datePart + "-" + randPart;
    }
}