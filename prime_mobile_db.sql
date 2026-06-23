IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'PrimeMobile')
    CREATE DATABASE PrimeMobile COLLATE Vietnamese_CI_AS;
GO

USE PrimeMobile;
GO

-- =====================================================
-- MODULE 1: NGƯỜI DÙNG & PHÂN QUYỀN
-- Bỏ bảng nhan_vien riêng, dùng nguoi_dung.vai_tro trực tiếp
-- =====================================================

CREATE TABLE nguoi_dung (
                            id                 INT           IDENTITY(1,1) PRIMARY KEY,
                            email              VARCHAR(100)  NOT NULL,
                            mat_khau           VARCHAR(255)  NOT NULL,
                            ho_ten             NVARCHAR(100) NOT NULL,
                            so_dien_thoai      VARCHAR(20),
                            vai_tro            VARCHAR(15)   NOT NULL DEFAULT 'KhachHang',
                            trang_thai         VARCHAR(10)   NOT NULL DEFAULT 'hoat_dong',
                            lan_dang_nhap_cuoi DATETIME2     NULL,
                            ngay_tao           DATETIME2     NOT NULL DEFAULT GETDATE(),
                            updated_at         DATETIME2     NOT NULL DEFAULT GETDATE(),
                            CONSTRAINT uq_nd_email       UNIQUE (email),
                            CONSTRAINT chk_nd_vai_tro    CHECK (vai_tro    IN ('Admin','NhanVien','KhachHang')),
                            CONSTRAINT chk_nd_trang_thai CHECK (trang_thai IN ('hoat_dong','khoa'))
);
GO

CREATE INDEX idx_nd_email ON nguoi_dung (email);
GO

-- khach_hang: khách vãng lai (nguoi_dung_id NULL) hoặc có tài khoản
CREATE TABLE khach_hang (
                            id               INT           IDENTITY(1,1) PRIMARY KEY,
                            nguoi_dung_id    INT           NULL,
                            ho_ten           NVARCHAR(100) NOT NULL,
                            email            VARCHAR(100),
                            so_dien_thoai    VARCHAR(20)   NOT NULL,
                            gioi_tinh        VARCHAR(5)    NULL,
                            ngay_sinh        DATE          NULL,
                            diem_tich_luy    INT           NOT NULL DEFAULT 0,
                            hang_thanh_vien  VARCHAR(15)   NOT NULL DEFAULT 'dong',
                            tong_chi_tieu    DECIMAL(15,2) NOT NULL DEFAULT 0,
                            ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
                            updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
                            CONSTRAINT uq_kh_nguoi_dung  UNIQUE (nguoi_dung_id),
                            CONSTRAINT chk_kh_gioi_tinh  CHECK (gioi_tinh       IN ('Nam','Nu','Khac')),
                            CONSTRAINT chk_kh_hang       CHECK (hang_thanh_vien IN ('dong','bac','vang','kim_cuong')),
                            CONSTRAINT fk_kh_nd          FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_kh_sdt   ON khach_hang (so_dien_thoai);
CREATE INDEX idx_kh_email ON khach_hang (email);
GO

-- =====================================================
-- MODULE 2: SẢN PHẨM & BIẾN THỂ
-- Chỉ bán duy nhất điện thoại không bán phụ kiến và những thứ khác
-- Bỏ bảng sku (gộp ma_sku, barcode vào bien_the_san_pham)
-- Giữ may_dien_thoai để quản lý IMEI từng máy vật lý
-- =====================================================

CREATE TABLE danh_muc (
                          id           INT           IDENTITY(1,1) PRIMARY KEY,
                          ten_danh_muc NVARCHAR(100) NOT NULL,
                          slug         VARCHAR(100)  NOT NULL,
                          mo_ta        NVARCHAR(MAX),
                          thu_tu       INT           NOT NULL DEFAULT 0,
                          kich_hoat    BIT           NOT NULL DEFAULT 1,
                          CONSTRAINT uq_dm_ten  UNIQUE (ten_danh_muc),
                          CONSTRAINT uq_dm_slug UNIQUE (slug)
);
GO

CREATE TABLE hang_san_xuat (
                               id       INT           IDENTITY(1,1) PRIMARY KEY,
                               ten_hang NVARCHAR(100) NOT NULL,
                               logo     VARCHAR(255),
                               quoc_gia NVARCHAR(50),
                               CONSTRAINT uq_hsx_ten UNIQUE (ten_hang)
);
GO

CREATE TABLE san_pham (
                          id               INT           IDENTITY(1,1) PRIMARY KEY,
                          ma_san_pham      VARCHAR(50)   NOT NULL,
                          ten_san_pham     NVARCHAR(255) NOT NULL,
                          danh_muc_id      INT           NOT NULL,
                          hang_san_xuat_id INT           NOT NULL,
                          mo_ta_ngan       NVARCHAR(500),
                          mo_ta_chi_tiet   NVARCHAR(MAX),
                          nam_ra_mat       SMALLINT,
                          bao_hanh_thang   INT           NOT NULL DEFAULT 12,
                          trang_thai       VARCHAR(15)   NOT NULL DEFAULT 'dang_ban',
                          luot_xem         INT           NOT NULL DEFAULT 0,
                          ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
                          updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT uq_sp_ma          UNIQUE (ma_san_pham),
                          CONSTRAINT chk_sp_trang_thai CHECK (trang_thai IN ('dang_ban','ngung_ban','sap_ra_mat')),
                          CONSTRAINT fk_sp_dm          FOREIGN KEY (danh_muc_id)      REFERENCES danh_muc(id),
                          CONSTRAINT fk_sp_hsx         FOREIGN KEY (hang_san_xuat_id) REFERENCES hang_san_xuat(id)
);
GO

CREATE INDEX idx_sp_ten  ON san_pham (ten_san_pham);
CREATE INDEX idx_sp_dm   ON san_pham (danh_muc_id, trang_thai);
CREATE INDEX idx_sp_hang ON san_pham (hang_san_xuat_id, trang_thai);
GO

-- bien_the_san_pham: 1 biến thể = 1 SKU (gộp ma_sku, barcode vào đây)
CREATE TABLE bien_the_san_pham (
                                   id               INT           IDENTITY(1,1) PRIMARY KEY,
                                   san_pham_id      INT           NOT NULL,
                                   ma_sku           VARCHAR(100)  NOT NULL,
                                   barcode          VARCHAR(50)   NULL,
                                   mau_sac          NVARCHAR(50)  NOT NULL,
                                   ma_mau_hex       VARCHAR(7),
                                   ram_gb           INT           NOT NULL,
                                   luu_tru_gb       INT           NOT NULL,
                                   loai_luu_tru     VARCHAR(20)   NOT NULL DEFAULT 'UFS',
                                   gia_nhap         DECIMAL(15,2) NOT NULL,
                                   gia_ban          DECIMAL(15,2) NOT NULL,
                                   gia_khuyen_mai   DECIMAL(15,2) NULL,
                                   trong_luong_gram INT,
                                   pin_mAh          INT,
                                   trang_thai       VARCHAR(20)   NOT NULL DEFAULT 'con_hang',
                                   ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
                                   updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
                                   CONSTRAINT uq_bt_ma_sku      UNIQUE (ma_sku),
                                   CONSTRAINT uq_bt_barcode     UNIQUE (barcode),
                                   CONSTRAINT chk_bt_trang_thai CHECK (trang_thai IN ('con_hang','het_hang','ngung_kinh_doanh')),
                                   CONSTRAINT fk_bt_sp          FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_bt_sp      ON bien_the_san_pham (san_pham_id, trang_thai);
CREATE INDEX idx_bt_barcode ON bien_the_san_pham (barcode);
GO

-- may_dien_thoai: theo dõi từng máy vật lý qua IMEI
CREATE TABLE may_dien_thoai (
                                id                   INT           IDENTITY(1,1) PRIMARY KEY,
                                bien_the_san_pham_id INT           NOT NULL,
                                imei1                VARCHAR(15)   NOT NULL,
                                imei2                VARCHAR(15)   NULL,
                                serial               VARCHAR(50)   NULL,
                                tinh_trang           VARCHAR(15)   NOT NULL DEFAULT 'trong_kho',
                                ngay_nhap_kho        DATETIME2     NOT NULL DEFAULT GETDATE(),
                                don_hang_id          INT           NULL,
                                ghi_chu              NVARCHAR(MAX) NULL,
                                CONSTRAINT uq_may_imei1       UNIQUE (imei1),
                                CONSTRAINT uq_may_imei2       UNIQUE (imei2),
                                CONSTRAINT uq_may_serial      UNIQUE (serial),
                                CONSTRAINT chk_may_tinh_trang CHECK (tinh_trang IN ('trong_kho','da_ban','bao_hanh','loi_hong')),
                                CONSTRAINT fk_may_bt          FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

CREATE INDEX idx_imei1          ON may_dien_thoai (imei1);
CREATE INDEX idx_may_tinh_trang ON may_dien_thoai (tinh_trang);
GO

CREATE TABLE hinh_anh_san_pham (
                                   id                   INT          IDENTITY(1,1) PRIMARY KEY,
                                   bien_the_san_pham_id INT          NOT NULL,
                                   duong_dan            VARCHAR(255) NOT NULL,
                                   la_anh_chinh         BIT          NOT NULL DEFAULT 0,
                                   thu_tu               INT          NOT NULL DEFAULT 0,
                                   CONSTRAINT fk_hasp_bt FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id) ON DELETE CASCADE
);
GO

CREATE TABLE thong_so_ky_thuat (
                                   id           INT           IDENTITY(1,1) PRIMARY KEY,
                                   san_pham_id  INT           NOT NULL,
                                   nhom         NVARCHAR(100) NOT NULL DEFAULT N'Thông tin chung',
                                   ten_thong_so NVARCHAR(100) NOT NULL,
                                   gia_tri      NVARCHAR(255) NOT NULL,
                                   thu_tu       INT           NOT NULL DEFAULT 0,
                                   CONSTRAINT fk_tskt_sp FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_tskt_sp ON thong_so_ky_thuat (san_pham_id, nhom);
GO

-- =====================================================
-- MODULE 3: KHO HÀNG
-- Chỉ có 2 kho cố định: kho_tong & kho_online
-- Bỏ loai_kho (dùng CHECK trực tiếp trong kho)
-- ton_kho & phieu tham chiếu bien_the_san_pham_id
-- =====================================================

CREATE TABLE kho (
                     id        INT           IDENTITY(1,1) PRIMARY KEY,
                     ten_kho   NVARCHAR(100) NOT NULL,
                     loai      VARCHAR(15)   NOT NULL,
                     dia_chi   NVARCHAR(255),
                     kich_hoat BIT           NOT NULL DEFAULT 1,
                     CONSTRAINT uq_kho_loai  UNIQUE (loai),
                     CONSTRAINT chk_kho_loai CHECK (loai IN ('kho_tong','kho_online'))
);
GO

CREATE TABLE ton_kho (
                         id                   INT       IDENTITY(1,1) PRIMARY KEY,
                         kho_id               INT       NOT NULL,
                         bien_the_san_pham_id INT       NOT NULL,
                         so_luong             INT       NOT NULL DEFAULT 0,
                         updated_at           DATETIME2 NOT NULL DEFAULT GETDATE(),
                         CONSTRAINT uq_ton_kho UNIQUE (kho_id, bien_the_san_pham_id),
                         CONSTRAINT chk_tk_sl  CHECK (so_luong >= 0),
                         CONSTRAINT fk_tk_kho  FOREIGN KEY (kho_id)               REFERENCES kho(id),
                         CONSTRAINT fk_tk_bt   FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

CREATE INDEX idx_tk_bt ON ton_kho (bien_the_san_pham_id);
GO

-- Phiếu nhập hàng từ NCC vào kho tổng
CREATE TABLE phieu_nhap_kho (
                                id              INT           IDENTITY(1,1) PRIMARY KEY,
                                ma_phieu        VARCHAR(50)   NOT NULL,
                                kho_id          INT           NOT NULL,
                                nha_cung_cap_id INT           NULL,
                                nguoi_tao_id    INT           NOT NULL,
                                ngay_nhap       DATETIME2     NOT NULL DEFAULT GETDATE(),
                                tong_tien       DECIMAL(15,2) NOT NULL DEFAULT 0,
                                trang_thai      VARCHAR(15)   NOT NULL DEFAULT 'hoan_thanh',
                                ghi_chu         NVARCHAR(MAX),
                                CONSTRAINT uq_pnk_ma          UNIQUE (ma_phieu),
                                CONSTRAINT chk_pnk_trang_thai CHECK (trang_thai IN ('hoan_thanh','huy')),
                                CONSTRAINT fk_pnk_kho         FOREIGN KEY (kho_id)       REFERENCES kho(id),
                                CONSTRAINT fk_pnk_nd          FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id)
);
GO

CREATE TABLE chi_tiet_phieu_nhap (
                                     id                   INT           IDENTITY(1,1) PRIMARY KEY,
                                     phieu_nhap_id        INT           NOT NULL,
                                     bien_the_san_pham_id INT           NOT NULL,
                                     so_luong             INT           NOT NULL,
                                     don_gia_nhap         DECIMAL(15,2) NOT NULL,
                                     thanh_tien           AS (so_luong * don_gia_nhap) PERSISTED,
                                     CONSTRAINT fk_ctpn_phieu FOREIGN KEY (phieu_nhap_id)        REFERENCES phieu_nhap_kho(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ctpn_bt    FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

-- Phiếu chuyển kho (kho_tong → kho_online)
CREATE TABLE phieu_chuyen_kho (
                                  id           INT           IDENTITY(1,1) PRIMARY KEY,
                                  ma_phieu     VARCHAR(50)   NOT NULL,
                                  kho_nguon_id INT           NOT NULL,
                                  kho_dich_id  INT           NOT NULL,
                                  nguoi_tao_id INT           NOT NULL,
                                  ngay_chuyen  DATETIME2     NOT NULL DEFAULT GETDATE(),
                                  ly_do        NVARCHAR(255),
                                  trang_thai   VARCHAR(15)   NOT NULL DEFAULT 'hoan_thanh',
                                  CONSTRAINT uq_pck_ma          UNIQUE (ma_phieu),
                                  CONSTRAINT chk_pck_kho_khac   CHECK (kho_nguon_id <> kho_dich_id),
                                  CONSTRAINT chk_pck_trang_thai CHECK (trang_thai IN ('hoan_thanh','huy')),
                                  CONSTRAINT fk_pck_nguon       FOREIGN KEY (kho_nguon_id) REFERENCES kho(id),
                                  CONSTRAINT fk_pck_dich        FOREIGN KEY (kho_dich_id)  REFERENCES kho(id),
                                  CONSTRAINT fk_pck_nd          FOREIGN KEY (nguoi_tao_id) REFERENCES nguoi_dung(id)
);
GO

CREATE TABLE chi_tiet_chuyen_kho (
                                     id                   INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
                                     phieu_chuyen_id      INT NOT NULL,
                                     bien_the_san_pham_id INT NOT NULL,
                                     so_luong             INT NOT NULL,
                                     CONSTRAINT fk_ctck_phieu FOREIGN KEY (phieu_chuyen_id)      REFERENCES phieu_chuyen_kho(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ctck_bt    FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

-- =====================================================
-- MODULE 4: NHÀ CUNG CẤP
-- Giữ nguyên, bỏ don_nhap_hang (không làm workflow đặt hàng NCC)
-- =====================================================

CREATE TABLE nha_cung_cap (
                              id            INT           IDENTITY(1,1) PRIMARY KEY,
                              ma_ncc        VARCHAR(20)   NOT NULL,
                              ten_ncc       NVARCHAR(200) NOT NULL,
                              so_dien_thoai VARCHAR(20),
                              email         VARCHAR(100),
                              dia_chi       NVARCHAR(255),
                              nguoi_lien_he NVARCHAR(100),
                              trang_thai    VARCHAR(20)   NOT NULL DEFAULT 'dang_hop_tac',
                              CONSTRAINT uq_ncc_ma          UNIQUE (ma_ncc),
                              CONSTRAINT chk_ncc_trang_thai CHECK (trang_thai IN ('dang_hop_tac','ngung_hop_tac'))
);
GO

ALTER TABLE phieu_nhap_kho
    ADD CONSTRAINT fk_pnk_ncc FOREIGN KEY (nha_cung_cap_id) REFERENCES nha_cung_cap(id);
GO

-- =====================================================
-- MODULE 5: ĐỊA CHỈ KHÁCH HÀNG
-- Liên kết API giao hàng của Giao Hàng Nhanh
-- =====================================================

-- =====================================================
-- BẢNG ĐỊA CHỈ KHÁCH HÀNG (Lưu cả ID để tính phí, lưu cả Text để hiển thị)
-- =====================================================
CREATE TABLE dia_chi_khach_hang (
                                    id                       INT           IDENTITY(1,1) PRIMARY KEY,
                                    khach_hang_id            INT           NOT NULL,
                                    loai_dia_chi             VARCHAR(15)   NOT NULL DEFAULT 'nha_rieng',
                                    ho_ten_nguoi_nhan        NVARCHAR(100),
                                    so_dien_thoai_nguoi_nhan VARCHAR(20),
                                    dia_chi_chi_tiet         NVARCHAR(255) NOT NULL,
    -- Nhóm 1: Dùng để Frontend gửi cho Backend gọi API tính phí ship GHN
                                    tinh_thanh_id            INT           NOT NULL,
                                    quan_huyen_id            INT           NOT NULL,
                                    phuong_xa_code           VARCHAR(20)   NOT NULL,
    -- Nhóm 2: Dùng để hiển thị giao diện UI ngay lập tức
                                    tinh_thanh_ten           NVARCHAR(100) NOT NULL,
                                    quan_huyen_ten           NVARCHAR(100) NOT NULL,
                                    phuong_xa_ten            NVARCHAR(100) NOT NULL,
                                    mac_dinh                 BIT           NOT NULL DEFAULT 0,
                                    CONSTRAINT chk_dc_loai CHECK (loai_dia_chi IN ('nha_rieng','co_quan','khac')),
                                    CONSTRAINT fk_dc_kh    FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE CASCADE
);
GO

-- =====================================================
-- MODULE 6: GIỎ HÀNG
-- Hỗ trợ cả khách vãng lai (session_id) và khách có tài khoản (khach_hang_id)
-- =====================================================

CREATE TABLE gio_hang (
                          id            INT          IDENTITY(1,1) PRIMARY KEY,
                          khach_hang_id INT          NULL,
                          session_id    VARCHAR(100) NULL,
                          ngay_tao      DATETIME2    NOT NULL DEFAULT GETDATE(),
                          updated_at    DATETIME2    NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT chk_gh_dinh_danh CHECK (khach_hang_id IS NOT NULL OR session_id IS NOT NULL),
                          CONSTRAINT fk_gh_kh          FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_gh_kh      ON gio_hang (khach_hang_id);
CREATE INDEX idx_gh_session ON gio_hang (session_id);
GO

CREATE TABLE chi_tiet_gio_hang (
                                   id                   INT       IDENTITY(1,1) PRIMARY KEY,
                                   gio_hang_id          INT       NOT NULL,
                                   bien_the_san_pham_id INT       NOT NULL,
                                   so_luong             INT       NOT NULL DEFAULT 1,
                                   ngay_them            DATETIME2 NOT NULL DEFAULT GETDATE(),
                                   CONSTRAINT uq_ctgh     UNIQUE (gio_hang_id, bien_the_san_pham_id),
                                   CONSTRAINT chk_ctgh_sl CHECK (so_luong > 0),
                                   CONSTRAINT fk_ctgh_gh  FOREIGN KEY (gio_hang_id)          REFERENCES gio_hang(id)          ON DELETE CASCADE,
                                   CONSTRAINT fk_ctgh_bt  FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

-- =====================================================
-- MODULE 7: ĐƠN HÀNG BÁN
-- Bỏ lich_su_don_hang (không cần audit trail chi tiết)
-- Giữ thanh_toan, phuong_thuc_thanh_toan
-- Thêm ma_giam_gia_id vào don_hang để biết đơn dùng mã nào
-- Thêm hỗ trợ thanh toán QR động qua SePay
-- =====================================================

CREATE TABLE phuong_thuc_thanh_toan (
                                        id        INT           IDENTITY(1,1) PRIMARY KEY,
                                        ten_pttt  NVARCHAR(50)  NOT NULL,
                                        mo_ta     NVARCHAR(255),
                                        kich_hoat BIT           NOT NULL DEFAULT 1,
                                        CONSTRAINT uq_pttt_ten UNIQUE (ten_pttt)
);
GO

-- Cấu hình tài khoản ngân hàng nhận tiền (dùng để sinh QR động qua VietQR)
CREATE TABLE cau_hinh_thanh_toan (
                                     id INT IDENTITY (1, 1) PRIMARY KEY,
                                     ten_ngan_hang NVARCHAR(100) NOT NULL,       -- VD: "MB Bank"
                                     bank_id VARCHAR(20) NOT NULL,        -- Mã VietQR: "MB", "VCB", "TCB"...
                                     so_tai_khoan VARCHAR(50) NOT NULL,        -- VD: "0123456789"
    -- VD: "NGUYEN VAN A" (in hoa, không dấu)
                                     ten_chu_tk NVARCHAR(100) NOT NULL,
                                     la_mac_dinh BIT NOT NULL DEFAULT 0,
                                     kich_hoat BIT NOT NULL DEFAULT 1,
                                     ghi_chu NVARCHAR(255) NULL
);
GO

-- FIX 3: chỉ cho phép tối đa 1 tài khoản là mặc định tại 1 thời điểm
CREATE UNIQUE INDEX uq_cauhinh_macdinh
    ON cau_hinh_thanh_toan (la_mac_dinh)
    WHERE la_mac_dinh = 1;
GO

CREATE TABLE don_hang (
                          id INT IDENTITY (1, 1) PRIMARY KEY,
                          ma_don_hang VARCHAR(50) NOT NULL,
                          khach_hang_id INT NOT NULL,
                          nguoi_xu_ly_id INT NULL,
                          cuoc_hoi_thoai_id INT NULL,
                          kenh_ban VARCHAR(10) NOT NULL DEFAULT 'online',
                          ngay_dat DATETIME2 NOT NULL DEFAULT GETDATE(),
    -- Snapshot địa chỉ giao tại thời điểm đặt hàng (Đã chuẩn hóa cho GHN)
                          dia_chi_giao_id INT NULL,
                          ho_ten_nguoi_nhan NVARCHAR(100),
                          sdt_nguoi_nhan VARCHAR(20),
                          email_nguoi_nhan VARCHAR(100) NULL,
                          dia_chi_giao_cu_the NVARCHAR(255),
                          phuong_xa_giao NVARCHAR(100),
                          quan_huyen_giao NVARCHAR(100),
                          tinh_thanh_giao NVARCHAR(100),
    -- Tài chính
                          tong_tien_hang DECIMAL(15, 2) NOT NULL,
                          tien_giam_gia DECIMAL(15, 2) NOT NULL DEFAULT 0,
                          phi_ship DECIMAL(15, 2) NOT NULL DEFAULT 0, -- Lấy từ API GHN
                          tong_thanh_toan AS (tong_tien_hang - tien_giam_gia + phi_ship) PERSISTED,
                          ma_giam_gia_id INT NULL,
    -- Vận chuyển (Đã xóa nguoi_giao_id và khoang_cach_km)
                          ngay_giao_du_kien DATE NULL, -- Lấy từ API GHN
                          ngay_giao_thuc_te DATETIME2 NULL,
    -- Trạng thái đơn hàng
                          trang_thai VARCHAR(20) NOT NULL DEFAULT 'cho_xac_nhan',
    -- Trạng thái thanh toán (Riêng cho luồng QR động / SePay)
                          trang_thai_thanh_toan VARCHAR(20) NOT NULL DEFAULT 'chua_thanh_toan',
                          thoi_gian_het_han_tt DATETIME2 NULL,
                          ghi_chu NVARCHAR(MAX),
                          updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT uq_dh_ma UNIQUE (ma_don_hang),
                          CONSTRAINT chk_dh_kenh CHECK (kenh_ban IN ('online', 'tai_quay')),
                          CONSTRAINT chk_dh_trang_thai CHECK (
                              trang_thai IN (
                                             'cho_xac_nhan', 'da_xac_nhan', 'dang_giao', 'da_giao', 'da_huy'
                                  )
                              ),
                          CONSTRAINT chk_dh_trang_thai_tt CHECK (trang_thai_thanh_toan IN (
                                                                                           'chua_thanh_toan',  -- mới tạo đơn
                                                                                           'dang_cho_qr',      -- đang hiển thị QR, chờ khách quét
                                                                                           'da_thanh_toan',    -- SePay webhook xác nhận OK
                                                                                           'that_bai',         -- quá hạn hoặc lỗi
                                                                                           'hoan_tien'         -- đã hoàn tiền
                              )),
    -- Chặn dữ liệu âm do lỗi nhập liệu/code
                          CONSTRAINT chk_dh_tong_tien_hang CHECK (tong_tien_hang >= 0),
                          CONSTRAINT chk_dh_tien_giam_gia CHECK (tien_giam_gia >= 0),
                          CONSTRAINT chk_dh_phi_ship CHECK (phi_ship >= 0),
                          CONSTRAINT fk_dh_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang (id),
                          CONSTRAINT fk_dh_nd FOREIGN KEY (nguoi_xu_ly_id) REFERENCES nguoi_dung (id),
                          CONSTRAINT fk_dh_dc FOREIGN KEY (dia_chi_giao_id) REFERENCES dia_chi_khach_hang (id) ON DELETE SET NULL,
);
GO

CREATE INDEX idx_dh_kh ON don_hang (khach_hang_id, ngay_dat);
CREATE INDEX idx_dh_tts ON don_hang (trang_thai, ngay_dat);
CREATE INDEX idx_dh_ngay ON don_hang (ngay_dat);
CREATE INDEX idx_dh_tttt ON don_hang (trang_thai_thanh_toan, thoi_gian_het_han_tt);
GO

ALTER TABLE may_dien_thoai
    ADD CONSTRAINT fk_may_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang(id) ON DELETE SET NULL;
GO

CREATE TABLE chi_tiet_don_hang (
                                   id INT IDENTITY (1, 1) PRIMARY KEY,
                                   don_hang_id INT NOT NULL,
                                   bien_the_san_pham_id INT NOT NULL,
                                   so_luong INT NOT NULL,
                                   don_gia_ban DECIMAL(15, 2) NOT NULL,
                                   thanh_tien AS (so_luong * don_gia_ban) PERSISTED,
                                   CONSTRAINT chk_ctdh_sl CHECK (so_luong > 0),
    -- FIX 5: chặn đơn giá âm
                                   CONSTRAINT chk_ctdh_dongia CHECK (don_gia_ban >= 0),
                                   CONSTRAINT fk_ctdh_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (
                                                                                                        id
                                       ) ON DELETE CASCADE,
                                   CONSTRAINT fk_ctdh_bt FOREIGN KEY (
                                                                      bien_the_san_pham_id
                                       ) REFERENCES bien_the_san_pham (id)
);
GO

-- FIX 4: index cho FK để join/lookup nhanh hơn (SQL Server không tự tạo)
CREATE INDEX idx_ctdh_dh ON chi_tiet_don_hang (don_hang_id);
GO

CREATE TABLE thanh_toan (
                            id INT IDENTITY (1, 1) PRIMARY KEY,
                            don_hang_id INT NOT NULL,
                            phuong_thuc_thanh_toan_id INT NOT NULL,
                            so_tien DECIMAL(15, 2) NOT NULL,
                            so_tien_thuc_te           DECIMAL(15, 2)  NULL,     -- Tiền KHÁCH ĐÃ CHUYỂN (Nhận từ SePay)
                            ma_giao_dich VARCHAR(100) NULL,
                            trang_thai VARCHAR(15) NOT NULL DEFAULT 'cho',
                            thoi_gian_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
                            thoi_gian_thanh_cong DATETIME2 NULL,
    -- Thông tin QR động (VietQR)
                            qr_code_url VARCHAR(500) NULL,   -- URL ảnh QR từ VietQR API
    -- Nội dung in sẵn trong QR: "THANHTOAN DH2024001"
                            noi_dung_chuyen_khoan VARCHAR(100) NULL,
                            thoi_gian_het_han DATETIME2 NULL,   -- QR hết hạn sau 15 phút
    -- Dữ liệu SePay webhook trả về
                            sepay_transaction_id BIGINT NULL,   -- ID giao dịch trên SePay
                            ten_ngan_hang_gui VARCHAR(50) NULL,   -- Ngân hàng khách dùng để quét
                            noi_dung_goc NVARCHAR(255) NULL,   -- Nội dung thực tế khách nhập
                            ma_tham_chieu VARCHAR(100) NULL,   -- referenceCode từ SePay
                            thoi_gian_ngan_hang DATETIME2 NULL,   -- Thời gian ngân hàng xử lý
                            raw_webhook NVARCHAR(MAX) NULL,   -- JSON gốc SePay gửi về (để debug)
                            CONSTRAINT chk_tt_trang_thai CHECK (
                                trang_thai IN ('cho', 'thanh_cong', 'that_bai', 'hoan_tien')
                                ),
    -- FIX 5: chặn số tiền âm
                            CONSTRAINT chk_tt_so_tien CHECK (so_tien >= 0),
                            CONSTRAINT chk_tt_so_tien_thuc_te CHECK (so_tien_thuc_te >= 0),
                            CONSTRAINT fk_tt_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (id),
                            CONSTRAINT fk_tt_pttt FOREIGN KEY (
                                                               phuong_thuc_thanh_toan_id
                                ) REFERENCES phuong_thuc_thanh_toan (id)
);
GO

CREATE INDEX idx_tt_dh ON thanh_toan (don_hang_id);
CREATE INDEX idx_tt_het_han
    ON thanh_toan (thoi_gian_het_han)
    WHERE thoi_gian_het_han IS NOT NULL;
GO

-- FIX 1: chống xử lý trùng giao dịch khi SePay gọi webhook nhiều lần
-- (thay cho idx_tt_sepay thường trước đây)
CREATE UNIQUE INDEX uq_tt_sepay_id
    ON thanh_toan (sepay_transaction_id)
    WHERE sepay_transaction_id IS NOT NULL;
GO

-- =====================================================
-- MODULE 8: BẢO HÀNH
-- Gộp phieu_gui_ncc_bao_hanh vào yeu_cau_bao_hanh
-- Bỏ lich_su_bao_hanh
-- =====================================================

CREATE TABLE phieu_bao_hanh (
                                id                   INT         IDENTITY(1,1) PRIMARY KEY,
                                ma_phieu             VARCHAR(50) NOT NULL,
                                may_dien_thoai_id    INT         NOT NULL,
                                khach_hang_id        INT         NOT NULL,
                                don_hang_id          INT         NOT NULL,
                                so_thang_bao_hanh    INT         NOT NULL,
                                ngay_bat_dau         DATE        NOT NULL,
                                ngay_het_han         DATE        NOT NULL,
                                trang_thai           VARCHAR(15) NOT NULL DEFAULT 'con_hieu_luc',
                                CONSTRAINT uq_pbh_ma          UNIQUE (ma_phieu),
                                CONSTRAINT uq_pbh_may         UNIQUE (may_dien_thoai_id),
                                CONSTRAINT chk_pbh_trang_thai CHECK (trang_thai IN ('con_hieu_luc','het_han','da_su_dung','void')),
                                CONSTRAINT fk_pbh_may         FOREIGN KEY (may_dien_thoai_id) REFERENCES may_dien_thoai(id),
                                CONSTRAINT fk_pbh_kh          FOREIGN KEY (khach_hang_id)     REFERENCES khach_hang(id),
                                CONSTRAINT fk_pbh_dh          FOREIGN KEY (don_hang_id)       REFERENCES don_hang(id)
);
GO

-- Gộp thông tin gửi NCC vào cùng bảng yêu cầu bảo hành
CREATE TABLE yeu_cau_bao_hanh (
                                  id                   INT           IDENTITY(1,1) PRIMARY KEY,
                                  ma_yeu_cau           VARCHAR(50)   NOT NULL,
                                  phieu_bao_hanh_id    INT           NOT NULL,
                                  nguoi_tiep_nhan_id   INT           NULL,
                                  ngay_tiep_nhan       DATETIME2     NOT NULL DEFAULT GETDATE(),
                                  mo_ta_loi            NVARCHAR(MAX),
                                  hinh_thuc            VARCHAR(15)   NOT NULL,
                                  trang_thai           VARCHAR(20)   NOT NULL DEFAULT 'tiep_nhan',
    -- Thông tin gửi NCC (gộp từ phieu_gui_ncc_bao_hanh)
                                  nha_cung_cap_id      INT           NULL,
                                  ngay_gui_ncc         DATETIME2     NULL,
                                  ngay_du_kien_nhan    DATE          NULL,
                                  ngay_nhan_lai_ncc    DATETIME2     NULL,
                                  ket_qua_ncc          NVARCHAR(MAX) NULL,
    -- Trả khách
                                  ngay_tra_khach       DATETIME2     NULL,
                                  ghi_chu              NVARCHAR(MAX),
                                  CONSTRAINT uq_ycbh_ma          UNIQUE (ma_yeu_cau),
                                  CONSTRAINT chk_ycbh_hinh_thuc  CHECK (hinh_thuc  IN ('sua_chua','doi_moi','hoan_tien')),
                                  CONSTRAINT chk_ycbh_trang_thai CHECK (trang_thai IN ('tiep_nhan','dang_kiem_tra','da_gui_ncc','ncc_dang_xu_ly','da_nhan_lai_ncc','cho_tra_khach','da_tra_khach','tu_choi')),
                                  CONSTRAINT fk_ycbh_pbh         FOREIGN KEY (phieu_bao_hanh_id)  REFERENCES phieu_bao_hanh(id),
                                  CONSTRAINT fk_ycbh_nd          FOREIGN KEY (nguoi_tiep_nhan_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL,
                                  CONSTRAINT fk_ycbh_ncc         FOREIGN KEY (nha_cung_cap_id)    REFERENCES nha_cung_cap(id)
);
GO

CREATE INDEX idx_ycbh_pbh ON yeu_cau_bao_hanh (phieu_bao_hanh_id);
GO

-- =====================================================
-- MODULE 9: KHUYẾN MÃI
-- Gộp flash_sale vào chuong_trinh_khuyen_mai
-- Giữ pham_vi, ma_giam_gia, chi_tiet_flash_sale
-- =====================================================

CREATE TABLE chuong_trinh_khuyen_mai (
                                         id                 INT           IDENTITY(1,1) PRIMARY KEY,
                                         ten_ctkm           NVARCHAR(200) NOT NULL,
                                         mo_ta              NVARCHAR(MAX),
                                         loai               VARCHAR(25)   NOT NULL,
                                         gia_tri_uu_dai     DECIMAL(15,2),
                                         la_phan_tram       BIT           NOT NULL DEFAULT 0,
                                         giam_toi_da        DECIMAL(15,2) NULL,
                                         ngay_bat_dau       DATETIME2     NOT NULL,
                                         ngay_ket_thuc      DATETIME2     NOT NULL,
    -- Flash sale: giờ cụ thể (NULL nếu không phải flash sale)
                                         gio_flash_bat_dau  DATETIME2     NULL,
                                         gio_flash_ket_thuc DATETIME2     NULL,
                                         so_luong_toi_da    INT           NULL,
                                         so_lan_da_dung     INT           NOT NULL DEFAULT 0,
                                         trang_thai         VARCHAR(15)   NOT NULL DEFAULT 'chua_bat_dau',
                                         CONSTRAINT chk_ctkm_loai       CHECK (loai      IN ('giam_gia_truc_tiep','phan_tram','ma_code','flash_sale','don_hang_toi_thieu')),
                                         CONSTRAINT chk_ctkm_trang_thai CHECK (trang_thai IN ('chua_bat_dau','dang_dien_ra','da_ket_thuc','tam_dung'))
);
GO

CREATE INDEX idx_ctkm_tts ON chuong_trinh_khuyen_mai (trang_thai, ngay_bat_dau, ngay_ket_thuc);
GO

CREATE TABLE pham_vi_khuyen_mai (
                                    id          INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
                                    ctkm_id     INT NOT NULL,
                                    san_pham_id INT NULL,
                                    danh_muc_id INT NULL,
                                    hang_sx_id  INT NULL,
                                    CONSTRAINT fk_pvkm_ctkm FOREIGN KEY (ctkm_id)     REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_pvkm_sp   FOREIGN KEY (san_pham_id) REFERENCES san_pham(id),
                                    CONSTRAINT fk_pvkm_dm   FOREIGN KEY (danh_muc_id) REFERENCES danh_muc(id),
                                    CONSTRAINT fk_pvkm_hsx  FOREIGN KEY (hang_sx_id)  REFERENCES hang_san_xuat(id)
);
GO

CREATE TABLE ma_giam_gia (
                             id                 INT           IDENTITY(1,1) PRIMARY KEY,
                             ctkm_id            INT           NOT NULL,
                             ma_code            VARCHAR(50)   NOT NULL,
                             don_hang_toi_thieu DECIMAL(15,2) NOT NULL DEFAULT 0,
                             giam_toi_da        DECIMAL(15,2) NULL,
                             so_luong_toi_da    INT           NOT NULL DEFAULT 1,
                             da_su_dung         INT           NOT NULL DEFAULT 0,
                             CONSTRAINT uq_mgg_code UNIQUE (ma_code),
                             CONSTRAINT fk_mgg_ctkm FOREIGN KEY (ctkm_id) REFERENCES chuong_trinh_khuyen_mai(id)
);
GO

CREATE INDEX idx_mgg_code ON ma_giam_gia (ma_code);
GO

-- Chi tiết giá flash sale cho từng biến thể
CREATE TABLE chi_tiet_flash_sale (
                                     id                   INT           IDENTITY(1,1) PRIMARY KEY,
                                     ctkm_id              INT           NOT NULL,
                                     bien_the_san_pham_id INT           NOT NULL,
                                     gia_flash            DECIMAL(15,2) NOT NULL,
                                     so_luong_gioi_han    INT           NOT NULL,
                                     da_ban               INT           NOT NULL DEFAULT 0,
                                     CONSTRAINT fk_ctfs_ctkm FOREIGN KEY (ctkm_id)              REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_ctfs_bt   FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

-- Gắn FK ma_giam_gia vào don_hang (thực hiện sau khi cả 2 bảng đã tồn tại)
ALTER TABLE don_hang
    ADD CONSTRAINT fk_dh_mgg FOREIGN KEY (ma_giam_gia_id) REFERENCES ma_giam_gia(id) ON DELETE SET NULL;
GO

-- =====================================================
-- MODULE 10: ĐIỂM TÍCH LŨY & HẠNG THÀNH VIÊN
-- Điểm hiện tại lưu trong khach_hang.diem_tich_luy
-- Điểm KHÔNG dùng để quy đổi/thanh toán — chỉ dùng để TÍCH LŨY hiển thị
-- và làm căn cứ xét THĂNG HẠNG.
--
-- Quy tắc tích điểm hardcode theo hạng thành viên:
--   dong:       hệ số 1.0   (100.000đ chi tiêu = 1 điểm cơ bản)
--   bac:        hệ số 1.2
--   vang:       hệ số 1.5
--   kim_cuong:  hệ số 2.0
--   Công thức: diem_cong = FLOOR(tong_thanh_toan / 100000) * he_so_hang
--
-- Quy tắc thăng hạng theo khach_hang.tong_chi_tieu (cộng dồn):
--   dong:       0đ          → giảm giá 0%   mọi đơn hàng
--   bac:        >= 50.000.000đ  → giảm giá 3%   mọi đơn hàng
--   vang:       >= 100.000.000đ → giảm giá 5%   mọi đơn hàng
--   kim_cuong:  >= 250.000.000đ → giảm giá 7%   mọi đơn hàng
--
-- Toàn bộ rule trên hardcode trong code backend (không thêm bảng/cột cấu hình)
-- =====================================================

CREATE TABLE lich_su_diem (
                              id            INT           IDENTITY(1,1) PRIMARY KEY,
                              khach_hang_id INT           NOT NULL,
                              loai          VARCHAR(15)   NOT NULL,
                              so_diem       INT           NOT NULL,
                              so_diem_truoc INT           NOT NULL,
                              so_diem_sau   INT           NOT NULL,
                              don_hang_id   INT           NULL,
                              ly_do         NVARCHAR(255),
                              ngay_het_han  DATE          NULL,
                              thoi_gian     DATETIME2     NOT NULL DEFAULT GETDATE(),
                              CONSTRAINT chk_lsd_loai CHECK (loai IN ('cong','tru','het_han','dieu_chinh')),
                              CONSTRAINT fk_lsd_kh    FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id),
                              CONSTRAINT fk_lsd_dh    FOREIGN KEY (don_hang_id)   REFERENCES don_hang(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_lsd_kh ON lich_su_diem (khach_hang_id, thoi_gian);
GO

-- =====================================================
-- MODULE 11: CHATBOT AI
-- Bỏ chatbot_cau_hoi_mau (hardcode phía frontend)
-- Giữ cuoc_hoi_thoai + tin_nhan_chat
-- =====================================================

CREATE TABLE cuoc_hoi_thoai (
                                id            INT           IDENTITY(1,1) PRIMARY KEY,
                                khach_hang_id INT           NULL,
                                session_id    VARCHAR(100)  NULL,
                                tieu_de       NVARCHAR(255) NULL,
                                ngay_tao      DATETIME2     NOT NULL DEFAULT GETDATE(),
                                updated_at    DATETIME2     NOT NULL DEFAULT GETDATE(),
                                CONSTRAINT fk_cht_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_cht_kh      ON cuoc_hoi_thoai (khach_hang_id, updated_at);
CREATE INDEX idx_cht_session ON cuoc_hoi_thoai (session_id);
GO

CREATE TABLE tin_nhan_chat (
                               id                INT           IDENTITY(1,1) PRIMARY KEY,
                               cuoc_hoi_thoai_id INT           NOT NULL,
                               vai               VARCHAR(10)   NOT NULL,
                               noi_dung          NVARCHAR(MAX) NOT NULL,
    -- Thêm mới: Hỗ trợ ảnh gửi kèm (Gemini Vision)
                               hinh_anh_url      VARCHAR(255)  NULL,
    -- Thêm mới: Quản lý Quota API Free
                               so_token          INT           NULL,
    -- Ngữ cảnh (Giữ nguyên)
                               intent            VARCHAR(30)   NULL,
                               don_hang_id_ref   INT           NULL,
                               san_pham_id_ref   INT           NULL,
                               thoi_gian         DATETIME2     NOT NULL DEFAULT GETDATE(),
    -- ĐÃ SỬA: Thay 'assistant' bằng 'model' chuẩn của Gemini
                               CONSTRAINT chk_tnc_vai    CHECK (vai    IN ('user','model','system')),
                               CONSTRAINT chk_tnc_intent CHECK (intent IN ('tu_van_sp','tra_cuu_dh','bao_hanh','khuyen_mai','chinh_sach','khac') OR intent IS NULL),
                               CONSTRAINT fk_tnc_cht     FOREIGN KEY (cuoc_hoi_thoai_id) REFERENCES cuoc_hoi_thoai(id) ON DELETE CASCADE,
                               CONSTRAINT fk_tnc_dh      FOREIGN KEY (don_hang_id_ref)   REFERENCES don_hang(id)       ON DELETE SET NULL,
                               CONSTRAINT fk_tnc_sp      FOREIGN KEY (san_pham_id_ref)   REFERENCES san_pham(id)       ON DELETE SET NULL
);
GO

CREATE INDEX idx_tnc_cht ON tin_nhan_chat (cuoc_hoi_thoai_id, thoi_gian);
GO

ALTER TABLE don_hang
    ADD CONSTRAINT fk_dh_cht FOREIGN KEY (cuoc_hoi_thoai_id) REFERENCES cuoc_hoi_thoai(id) ON DELETE SET NULL;
GO

-- =====================================================
-- MODULE 12: ĐÁNH GIÁ & HỎI ĐÁP
-- Gộp hinh_anh_danh_gia → lưu JSON trong danh_gia_san_pham
-- =====================================================

CREATE TABLE danh_gia_san_pham (
                                   id            INT           IDENTITY(1,1) PRIMARY KEY,
                                   san_pham_id   INT           NOT NULL,
                                   khach_hang_id INT           NOT NULL,
                                   don_hang_id   INT           NOT NULL,
                                   sao           INT           NOT NULL,
                                   tieu_de       NVARCHAR(200),
                                   noi_dung      NVARCHAR(MAX),
                                   hinh_anh_json NVARCHAR(MAX) NULL,
                                   trang_thai    VARCHAR(15)   NOT NULL DEFAULT 'cho_duyet',
                                   ngay_tao      DATETIME2     NOT NULL DEFAULT GETDATE(),
                                   CONSTRAINT chk_dg_sao        CHECK (sao BETWEEN 1 AND 5),
                                   CONSTRAINT chk_dg_trang_thai CHECK (trang_thai IN ('cho_duyet','da_duyet','an')),
                                   CONSTRAINT uq_dg             UNIQUE (khach_hang_id, don_hang_id, san_pham_id),
                                   CONSTRAINT fk_dg_sp          FOREIGN KEY (san_pham_id)   REFERENCES san_pham(id),
                                   CONSTRAINT fk_dg_kh          FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id),
                                   CONSTRAINT fk_dg_dh          FOREIGN KEY (don_hang_id)   REFERENCES don_hang(id)
);
GO

CREATE INDEX idx_dg_sp ON danh_gia_san_pham (san_pham_id, trang_thai);
GO

CREATE TABLE hoi_dap_san_pham (
                                  id               INT           IDENTITY(1,1) PRIMARY KEY,
                                  san_pham_id      INT           NOT NULL,
                                  khach_hang_id    INT           NOT NULL,
                                  cau_hoi          NVARCHAR(MAX) NOT NULL,
                                  tra_loi          NVARCHAR(MAX),
                                  nguoi_tra_loi_id INT           NULL,
                                  ngay_hoi         DATETIME2     NOT NULL DEFAULT GETDATE(),
                                  ngay_tra_loi     DATETIME2     NULL,
                                  hien_thi         BIT           NOT NULL DEFAULT 1,
                                  CONSTRAINT fk_hdsp_sp FOREIGN KEY (san_pham_id)      REFERENCES san_pham(id),
                                  CONSTRAINT fk_hdsp_kh FOREIGN KEY (khach_hang_id)    REFERENCES khach_hang(id),
                                  CONSTRAINT fk_hdsp_nd FOREIGN KEY (nguoi_tra_loi_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_hdsp_sp ON hoi_dap_san_pham (san_pham_id, hien_thi);
GO

-- =====================================================
-- MODULE 13: WISHLIST
-- =====================================================

CREATE TABLE yeu_thich (
                           id            INT       IDENTITY(1,1) PRIMARY KEY,
                           khach_hang_id INT       NOT NULL,
                           san_pham_id   INT       NOT NULL,
                           ngay_them     DATETIME2 NOT NULL DEFAULT GETDATE(),
                           CONSTRAINT uq_yt    UNIQUE (khach_hang_id, san_pham_id),
                           CONSTRAINT fk_yt_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE CASCADE,
                           CONSTRAINT fk_yt_sp FOREIGN KEY (san_pham_id)   REFERENCES san_pham(id)
);
GO

-- =====================================================
-- TRIGGERS
-- =====================================================

CREATE TRIGGER trg_nd_updated ON nguoi_dung AFTER UPDATE
                                                      AS BEGIN
UPDATE nguoi_dung SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_kh_updated ON khach_hang AFTER UPDATE
                                                      AS BEGIN
UPDATE khach_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_sp_updated ON san_pham AFTER UPDATE
                                                    AS BEGIN
UPDATE san_pham SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_bt_updated ON bien_the_san_pham AFTER UPDATE
                                                             AS BEGIN
UPDATE bien_the_san_pham SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_dh_updated ON don_hang AFTER UPDATE
                                                    AS BEGIN
UPDATE don_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_tk_updated ON ton_kho AFTER UPDATE
                                                   AS BEGIN
UPDATE ton_kho SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_gh_updated ON gio_hang AFTER UPDATE
                                                    AS BEGIN
UPDATE gio_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_cht_updated ON cuoc_hoi_thoai AFTER UPDATE
                                                           AS BEGIN
UPDATE cuoc_hoi_thoai SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

-- =====================================================
-- TỔNG KẾT
-- =====================================================
-- Tổng số bảng: 35 bảng
--
--  MODULE 1  - Người dùng & Phân quyền (2):
--    01. nguoi_dung
--    02. khach_hang
--  MODULE 2  - Sản phẩm & Biến thể (7):
--    03. danh_muc
--    04. hang_san_xuat
--    05. san_pham
--    06. bien_the_san_pham
--    07. may_dien_thoai
--    08. hinh_anh_san_pham
--    09. thong_so_ky_thuat
--  MODULE 3  - Kho hàng (6):
--    10. kho
--    11. ton_kho
--    12. phieu_nhap_kho
--    13. chi_tiet_phieu_nhap
--    14. phieu_chuyen_kho
--    15. chi_tiet_chuyen_kho
--  MODULE 4  - Nhà cung cấp (1):
--    16. nha_cung_cap
--  MODULE 5  - Địa chỉ (1):
--    17. dia_chi_khach_hang
--  MODULE 6  - Giỏ hàng (2):
--    18. gio_hang
--    19. chi_tiet_gio_hang
--  MODULE 7  - Đơn hàng (4):
--    20. phuong_thuc_thanh_toan
--    21. cau_hinh_thanh_toan
--    22. don_hang
--    23. chi_tiet_don_hang
--    24. thanh_toan
--  MODULE 8  - Bảo hành (2):
--    25. phieu_bao_hanh
--    26. yeu_cau_bao_hanh
--  MODULE 9  - Khuyến mãi (4):
--    27. chuong_trinh_khuyen_mai
--    28. pham_vi_khuyen_mai
--    29. ma_giam_gia
--    30. chi_tiet_flash_sale
--  MODULE 10 - Điểm tích lũy (1):
--    31. lich_su_diem
--  MODULE 11 - Chatbot AI (2):
--    32. cuoc_hoi_thoai
--    33. tin_nhan_chat
--  MODULE 12 - Đánh giá & Hỏi đáp (2):
--    34. danh_gia_san_pham
--    35. hoi_dap_san_pham
--  MODULE 13 - Wishlist (1):
--    36. yeu_thich
-- =====================================================


-- =====================================================
-- Dữ Liệu Mẫu: không cố định có thể sẽ thay đổi sau
-- =====================================================
USE PrimeMobile;
GO

-- =====================================================
-- 2. DỮ LIỆU GỐC: DANH MỤC & HÃNG SẢN XUẤT
-- =====================================================
INSERT INTO danh_muc (ten_danh_muc, slug, thu_tu) VALUES
(N'Điện thoại cao cấp', 'dien-thoai-cao-cap', 1),
(N'Điện thoại tầm trung', 'dien-thoai-tam-trung', 2),
(N'Điện thoại gập', 'dien-thoai-gap', 3),
(N'Điện thoại giá rẻ', 'dien-thoai-gia-re', 4);
GO

INSERT INTO hang_san_xuat (ten_hang, logo, quoc_gia) VALUES
('Apple', 'https://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg', N'Mỹ'),
('Samsung', 'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg', N'Hàn Quốc'),
('Xiaomi', 'https://upload.wikimedia.org/wikipedia/commons/2/29/Xiaomi_logo.svg', N'Trung Quốc'),
('OPPO', 'https://upload.wikimedia.org/wikipedia/commons/c/ca/OPPO_LOGO_2019.svg', N'Trung Quốc');
GO

-- =====================================================
-- 3. CẤU HÌNH KHO & THANH TOÁN
-- =====================================================
INSERT INTO kho (ten_kho, loai, dia_chi, kich_hoat) VALUES
(N'Kho Tổng Miền Bắc', 'kho_tong', N'123 Cầu Giấy, Hà Nội', 1),
(N'Kho Chuyển Phát Online', 'kho_online', N'456 Lê Lợi, TP.HCM', 1);
GO

INSERT INTO phuong_thuc_thanh_toan (ten_pttt, mo_ta, kich_hoat) VALUES
(N'Tiền mặt (COD)', N'Thanh toán khi nhận hàng', 1),
(N'SePay (VietQR)', N'Chuyển khoản quét mã QR tự động', 1);
GO

INSERT INTO cau_hinh_thanh_toan (ten_ngan_hang, bank_id, so_tai_khoan, ten_chu_tk, la_mac_dinh) VALUES
(N'MB Bank', 'MB', '190088889999', 'NGUYEN VAN ADMIN', 1);
GO

INSERT INTO nha_cung_cap (ma_ncc, ten_ncc, so_dien_thoai, dia_chi, nguoi_lien_he, trang_thai) VALUES
('NCC_FPT', N'FPT Synnex', '19006600', N'Cầu Giấy, Hà Nội', N'Mr. Hùng', 'dang_hop_tac'),
('NCC_DGW', N'Digiworld', '19001234', N'Quận 1, TP.HCM', N'Ms. Lan', 'dang_hop_tac');
GO

-- =====================================================
-- 4. SẢN PHẨM & THÔNG SỐ (8 SẢN PHẨM)
-- =====================================================
INSERT INTO san_pham (ma_san_pham, ten_san_pham, danh_muc_id, hang_san_xuat_id, mo_ta_ngan, nam_ra_mat, bao_hanh_thang) VALUES
('SP_IP15PM', N'iPhone 15 Pro Max', 1, 1, N'Khung Titanium siêu nhẹ, Camera 5x zoom quang học.', 2023, 12),
('SP_IP14', N'iPhone 14', 1, 1, N'Màn hình Super Retina XDR, Pin bền bỉ.', 2022, 12),
('SP_S24U', N'Samsung Galaxy S24 Ultra', 1, 2, N'Quyền năng Galaxy AI, bút S-Pen thông minh.', 2024, 12),
('SP_ZFOLD5', N'Samsung Galaxy Z Fold5', 3, 2, N'Bản lề Flex mượt mà, màn hình gập đỉnh cao.', 2023, 12),
('SP_XM14', N'Xiaomi 14', 1, 3, N'Ống kính Leica, cấu hình mạnh mẽ.', 2024, 18),
('SP_RMN13', N'Redmi Note 13 Pro', 2, 3, N'Camera 200MP, sạc siêu tốc 67W.', 2023, 18),
('SP_RENO11', N'OPPO Reno11 5G', 2, 4, N'Chuyên gia chân dung, thiết kế viền cong.', 2024, 12),
('SP_A18', N'OPPO A18', 4, 4, N'Màn hình lớn 90Hz, pin 5000mAh.', 2023, 12);
GO

-- =====================================================
-- 5. BIẾN THỂ SẢN PHẨM (12 BIẾN THỂ)
-- =====================================================
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, barcode, mau_sac, ma_mau_hex, ram_gb, luu_tru_gb, gia_nhap, gia_ban, gia_khuyen_mai, trang_thai) VALUES
-- 1. iPhone 15 Pro Max
(1, 'IP15PM-256-NAT', '88011', N'Titan Tự Nhiên', '#B5B6B1', 8, 256, 27000000, 29990000, 28990000, 'con_hang'),
(1, 'IP15PM-512-BLK', '88012', N'Titan Đen', '#4B4B4D', 8, 512, 32000000, 35990000, NULL, 'con_hang'),
-- 2. iPhone 14
(2, 'IP14-128-BLU', '88013', N'Xanh Dương', '#A3C6D3', 6, 128, 16000000, 18490000, 17990000, 'con_hang'),
-- 3. S24 Ultra
(3, 'S24U-256-GRY', '88021', N'Xám Titan', '#7D7A7D', 12, 256, 24000000, 26990000, 25490000, 'con_hang'),
(3, 'S24U-512-YEL', '88022', N'Vàng Titan', '#E6DEB8', 12, 512, 28000000, 31490000, NULL, 'con_hang'),
-- 4. Z Fold 5
(4, 'ZF5-256-BLU', '88023', N'Xanh Icy', '#A9BCD0', 12, 256, 30000000, 34990000, NULL, 'con_hang'),
-- 5. Xiaomi 14
(5, 'XM14-256-BLK', '88031', N'Đen', '#000000', 12, 256, 18000000, 20990000, 19990000, 'con_hang'),
-- 6. Redmi Note 13 Pro (Cố tình để Hết Hàng để test UI)
(6, 'RMN13-128-PUR', '88032', N'Tím', '#9D84B5', 8, 128, 6000000, 7490000, NULL, 'het_hang'),
-- 7. OPPO Reno11
(7, 'R11-256-GRN', '88041', N'Xanh Sóng Biển', '#7BA89D', 8, 256, 9000000, 10990000, 10490000, 'con_hang'),
-- 8. OPPO A18
(8, 'A18-128-BLU', '88042', N'Xanh Phát Sáng', '#87CEEB', 4, 128, 3000000, 3990000, 3690000, 'con_hang');
GO

-- =====================================================
-- 6. HÌNH ẢNH SẢN PHẨM & TỒN KHO
-- =====================================================
-- Insert Ảnh (Dùng link chuẩn để hiển thị UI mượt)
INSERT INTO hinh_anh_san_pham (bien_the_san_pham_id, duong_dan, la_anh_chinh, thu_tu) VALUES
(1, 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-blue-thumbnew-600x600.jpg', 1, 1),
(2, 'https://cdn.tgdd.vn/Products/Images/42/305658/iphone-15-pro-max-black-thumbnew-600x600.jpg', 1, 1),
(3, 'https://cdn.tgdd.vn/Products/Images/42/289663/iphone-14-blue-thumb-600x600.jpg', 1, 1),
(4, 'https://cdn.tgdd.vn/Products/Images/42/319665/samsung-galaxy-s24-ultra-grey-thumbnew-600x600.jpg', 1, 1),
(5, 'https://cdn.tgdd.vn/Products/Images/42/319665/samsung-galaxy-s24-ultra-yellow-thumbnew-600x600.jpg', 1, 1),
(6, 'https://cdn.tgdd.vn/Products/Images/42/301608/samsung-galaxy-z-fold5-blue-thumbnew-600x600.jpg', 1, 1),
(7, 'https://cdn.tgdd.vn/Products/Images/42/319741/xiaomi-14-black-thumb-600x600.jpg', 1, 1),
(8, 'https://cdn.tgdd.vn/Products/Images/42/322055/xiaomi-redmi-note-13-pro-4g-tim-thumb-600x600.jpg', 1, 1),
(9, 'https://cdn.tgdd.vn/Products/Images/42/319985/oppo-reno11-xanh-thumb-600x600.jpg', 1, 1),
(10, 'https://cdn.tgdd.vn/Products/Images/42/313334/oppo-a18-xanh-thumb-600x600.jpg', 1, 1);
GO

-- Insert Tồn Kho (Biến thể số 8 Redmi cố tình để 0)
INSERT INTO ton_kho (kho_id, bien_the_san_pham_id, so_luong) VALUES
(1, 1, 50), (2, 1, 15),
(1, 2, 20), (2, 2, 5),
(1, 3, 30), (2, 3, 10),
(1, 4, 40), (2, 4, 20),
(1, 5, 15), (2, 5, 5),
(1, 6, 10), (2, 6, 2),
(1, 7, 25), (2, 7, 10),
(1, 8, 0),  (2, 8, 0), -- Hết hàng
(1, 9, 30), (2, 9, 10),
(1, 10, 40), (2, 10, 15);
GO

-- =====================================================
-- 7. KHUYẾN MÃI & MÃ GIẢM GIÁ
-- =====================================================
INSERT INTO chuong_trinh_khuyen_mai (ten_ctkm, loai, gia_tri_uu_dai, la_phan_tram, ngay_bat_dau, ngay_ket_thuc, trang_thai) VALUES
(N'Siêu Sale Sinh Nhật', 'ma_code', 500000, 0, '2024-01-01', '2026-12-31', 'dang_dien_ra'),
(N'Giảm giá Tân Sinh Viên', 'ma_code', 10, 1, '2024-01-01', '2026-12-31', 'dang_dien_ra');
GO

INSERT INTO ma_giam_gia (ctkm_id, ma_code, don_hang_toi_thieu, giam_toi_da, so_luong_toi_da) VALUES
(1, 'BIRTHDAY500', 15000000, 500000, 100),
(2, 'STUDENT10', 5000000, 1000000, 50);
GO

-- =====================================================
-- 8. TÀI KHOẢN, KHÁCH HÀNG & ĐỊA CHỈ
-- =====================================================
INSERT INTO nguoi_dung (email, mat_khau, ho_ten, so_dien_thoai, vai_tro, trang_thai) VALUES
('admin@primemobile.vn', 'hash_admin', N'Lê Quản Trị', '0901111111', 'Admin', 'hoat_dong'),
('nhanvien@primemobile.vn', 'hash_nv', N'Trần Bán Hàng', '0902222222', 'NhanVien', 'hoat_dong'),
('khach1@gmail.com', 'hash_kh1', N'Nguyễn Văn An', '0912345678', 'KhachHang', 'hoat_dong'),
('khach2@gmail.com', 'hash_kh2', N'Phạm Thị Hoa', '0987654321', 'KhachHang', 'hoat_dong');
GO

INSERT INTO khach_hang (nguoi_dung_id, ho_ten, email, so_dien_thoai, diem_tich_luy, hang_thanh_vien, tong_chi_tieu) VALUES
(3, N'Nguyễn Văn An', 'khach1@gmail.com', '0912345678', 550, 'bac', 55000000), -- Khách VIP Hạng Bạc
(4, N'Phạm Thị Hoa', 'khach2@gmail.com', '0987654321', 0, 'dong', 0),
(NULL, N'Khách Lẻ Mặc Định', NULL, '0000000000', 0, 'dong', 0);
GO

INSERT INTO dia_chi_khach_hang (khach_hang_id, loai_dia_chi, ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, dia_chi_chi_tiet, tinh_thanh_id, quan_huyen_id, phuong_xa_code, tinh_thanh_ten, quan_huyen_ten, phuong_xa_ten, mac_dinh) VALUES
(1, 'nha_rieng', N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', 201, 1482, '1A03', N'Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng', 1),
(2, 'co_quan', N'Phạm Thị Hoa', '0987654321', N'Tòa nhà X, Đường Y', 202, 1442, '1B04', N'TP.HCM', N'Quận 1', N'Phường Bến Nghé', 1);
GO

-- =====================================================
-- 9. IMEI (MÁY VẬT LÝ) 
-- =====================================================
INSERT INTO may_dien_thoai (bien_the_san_pham_id, imei1, tinh_trang) VALUES
(1, '351111111111111', 'da_ban'), -- Đã bán trong Đơn hàng 1
(4, '352222222222222', 'da_ban'), -- Đã bán trong Đơn hàng 2
(7, '353333333333333', 'trong_kho'),
(9, '354444444444444', 'trong_kho');
GO

-- =====================================================
-- 10. ĐƠN HÀNG (ĐA TRẠNG THÁI CHO DEMO ADMIN DASHBOARD)
-- =====================================================
-- Đơn hàng 1: ĐÃ GIAO - Thanh toán SePay thành công (Khách 1 mua IP 15PM)
INSERT INTO don_hang (ma_don_hang, khach_hang_id, nguoi_xu_ly_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, ma_giam_gia_id, ngay_giao_du_kien, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH_2026_0001', 1, 2, 'online', 1, N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', N'Phường Dịch Vọng', N'Quận Cầu Giấy', N'Hà Nội', 28990000, 500000, 30000, 1, '2026-06-25', 'da_giao', 'da_thanh_toan', '2026-05-10');

-- Đơn hàng 2: ĐANG GIAO - Thanh toán COD (Khách 2 mua S24 Ultra)
INSERT INTO don_hang (ma_don_hang, khach_hang_id, nguoi_xu_ly_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, ngay_giao_du_kien, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH_2026_0002', 2, 2, 'online', 2, N'Phạm Thị Hoa', '0987654321', N'Tòa nhà X, Đường Y', N'Phường Bến Nghé', N'Quận 1', N'TP.HCM', 25490000, 0, 45000, '2026-06-22', 'dang_giao', 'chua_thanh_toan', GETDATE());

-- Đơn hàng 3: CHỜ XÁC NHẬN - Đang đợi quét mã QR SePay (Lazy Check)
INSERT INTO don_hang (ma_don_hang, khach_hang_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, trang_thai, trang_thai_thanh_toan, thoi_gian_het_han_tt, ngay_dat) VALUES
('DH_2026_0003', 1, 'online', 1, N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', N'Phường Dịch Vọng', N'Quận Cầu Giấy', N'Hà Nội', 19990000, 0, 25000, 'cho_xac_nhan', 'dang_cho_qr', DATEADD(MINUTE, 15, GETDATE()), GETDATE());

-- Đơn hàng 4: ĐÃ HỦY - Khách đổi ý
INSERT INTO don_hang (ma_don_hang, khach_hang_id, kenh_ban, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH_2026_0004', 3, 'online', N'Khách Lẻ Mặc Định', '0000000000', N'Tạm vắng', N'Phường X', N'Quận Y', N'Tỉnh Z', 10490000, 0, 30000, 'da_huy', 'that_bai', '2026-06-01');
GO

-- Chi tiết đơn hàng
INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_san_pham_id, so_luong, don_gia_ban) VALUES
(1, 1, 1, 28990000), -- Đơn 1: IP15PM
(2, 4, 1, 25490000), -- Đơn 2: S24 Ultra
(3, 7, 1, 19990000), -- Đơn 3: Xiaomi 14
(4, 9, 1, 10490000); -- Đơn 4: OPPO Reno11
GO

-- Bảng Thanh Toán
INSERT INTO thanh_toan (don_hang_id, phuong_thuc_thanh_toan_id, so_tien, so_tien_thuc_te, trang_thai, sepay_transaction_id) VALUES
(1, 2, 28520000, 28520000, 'thanh_cong', 111222333), -- Đơn 1 đã bank
(2, 1, 25535000, NULL, 'cho', NULL),                 -- Đơn 2 chờ COD
(3, 2, 20015000, NULL, 'cho', NULL);                 -- Đơn 3 chờ quét QR
GO

-- Update máy vật lý gán vào đơn 1 và đơn 2
UPDATE may_dien_thoai SET don_hang_id = 1 WHERE imei1 = '351111111111111';
UPDATE may_dien_thoai SET don_hang_id = 2 WHERE imei1 = '352222222222222';
GO

-- =====================================================
-- 11. ĐÁNH GIÁ, BẢO HÀNH & WISHLIST (Mồi UI)
-- =====================================================
-- Đánh giá (Khách 1 đã nhận Đơn 1 mới được đánh giá)
INSERT INTO danh_gia_san_pham (san_pham_id, khach_hang_id, don_hang_id, sao, tieu_de, noi_dung, trang_thai) VALUES
(1, 1, 1, 5, N'Điện thoại siêu xịn', N'Cầm rất nhẹ tay do khung titan, màu Tự Nhiên cực sang, ship siêu nhanh!', 'da_duyet');
GO

-- Phiếu bảo hành (Sinh tự động khi Đơn 1 giao thành công)
INSERT INTO phieu_bao_hanh (ma_phieu, may_dien_thoai_id, khach_hang_id, don_hang_id, so_thang_bao_hanh, ngay_bat_dau, ngay_het_han) VALUES
('BH_IP15_001', 1, 1, 1, 12, '2026-05-12', '2027-05-12');
GO

-- Yêu thích (Wishlist)
INSERT INTO yeu_thich (khach_hang_id, san_pham_id) VALUES
(1, 3), (1, 4), (2, 1);
GO

-- Hỏi đáp
INSERT INTO hoi_dap_san_pham (san_pham_id, khach_hang_id, cau_hoi, tra_loi, nguoi_tra_loi_id) VALUES
(1, 2, N'Máy này dùng sạc 20W hay 30W vậy shop?', N'Dạ chào bạn, iPhone 15 Pro Max hỗ trợ sạc nhanh tối đa lên đến 27W ạ.', 1);
GO

USE PrimeMobile;
GO

-- 1. Bơm lại 4 máy điện thoại (Đã điền đủ imei1, imei2 và serial để không bị dính chưởng UNIQUE NULL)
INSERT INTO may_dien_thoai (bien_the_san_pham_id, imei1, imei2, serial, tinh_trang, don_hang_id) VALUES
(1, '351111111111111', '861111111111111', 'SN_IP15_001', 'da_ban', 1), 
(4, '352222222222222', '862222222222222', 'SN_S24U_002', 'da_ban', 2), 
(7, '353333333333333', '863333333333333', 'SN_XM14_003', 'trong_kho', NULL),
(9, '354444444444444', '864444444444444', 'SN_RENO_004', 'trong_kho', NULL);
GO

USE PrimeMobile;
GO

-- 1. Khai báo biến để đi tìm ID thực tế của chiếc máy iPhone 15 Pro Max
DECLARE @ActualMayId INT;
SELECT @ActualMayId = id FROM may_dien_thoai WHERE imei1 = '351111111111111';

-- 2. Tạo Phiếu bảo hành bằng chính ID vừa tìm được
INSERT INTO phieu_bao_hanh (ma_phieu, may_dien_thoai_id, khach_hang_id, don_hang_id, so_thang_bao_hanh, ngay_bat_dau, ngay_het_han) 
VALUES ('BH_IP15_001', @ActualMayId, 1, 1, 12, '2026-05-12', '2027-05-12');
GO