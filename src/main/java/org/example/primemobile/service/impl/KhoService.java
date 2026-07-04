package org.example.primemobile.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.dto.kho.TaoPhieuChuyenKhoRequest;
import org.example.primemobile.dto.kho.TaoPhieuChuyenKhoRequest.ChiTietChuyenRequest;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service xử lý toàn bộ nghiệp vụ Kho hàng cho PrimeMobile.
 *
 * <h2>Quy tắc nghiệp vụ cốt lõi (system_rules.md §3.1, §3.2):</h2>
 * <ul>
 *   <li>Nhập hàng: NCC → <b>Kho Tổng</b> (loai = 'kho_tong') — không nhập trực tiếp vào Kho Online.</li>
 *   <li>Chuyển kho: Kho Tổng → Kho Online (phục vụ đơn hàng online) hoặc chiều ngược lại.</li>
 *   <li><b>Safety Stock Rule</b>: Sau bất kỳ thao tác trừ kho nào, tồn kho còn lại
 *       TUYỆT ĐỐI KHÔNG được dưới {@value #TON_KHO_TOI_THIEU} đơn vị / SKU / kho.</li>
 *   <li>Phiếu nhập và phiếu chuyển kho đều được <b>chốt ngay</b> (hoan_thanh) khi tạo,
 *       không qua bước chờ duyệt.</li>
 *   <li>Khi chuyển kho, nếu có danh sách IMEI cụ thể, hệ thống sẽ cập nhật kho_id của các IMEI đó.</li>
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

    /** Pattern sinh mã phiếu: PNK-YYYYMM-<millis 6 chữ số cuối>. */
    private static final DateTimeFormatter MA_PHIEU_DATE_FMT = DateTimeFormatter.ofPattern("yyyyMM");

    // -----------------------------------------------------------------------
    // DEPENDENCIES (inject qua constructor nhờ @RequiredArgsConstructor)
    // -----------------------------------------------------------------------

    private final KhoRepository               khoRepository;
    private final TonKhoRepository            tonKhoRepository;
    private final PhieuNhapKhoRepository      phieuNhapKhoRepository;
    private final ChiTietPhieuNhapRepository  chiTietPhieuNhapRepository;
    private final PhieuChuyenKhoRepository    phieuChuyenKhoRepository;
    private final ChiTietChuyenKhoRepository  chiTietChuyenKhoRepository;
    private final BienTheSanPhamRepository    bienTheSanPhamRepository;
    private final NhaCungCapRepository        nhaCungCapRepository;
    private final NguoiDungRepository         nguoiDungRepository;
    private final MayDienThoaiRepository      mayDienThoaiRepository; // ✅ Thêm để quản lý IMEI

    // =======================================================================
    // PUBLIC METHODS — NGHIỆP VỤ CHÍNH
    // =======================================================================

    // -----------------------------------------------------------------------
    // HÀM 1: TẠO PHIẾU NHẬP KHO
    // -----------------------------------------------------------------------

    /**
     * Tạo mới phiếu nhập kho và cộng tồn kho trực tiếp vào Kho Tổng.
     *
     * <h3>Luồng xử lý:</h3>
     * <ol>
     *   <li>Validate kho đích phải là <b>kho_tong</b>.</li>
     *   <li>Validate từng dòng chi tiết (biến thể tồn tại, số lượng > 0).</li>
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
        // Bước 1: Validate Kho — BẮT BUỘC phải là Kho Tổng
        // ------------------------------------------------------------------
        Kho kho = khoRepository.findById(request.getKhoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy kho có ID: " + request.getKhoId()));

        if (!"kho_tong".equals(kho.getLoai())) {
            throw new IllegalArgumentException(
                    "Vi phạm quy tắc nghiệp vụ: Chỉ được phép nhập hàng vào Kho Tổng. " +
                            "Kho [" + kho.getTenKho() + "] có loại [" + kho.getLoai() + "] không hợp lệ.");
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
        // Bước 6: CỘNG TỒN KHO — Cập nhật bảng ton_kho (Kho Tổng)
        // ------------------------------------------------------------------
        for (ChiTietPhieuNhap chiTiet : danhSachChiTiet) {
            congTonKho(kho, chiTiet.getBienTheSanPham(), chiTiet.getSoLuong());
        }

        log.info("[KhoService] Tạo phiếu nhập kho thành công — mã phiếu={}, tổng tiền={}", maPhieu, tongTien);
        return phieuNhapKho;
    }

    // -----------------------------------------------------------------------
    // HÀM 2: TẠO PHIẾU CHUYỂN KHO (CÓ KIỂM TRA SAFETY STOCK & IMEI)
    // -----------------------------------------------------------------------

    /**
     * Tạo phiếu chuyển kho giữa 2 kho với kiểm tra Safety Stock Rule bắt buộc.
     * <p>
     * Nếu request có danh sách IMEI cụ thể cho từng chi tiết, hệ thống sẽ:
     * <ul>
     *   <li>Kiểm tra số lượng IMEI khớp với số lượng chuyển.</li>
     *   <li>Kiểm tra từng IMEI có tinh_trang = 'trong_kho' và thuộc kho nguồn.</li>
     *   <li>Cập nhật kho_id của các IMEI đó sang kho đích.</li>
     * </ul>
     *
     * <h3>⚠️ Chiến lược Fail-Fast (Pre-validate ALL trước khi Execute):</h3>
     * Toàn bộ danh sách được kiểm tra Safety Stock và IMEI trong 1 vòng lặp đầu tiên.
     * Nếu BẤT KỲ dòng nào vi phạm → ném ngoại lệ NGAY, không thực hiện bất cứ thay đổi nào.
     *
     * @param request    Thông tin phiếu chuyển (có thể chứa danh sách IMEI).
     * @param nguoiTaoId ID nhân viên / admin tạo phiếu.
     * @return Phiếu chuyển kho đã được lưu.
     * @throws IllegalArgumentException   Nếu vi phạm Safety Stock Rule, khoNguon = khoDich,
     *                                    hoặc IMEI không hợp lệ.
     * @throws EntityNotFoundException    Nếu không tìm thấy kho, biến thể, tồn kho, hoặc IMEI.
     */
    @Override
    @Transactional
    public PhieuChuyenKho taoPhieuChuyenKho(TaoPhieuChuyenKhoRequest request, Integer nguoiTaoId) {

        log.info("[KhoService] Bắt đầu tạo phiếu chuyển kho — nguon={}, dich={}, nguoiTaoId={}",
                request.getKhoNguonId(), request.getKhoDichId(), nguoiTaoId);

        // ------------------------------------------------------------------
        // Bước 1: Validate khoNguon ≠ khoDich
        // ------------------------------------------------------------------
        if (request.getKhoNguonId().equals(request.getKhoDichId())) {
            throw new IllegalArgumentException(
                    "Kho nguồn và kho đích không được trùng nhau (ID: " + request.getKhoNguonId() + ").");
        }

        if (request.getChiTiets() == null || request.getChiTiets().isEmpty()) {
            throw new IllegalArgumentException("Phiếu chuyển kho phải có ít nhất 1 dòng chi tiết.");
        }

        // ------------------------------------------------------------------
        // Bước 2: Load các entity header
        // ------------------------------------------------------------------
        Kho khoNguon = khoRepository.findById(request.getKhoNguonId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy kho nguồn ID: " + request.getKhoNguonId()));

        Kho khoDich = khoRepository.findById(request.getKhoDichId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy kho đích ID: " + request.getKhoDichId()));

        NguoiDung nguoiTao = nguoiDungRepository.findById(nguoiTaoId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy người dùng ID: " + nguoiTaoId));

        // ------------------------------------------------------------------
        // Bước 3: PRE-VALIDATE — Kiểm tra Safety Stock Rule & IMEI
        // Cache kết quả để tránh query 2 lần.
        // ------------------------------------------------------------------
        // Map<bienTheSanPhamId, TonKho tại khoNguon>
        Map<Integer, TonKho> tonKhoNguonCache = new HashMap<>();
        // Map<bienTheSanPhamId, BienTheSanPham>
        Map<Integer, BienTheSanPham> bienTheCache = new HashMap<>();
        // Map<chiTietIndex, List<MayDienThoai>> để lưu IMEI đã validate
        Map<Integer, List<MayDienThoai>> imeiCache = new HashMap<>();

        int chiTietIndex = 0;
        for (ChiTietChuyenRequest dtoChiTiet : request.getChiTiets()) {
            chiTietIndex++;

            // Validate số lượng đầu vào
            if (dtoChiTiet.getSoLuong() == null || dtoChiTiet.getSoLuong() <= 0) {
                throw new IllegalArgumentException(
                        "Số lượng chuyển phải lớn hơn 0 (biến thể ID: " + dtoChiTiet.getBienTheSanPhamId() + ").");
            }

            // Load biến thể
            BienTheSanPham bienThe = bienTheSanPhamRepository.findById(dtoChiTiet.getBienTheSanPhamId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "Không tìm thấy biến thể sản phẩm ID: " + dtoChiTiet.getBienTheSanPhamId()));
            bienTheCache.put(dtoChiTiet.getBienTheSanPhamId(), bienThe);

            // Load tồn kho tại kho nguồn
            TonKho tonKhoNguon = tonKhoRepository.findByKhoAndBienTheSanPham(khoNguon, bienThe)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(
                            "Sản phẩm [%s] không có trong kho nguồn [%s].",
                            bienThe.getMaSku(), khoNguon.getTenKho())));
            tonKhoNguonCache.put(dtoChiTiet.getBienTheSanPhamId(), tonKhoNguon);

            // ================================================================
            // ⚠️ KIỂM TRA SAFETY STOCK RULE
            // ================================================================
            int soLuongHienTai   = tonKhoNguon.getSoLuong();
            int soLuongMuonChuyen = dtoChiTiet.getSoLuong();
            int soLuongSauKhiTru = soLuongHienTai - soLuongMuonChuyen;

            if (soLuongSauKhiTru < TON_KHO_TOI_THIEU) {
                throw new IllegalArgumentException(String.format(
                        "Vi phạm quy tắc tồn kho tối thiểu. " +
                                "Sản phẩm [%s] tại kho [%s]: " +
                                "Tồn kho hiện tại = %d, Muốn chuyển = %d, Còn lại = %d. " +
                                "Số lượng còn lại trong kho không được dưới %d sản phẩm.",
                        bienThe.getMaSku(), khoNguon.getTenKho(),
                        soLuongHienTai, soLuongMuonChuyen, soLuongSauKhiTru,
                        TON_KHO_TOI_THIEU
                ));
            }

            // ================================================================
            // ⚠️ KIỂM TRA IMEI (nếu có)
            // ================================================================
            List<String> imeiList = dtoChiTiet.getImeiList();
            if (imeiList != null && !imeiList.isEmpty()) {
                // Số lượng IMEI phải khớp với số lượng chuyển
                if (imeiList.size() != soLuongMuonChuyen) {
                    throw new IllegalArgumentException(String.format(
                            "Số lượng IMEI (%d) không khớp với số lượng chuyển (%d) cho biến thể [%s].",
                            imeiList.size(), soLuongMuonChuyen, bienThe.getMaSku()));
                }

                // Kiểm tra từng IMEI
                List<MayDienThoai> imeiEntities = new ArrayList<>();
                for (String imei : imeiList) {
                    MayDienThoai may = mayDienThoaiRepository.findByImei1(imei.trim())
                            .orElseThrow(() -> new EntityNotFoundException(
                                    "Không tìm thấy IMEI: " + imei.trim()));

                    // Kiểm tra IMEI thuộc đúng biến thể
                    if (!may.getBienTheSanPham().getId().equals(bienThe.getId())) {
                        throw new IllegalArgumentException(String.format(
                                "IMEI [%s] không thuộc biến thể [%s].",
                                imei.trim(), bienThe.getMaSku()));
                    }

                    // Kiểm tra IMEI đang trong kho
                    if (!"trong_kho".equals(may.getTinhTrang())) {
                        throw new IllegalArgumentException(String.format(
                                "IMEI [%s] không ở trạng thái 'trong_kho' (hiện tại: '%s').",
                                imei.trim(), may.getTinhTrang()));
                    }

                    // Kiểm tra IMEI đang ở kho nguồn
                    if (may.getKho() == null || !may.getKho().getId().equals(khoNguon.getId())) {
                        throw new IllegalArgumentException(String.format(
                                "IMEI [%s] không ở kho nguồn [%s].",
                                imei.trim(), khoNguon.getTenKho()));
                    }

                    imeiEntities.add(may);
                }
                imeiCache.put(chiTietIndex - 1, imeiEntities);
            }
            // ================================================================
        }

        // ------------------------------------------------------------------
        // Bước 4: Tất cả validation đã pass — Lưu PhieuChuyenKho
        // ------------------------------------------------------------------
        String maPhieu = sinhMaPhieu("PCK");
        PhieuChuyenKho phieuChuyenKho = PhieuChuyenKho.builder()
                .maPhieu(maPhieu)
                .khoNguon(khoNguon)
                .khoDich(khoDich)
                .nguoiTao(nguoiTao)
                .ngayChuyen(LocalDateTime.now())
                .lyDo(request.getLyDo())
                .trangThai("hoan_thanh")
                .build();

        phieuChuyenKho = phieuChuyenKhoRepository.save(phieuChuyenKho);

        // ------------------------------------------------------------------
        // Bước 5: EXECUTE — Trừ kho nguồn, Cộng kho đích, Lưu chi tiết, Cập nhật IMEI
        // ------------------------------------------------------------------
        List<ChiTietChuyenKho> danhSachChiTiet = new ArrayList<>();
        chiTietIndex = 0;

        for (ChiTietChuyenRequest dtoChiTiet : request.getChiTiets()) {
            BienTheSanPham bienThe     = bienTheCache.get(dtoChiTiet.getBienTheSanPhamId());
            TonKho tonKhoNguon         = tonKhoNguonCache.get(dtoChiTiet.getBienTheSanPhamId());
            int soLuongChuyen          = dtoChiTiet.getSoLuong();

            // Trừ số lượng tại kho nguồn (đã validated)
            truTonKho(tonKhoNguon, soLuongChuyen);

            // Cộng số lượng vào kho đích
            congTonKho(khoDich, bienThe, soLuongChuyen);

            // Cập nhật IMEI nếu có
            List<MayDienThoai> imeiEntities = imeiCache.get(chiTietIndex);
            if (imeiEntities != null && !imeiEntities.isEmpty()) {
                for (MayDienThoai may : imeiEntities) {
                    may.setKho(khoDich);
                    mayDienThoaiRepository.save(may);
                }
                log.debug("[KhoService] Đã cập nhật {} IMEI sang kho [{}] cho SKU [{}]",
                        imeiEntities.size(), khoDich.getTenKho(), bienThe.getMaSku());
            }

            // Build dòng chi tiết
            ChiTietChuyenKho chiTiet = ChiTietChuyenKho.builder()
                    .phieuChuyenKho(phieuChuyenKho)
                    .bienTheSanPham(bienThe)
                    .soLuong(soLuongChuyen)
                    .build();
            danhSachChiTiet.add(chiTiet);

            chiTietIndex++;
        }

        chiTietChuyenKhoRepository.saveAll(danhSachChiTiet);

        log.info("[KhoService] Tạo phiếu chuyển kho thành công — mã phiếu={}", maPhieu);
        return phieuChuyenKho;
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

    // -----------------------------------------------------------------------
    // HÀM 5: DANH SÁCH PHIẾU NHẬP KHO (phân trang)
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PhieuNhapKho> layDanhSachPhieuNhap(
            org.springframework.data.domain.Pageable pageable) {
        log.debug("[KhoService] layDanhSachPhieuNhap — page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return phieuNhapKhoRepository.layDanhSachPhanTrang(pageable);
    }

    // -----------------------------------------------------------------------
    // HÀM 6: CHI TIẾT PHIẾU NHẬP KHO
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PhieuNhapKho layChiTietPhieuNhap(Integer id) {
        log.debug("[KhoService] layChiTietPhieuNhap — id={}", id);
        return phieuNhapKhoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phiếu nhập kho có ID: " + id));
    }

    // -----------------------------------------------------------------------
    // HÀM 7: DANH SÁCH PHIẾU CHUYỂN KHO (phân trang)
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<PhieuChuyenKho> layDanhSachPhieuChuyen(
            org.springframework.data.domain.Pageable pageable) {
        log.debug("[KhoService] layDanhSachPhieuChuyen — page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return phieuChuyenKhoRepository.layDanhSachPhanTrang(pageable);
    }

    // -----------------------------------------------------------------------
    // HÀM 8: CHI TIẾT PHIẾU CHUYỂN KHO
    // -----------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PhieuChuyenKho layChiTietPhieuChuyen(Integer id) {
        log.debug("[KhoService] layChiTietPhieuChuyen — id={}", id);
        return phieuChuyenKhoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Không tìm thấy phiếu chuyển kho có ID: " + id));
    }

    // =======================================================================
    // PUBLIC METHOD — LẤY DANH SÁCH IMEI TRONG KHO
    // =======================================================================

    @Override
    @Transactional(readOnly = true)
    public List<MayDienThoai> layDanhSachImeiTrongKho(Integer khoId, Integer bienTheSanPhamId) {
        // Kiểm tra kho tồn tại
        if (!khoRepository.existsById(khoId)) {
            throw new EntityNotFoundException("Không tìm thấy kho có ID: " + khoId);
        }

        // Kiểm tra biến thể tồn tại
        if (!bienTheSanPhamRepository.existsById(bienTheSanPhamId)) {
            throw new EntityNotFoundException("Không tìm thấy biến thể sản phẩm có ID: " + bienTheSanPhamId);
        }

        return mayDienThoaiRepository.findTrongKhoByBienTheIdAndKhoId(bienTheSanPhamId, khoId);
    }

    // =======================================================================
    // PRIVATE HELPER METHODS
    // =======================================================================

    /**
     * Cộng thêm số lượng vào tồn kho của một SKU tại một kho.
     * Nếu bản ghi tồn kho chưa tồn tại, tự động khởi tạo mới.
     *
     * @param kho           Kho nhận hàng.
     * @param bienTheSanPham Biến thể SKU.
     * @param soLuong       Số lượng cần cộng thêm (phải > 0).
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
     * Trừ số lượng khỏi tồn kho (dùng đối tượng TonKho đã load sẵn từ cache).
     * <p>
     * <b>Lưu ý:</b> Hàm này KHÔNG tự kiểm tra Safety Stock vì việc đó đã được
     * thực hiện ở bước pre-validate.
     *
     * @param tonKho     Bản ghi tồn kho cần trừ.
     * @param soLuong    Số lượng cần trừ.
     */
    private void truTonKho(TonKho tonKho, int soLuong) {
        tonKho.setSoLuong(tonKho.getSoLuong() - soLuong);
        tonKho.setUpdatedAt(LocalDateTime.now());
        tonKhoRepository.save(tonKho);

        log.debug("[KhoService] truTonKho: kho=[{}] sku=[{}] -{} → tồn={}",
                tonKho.getKho().getTenKho(),
                tonKho.getBienTheSanPham().getMaSku(),
                soLuong, tonKho.getSoLuong());
    }

    /**
     * Sinh mã phiếu tự động theo format: {@code PREFIX-YYYYMM-<6 chữ số cuối millis>}.
     *
     * @param prefix Tiền tố mã phiếu (ví dụ: "PNK", "PCK").
     * @return Chuỗi mã phiếu duy nhất.
     */
    private String sinhMaPhieu(String prefix) {
        String datePart  = LocalDateTime.now().format(MA_PHIEU_DATE_FMT);
        String randPart  = String.format("%06d", System.currentTimeMillis() % 1_000_000L);
        return prefix + "-" + datePart + "-" + randPart;
    }
}