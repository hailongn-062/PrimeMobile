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
-- Đã xóa bỏ các cột diem_tich_luy, hang_thanh_vien, tong_chi_tieu
CREATE TABLE khach_hang (
                            id               INT           IDENTITY(1,1) PRIMARY KEY,
                            nguoi_dung_id    INT           NULL,
                            ho_ten           NVARCHAR(100) NOT NULL,
                            email            VARCHAR(100),
                            so_dien_thoai    VARCHAR(20)   NOT NULL,
                            gioi_tinh        VARCHAR(5)    NULL,
                            ngay_sinh        DATE          NULL,
                            ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
                            updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
                            CONSTRAINT uq_kh_nguoi_dung  UNIQUE (nguoi_dung_id),
                            CONSTRAINT chk_kh_gioi_tinh  CHECK (gioi_tinh IN ('Nam','Nu','Khac')),
                            CONSTRAINT fk_kh_nd          FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_kh_sdt   ON khach_hang (so_dien_thoai);
CREATE INDEX idx_kh_email ON khach_hang (email);
GO

-- =====================================================
-- MODULE 2: SẢN PHẨM & BIẾN THỂ
-- Chỉ bán duy nhất điện thoại không bán phụ kiến và những thứ khác
-- Bỏ bảng sku (gộp ma_sku vào bien_the_san_pham), Đã xóa barcode
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
                          CONSTRAINT uq_sp_ma         UNIQUE (ma_san_pham),
                          CONSTRAINT chk_sp_trang_thai CHECK (trang_thai IN ('dang_ban','ngung_ban','sap_ra_mat')),
                          CONSTRAINT fk_sp_dm          FOREIGN KEY (danh_muc_id)      REFERENCES danh_muc(id),
                          CONSTRAINT fk_sp_hsx         FOREIGN KEY (hang_san_xuat_id) REFERENCES hang_san_xuat(id)
);
GO

CREATE INDEX idx_sp_ten  ON san_pham (ten_san_pham);
CREATE INDEX idx_sp_dm   ON san_pham (danh_muc_id, trang_thai);
CREATE INDEX idx_sp_hang ON san_pham (hang_san_xuat_id, trang_thai);
GO

-- bien_the_san_pham: 1 biến thể = 1 SKU
CREATE TABLE bien_the_san_pham (
                                   id               INT           IDENTITY(1,1) PRIMARY KEY,
                                   san_pham_id      INT           NOT NULL,
                                   ma_sku           VARCHAR(100)  NOT NULL,
                                   mau_sac          NVARCHAR(50)  NOT NULL,
                                   ma_mau_hex       VARCHAR(7),
                                   ram_gb           INT           NOT NULL,
                                   luu_tru_gb       INT           NOT NULL,
                                   loai_luu_tru     VARCHAR(20)   NOT NULL DEFAULT 'UFS',
                                   gia_nhap         DECIMAL(15,2) NOT NULL,
                                   gia_ban          DECIMAL(15,2) NOT NULL,
                                   trong_luong_gram INT,
                                   pin_mAh          INT,
                                   trang_thai       VARCHAR(20)   NOT NULL DEFAULT 'con_hang',
                                   ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
                                   updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
                                   CONSTRAINT uq_bt_ma_sku      UNIQUE (ma_sku),
                                   CONSTRAINT chk_bt_trang_thai CHECK (trang_thai IN ('con_hang','het_hang','ngung_kinh_doanh')),
                                   CONSTRAINT fk_bt_sp          FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_bt_sp      ON bien_the_san_pham (san_pham_id, trang_thai);
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
-- =====================================================

CREATE TABLE dia_chi_khach_hang (
                                    id                       INT           IDENTITY(1,1) PRIMARY KEY,
                                    khach_hang_id            INT           NOT NULL,
                                    loai_dia_chi             VARCHAR(15)   NOT NULL DEFAULT 'nha_rieng',
                                    ho_ten_nguoi_nhan        NVARCHAR(100),
                                    so_dien_thoai_nguoi_nhan VARCHAR(20),
                                    dia_chi_chi_tiet         NVARCHAR(255) NOT NULL,
                                    tinh_thanh_id            INT           NOT NULL,
                                    quan_huyen_id            INT           NOT NULL,
                                    phuong_xa_code           VARCHAR(20)   NOT NULL,
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
-- =====================================================

CREATE TABLE gio_hang (
                          id            INT          IDENTITY(1,1) PRIMARY KEY,
                          khach_hang_id INT          NULL,
                          session_id    VARCHAR(100) NULL,
                          ngay_tao      DATETIME2    NOT NULL DEFAULT GETDATE(),
                          updated_at    DATETIME2    NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT chk_gh_dinh_danh CHECK (khach_hang_id IS NOT NULL OR session_id IS NOT NULL),
                          CONSTRAINT fk_gh_kh         FOREIGN KEY (khach_hang_id) REFERENCES khach_hang(id) ON DELETE CASCADE
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
-- =====================================================

CREATE TABLE phuong_thuc_thanh_toan (
    id INT IDENTITY (1, 1) PRIMARY KEY,
    ten_pttt NVARCHAR(50) NOT NULL,
    mo_ta NVARCHAR(255) NULL,
    kich_hoat BIT NOT NULL DEFAULT 1,
    CONSTRAINT uq_pttt_ten UNIQUE (ten_pttt)
);
GO

INSERT INTO phuong_thuc_thanh_toan (ten_pttt, mo_ta)
VALUES
    (N'Tien mat', N'Thanh toan tien mat tai quay'),
    (N'Chuyen khoan', N'Chuyen khoan qua QR tinh tai quay, nhan vien xac nhan'),
    (N'VNPay', N'Thanh toan truc tuyen qua cong VNPay, khach redirect sang VNPay');
GO

CREATE TABLE don_hang (
                          id INT IDENTITY (1, 1) PRIMARY KEY,
                          ma_don_hang VARCHAR(50) NOT NULL,
                          khach_hang_id INT NOT NULL,
                          nguoi_xu_ly_id INT NULL,
                          -- Đã xóa cuoc_hoi_thoai_id
                          kenh_ban VARCHAR(10) NOT NULL DEFAULT 'online',
                          ngay_dat DATETIME2 NOT NULL DEFAULT GETDATE(),
                          dia_chi_giao_id INT NULL,
                          ho_ten_nguoi_nhan NVARCHAR(100) NULL,
                          sdt_nguoi_nhan VARCHAR(20) NULL,
                          email_nguoi_nhan VARCHAR(100) NULL,
                          dia_chi_giao_cu_the NVARCHAR(255) NULL,
                          phuong_xa_giao NVARCHAR(100) NULL,
                          quan_huyen_giao NVARCHAR(100) NULL,
                          tinh_thanh_giao NVARCHAR(100) NULL,
                          tong_tien_hang DECIMAL(15, 2) NOT NULL,
                          tien_giam_gia DECIMAL(15, 2) NOT NULL DEFAULT 0,
                          phi_ship DECIMAL(15, 2) NOT NULL DEFAULT 0,
                          tong_thanh_toan AS (tong_tien_hang - tien_giam_gia + phi_ship) PERSISTED,
                          chuong_trinh_khuyen_mai_id INT NULL, -- Thay thế ma_giam_gia_id
                          ngay_giao_du_kien DATE NULL,
                          ngay_giao_thuc_te DATETIME2 NULL,
                          trang_thai VARCHAR(20) NOT NULL DEFAULT 'cho_xac_nhan',
                          trang_thai_thanh_toan VARCHAR(20) NOT NULL DEFAULT 'chua_thanh_toan',
                          thoi_gian_het_han_tt DATETIME2 NULL,
                          ghi_chu NVARCHAR(MAX) NULL,
                          updated_at DATETIME2 NOT NULL DEFAULT GETDATE(),
                          CONSTRAINT uq_dh_ma UNIQUE (ma_don_hang),
                          CONSTRAINT chk_dh_kenh CHECK (kenh_ban IN ('online', 'tai_quay')),
                          CONSTRAINT chk_dh_trang_thai CHECK (trang_thai IN ('cho_xac_nhan', 'da_xac_nhan', 'dang_giao', 'da_giao', 'da_huy')),
                          CONSTRAINT chk_dh_trang_thai_tt CHECK (trang_thai_thanh_toan IN ('chua_thanh_toan', 'dang_chuyen_huong', 'da_thanh_toan', 'that_bai')),
                          CONSTRAINT chk_dh_tong_tien_hang CHECK (tong_tien_hang >= 0),
                          CONSTRAINT chk_dh_tien_giam_gia CHECK (tien_giam_gia >= 0),
                          CONSTRAINT chk_dh_phi_ship CHECK (phi_ship >= 0),
                          CONSTRAINT fk_dh_kh FOREIGN KEY (khach_hang_id) REFERENCES khach_hang (id),
                          CONSTRAINT fk_dh_nd FOREIGN KEY (nguoi_xu_ly_id) REFERENCES nguoi_dung (id),
                          CONSTRAINT fk_dh_dc FOREIGN KEY (dia_chi_giao_id) REFERENCES dia_chi_khach_hang (id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_dh_kh   ON don_hang (khach_hang_id, ngay_dat);
CREATE INDEX idx_dh_tts  ON don_hang (trang_thai, ngay_dat);
CREATE INDEX idx_dh_ngay ON don_hang (ngay_dat);
CREATE INDEX idx_dh_tttt ON don_hang (trang_thai_thanh_toan, thoi_gian_het_han_tt);
GO

ALTER TABLE may_dien_thoai
    ADD CONSTRAINT fk_may_dh FOREIGN KEY (don_hang_id)
        REFERENCES don_hang (id) ON DELETE SET NULL;
GO

CREATE TABLE chi_tiet_don_hang (
                                   id INT IDENTITY (1, 1) PRIMARY KEY,
                                   don_hang_id INT NOT NULL,
                                   bien_the_san_pham_id INT NOT NULL,
                                   so_luong INT NOT NULL,
                                   don_gia_ban DECIMAL(15, 2) NOT NULL,
                                   thanh_tien AS (so_luong * don_gia_ban) PERSISTED,
                                   CONSTRAINT chk_ctdh_sl CHECK (so_luong > 0),
                                   CONSTRAINT chk_ctdh_dongia CHECK (don_gia_ban >= 0),
                                   CONSTRAINT fk_ctdh_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (id) ON DELETE CASCADE,
                                   CONSTRAINT fk_ctdh_bt FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham (id)
);
GO

CREATE INDEX idx_ctdh_dh ON chi_tiet_don_hang (don_hang_id);
GO

CREATE TABLE thanh_toan (
                            id INT IDENTITY (1, 1) PRIMARY KEY,
                            don_hang_id INT NOT NULL,
                            phuong_thuc_thanh_toan_id INT NOT NULL,
                            so_tien DECIMAL(15, 2) NOT NULL,
                            so_tien_thuc_te DECIMAL(15, 2) NULL,
                            ma_giao_dich VARCHAR(100) NULL,
                            trang_thai VARCHAR(15) NOT NULL DEFAULT 'cho',
                            thoi_gian_tao DATETIME2 NOT NULL DEFAULT GETDATE(),
                            thoi_gian_thanh_cong DATETIME2 NULL,
                            vnp_txn_ref VARCHAR(100) NULL,
                            vnp_transaction_no VARCHAR(100) NULL,
                            vnp_response_code VARCHAR(10) NULL,
                            vnp_bank_code VARCHAR(20) NULL,
                            vnp_bank_tran_no VARCHAR(100) NULL,
                            vnp_card_type VARCHAR(20) NULL,
                            vnp_pay_date VARCHAR(20) NULL,
                            vnp_secure_hash VARCHAR(256) NULL,
                            raw_ipn NVARCHAR(MAX) NULL,
                            CONSTRAINT chk_tt_trang_thai CHECK (trang_thai IN ('cho', 'thanh_cong', 'that_bai')),
                            CONSTRAINT chk_tt_so_tien CHECK (so_tien >= 0),
                            CONSTRAINT chk_tt_so_tien_thuc_te CHECK (so_tien_thuc_te >= 0),
                            CONSTRAINT fk_tt_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (id),
                            CONSTRAINT fk_tt_pttt FOREIGN KEY (phuong_thuc_thanh_toan_id) REFERENCES phuong_thuc_thanh_toan (id)
);
GO

CREATE INDEX idx_tt_dh ON thanh_toan (don_hang_id);
GO

CREATE UNIQUE INDEX uq_tt_vnp_txn
    ON thanh_toan (vnp_transaction_no)
    WHERE vnp_transaction_no IS NOT NULL;
GO

CREATE INDEX idx_tt_het_han
    ON thanh_toan (thoi_gian_tao)
    WHERE trang_thai = 'cho';
GO

-- =====================================================
-- MODULE 8: BẢO HÀNH
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

CREATE TABLE yeu_cau_bao_hanh (
                                  id                   INT           IDENTITY(1,1) PRIMARY KEY,
                                  ma_yeu_cau           VARCHAR(50)   NOT NULL,
                                  phieu_bao_hanh_id    INT           NOT NULL,
                                  nguoi_tiep_nhan_id   INT           NULL,
                                  ngay_tiep_nhan       DATETIME2     NOT NULL DEFAULT GETDATE(),
                                  mo_ta_loi            NVARCHAR(MAX),
                                  hinh_thuc            VARCHAR(15)   NOT NULL,
                                  trang_thai           VARCHAR(20)   NOT NULL DEFAULT 'tiep_nhan',
                                  nha_cung_cap_id      INT           NULL,
                                  ngay_gui_ncc         DATETIME2     NULL,
                                  ngay_du_kien_nhan    DATE          NULL,
                                  ngay_nhan_lai_ncc    DATETIME2     NULL,
                                  ket_qua_ncc          NVARCHAR(MAX) NULL,
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
-- MODULE 9: KHUYẾN MÃI (CHỈ GIẢM THEO % & ĐIỀU KIỆN ĐƠN HÀNG)
-- =====================================================

CREATE TABLE chuong_trinh_khuyen_mai (
                                         id                 INT           IDENTITY(1,1) PRIMARY KEY,
                                         ten_ctkm           NVARCHAR(200) NOT NULL,
                                         mo_ta              NVARCHAR(MAX),
                                         loai               VARCHAR(25)   NOT NULL,
                                         gia_tri_uu_dai     DECIMAL(15,2) NOT NULL, -- Giá trị %
                                         giam_toi_da        DECIMAL(15,2) NULL,
                                         don_hang_toi_thieu DECIMAL(15,2) NULL,     -- Được "bế" từ bảng mã giảm giá qua
                                         ngay_bat_dau       DATETIME2     NOT NULL,
                                         ngay_ket_thuc      DATETIME2     NOT NULL,
                                         gio_flash_bat_dau  DATETIME2     NULL,
                                         gio_flash_ket_thuc DATETIME2     NULL,
                                         so_luong_toi_da    INT           NULL,
                                         so_lan_da_dung     INT           NOT NULL DEFAULT 0,
                                         trang_thai         VARCHAR(15)   NOT NULL DEFAULT 'chua_bat_dau',
                                         CONSTRAINT chk_ctkm_loai       CHECK (loai IN ('giam_gia_truc_tiep','phan_tram','flash_sale','don_hang_toi_thieu')),
                                         CONSTRAINT chk_ctkm_trang_thai CHECK (trang_thai IN ('chua_bat_dau','dang_dien_ra','da_ket_thuc','tam_dung'))
);
GO

CREATE INDEX idx_ctkm_tts ON chuong_trinh_khuyen_mai (trang_thai, ngay_bat_dau, ngay_ket_thuc);
GO

CREATE TABLE pham_vi_khuyen_mai (
                                    id          INT NOT NULL IDENTITY(1,1) PRIMARY KEY,
                                    ctkm_id     INT NOT NULL,
                                    san_pham_id INT NOT NULL, -- Bỏ danh_muc_id và hang_sx_id
                                    CONSTRAINT fk_pvkm_ctkm FOREIGN KEY (ctkm_id)     REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE CASCADE,
                                    CONSTRAINT fk_pvkm_sp   FOREIGN KEY (san_pham_id) REFERENCES san_pham(id)
);
GO

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

-- Gắn FK chuong_trinh_khuyen_mai vào don_hang
ALTER TABLE don_hang
    ADD CONSTRAINT fk_dh_ctkm FOREIGN KEY (chuong_trinh_khuyen_mai_id) REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE SET NULL;
GO

-- =====================================================
-- MODULE 11: CHATBOT AI (ĐỘC LẬP TÙY CHỌN, ĐÃ GỠ KHÓA NGOẠI RÀNG BUỘC)
-- =====================================================

CREATE TABLE cuoc_hoi_thoai (
                                id            INT           IDENTITY(1,1) PRIMARY KEY,
                                khach_hang_id INT           NULL,         -- Vẫn lưu ID để biết của ai, nhưng KHÔNG còn FK constraint
                                session_id    VARCHAR(100)  NULL,
                                tieu_de       NVARCHAR(255) NULL,
                                ngay_tao      DATETIME2     NOT NULL DEFAULT GETDATE(),
                                updated_at    DATETIME2     NOT NULL DEFAULT GETDATE()
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
                               hinh_anh_url      VARCHAR(255)  NULL,
                               so_token          INT           NULL,
                               intent            VARCHAR(30)   NULL,
                               don_hang_id_ref   INT           NULL,         -- Lưu tham chiếu lỏng, ĐÃ XÓA FK constraint
                               san_pham_id_ref   INT           NULL,         -- Lưu tham chiếu lỏng, ĐÃ XÓA FK constraint
                               thoi_gian         DATETIME2     NOT NULL DEFAULT GETDATE(),
                               CONSTRAINT chk_tnc_vai    CHECK (vai    IN ('user','model','system')),
                               CONSTRAINT chk_tnc_intent CHECK (intent IN ('tu_van_sp','tra_cuu_dh','bao_hanh','khuyen_mai','chinh_sach','khac') OR intent IS NULL),
                               CONSTRAINT fk_tnc_cht     FOREIGN KEY (cuoc_hoi_thoai_id) REFERENCES cuoc_hoi_thoai(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_tnc_cht ON tin_nhan_chat (cuoc_hoi_thoai_id, thoi_gian);
GO

-- =====================================================
-- MODULE 12: ĐÁNH GIÁ & HỎI ĐÁP
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
-- TRIGGERS CẬP NHẬT UPDATED_AT TỰ ĐỘNG
-- =====================================================

CREATE TRIGGER trg_nd_updated ON nguoi_dung AFTER UPDATE AS
BEGIN
    UPDATE nguoi_dung SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_kh_updated ON khach_hang AFTER UPDATE AS
BEGIN
    UPDATE khach_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_sp_updated ON san_pham AFTER UPDATE AS
BEGIN
    UPDATE san_pham SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_bt_updated ON bien_the_san_pham AFTER UPDATE AS
BEGIN
    UPDATE bien_the_san_pham SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_dh_updated ON don_hang AFTER UPDATE AS
BEGIN
    UPDATE don_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_tk_updated ON ton_kho AFTER UPDATE AS
BEGIN
    UPDATE ton_kho SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_gh_updated ON gio_hang AFTER UPDATE AS
BEGIN
    UPDATE gio_hang SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO

CREATE TRIGGER trg_cht_updated ON cuoc_hoi_thoai AFTER UPDATE AS
BEGIN
    UPDATE cuoc_hoi_thoai SET updated_at = GETDATE() WHERE id IN (SELECT id FROM inserted);
END;
GO


-- 1. Xóa cái ràng buộc UNIQUE cũ đi (Tên constraint có thể khác, bạn xem đúng tên trong log nhé)
ALTER TABLE may_dien_thoai DROP CONSTRAINT uq_may_serial;
ALTER TABLE may_dien_thoai DROP CONSTRAINT uq_may_imei2; -- Chắc chắn imei2 cũng sẽ bị lỗi tương tự, xóa luôn

-- 2. Tạo lại UNIQUE bằng Filtered Index (Chỉ áp dụng Unique khi khác NULL)
CREATE UNIQUE NONCLUSTERED INDEX idx_uq_may_serial 
ON may_dien_thoai(serial) 
WHERE serial IS NOT NULL;

CREATE UNIQUE NONCLUSTERED INDEX idx_uq_may_imei2 
ON may_dien_thoai(imei2) 
WHERE imei2 IS NOT NULL;


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
-- 3. CẤU HÌNH KHO & NHÀ CUNG CẤP
-- =====================================================
INSERT INTO kho (ten_kho, loai, dia_chi, kich_hoat) VALUES
(N'Kho Tổng Miền Bắc', 'kho_tong', N'123 Cầu Giấy, Hà Nội', 1),
(N'Kho Chuyển Phát Online', 'kho_online', N'456 Lê Lợi, TP.HCM', 1);
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
-- 5. BIẾN THỂ SẢN PHẨM (12 BIẾN THỂ - ĐÃ XÓA BARCODE)
-- =====================================================
INSERT INTO bien_the_san_pham (san_pham_id, ma_sku, mau_sac, ma_mau_hex, ram_gb, luu_tru_gb, gia_nhap, gia_ban, trang_thai) VALUES
(1, 'IP15PM-256-NAT', N'Titan Tự Nhiên', '#B5B6B1', 8, 256, 27000000, 29990000, 'con_hang'),
(1, 'IP15PM-512-BLK', N'Titan Đen', '#4B4B4D', 8, 512, 32000000, 35990000, 'con_hang'),
(2, 'IP14-128-BLU', N'Xanh Dương', '#A3C6D3', 6, 128, 16000000, 18490000, 'con_hang'),
(3, 'S24U-256-GRY', N'Xám Titan', '#7D7A7D', 12, 256, 24000000, 26990000, 'con_hang'),
(3, 'S24U-512-YEL', N'Vàng Titan', '#E6DEB8', 12, 512, 28000000, 31490000, 'con_hang'),
(4, 'ZF5-256-BLU', N'Xanh Icy', '#A9BCD0', 12, 256, 30000000, 34990000, 'con_hang'),
(5, 'XM14-256-BLK', N'Đen', '#000000', 12, 256, 18000000, 20990000, 'con_hang'),
(6, 'RMN13-128-PUR', N'Tím', '#9D84B5', 8, 128, 6000000, 7490000, 'het_hang'),
(7, 'R11-256-GRN', N'Xanh Sóng Biển', '#7BA89D', 8, 256, 9000000, 10990000, 'con_hang'),
(8, 'A18-128-BLU', N'Xanh Phát Sáng', '#87CEEB', 4, 128, 3000000, 3990000, 'con_hang');
GO
-- =====================================================
-- 6. HÌNH ẢNH SẢN PHẨM & TỒN KHO
-- =====================================================
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
-- 7. KHUYẾN MÃI (LOGIC MỚI - CHỈ DÙNG %)
-- =====================================================
INSERT INTO chuong_trinh_khuyen_mai (ten_ctkm, loai, gia_tri_uu_dai, giam_toi_da, don_hang_toi_thieu, ngay_bat_dau, ngay_ket_thuc, trang_thai) VALUES
(N'Siêu Sale Sinh Nhật', 'don_hang_toi_thieu', 5.00, 500000, 15000000, '2024-01-01', '2026-12-31', 'dang_dien_ra'),
(N'Giảm giá Tân Sinh Viên', 'phan_tram', 10.00, 1000000, NULL, '2024-01-01', '2026-12-31', 'dang_dien_ra');
GO

-- =====================================================
-- 8. TÀI KHOẢN, KHÁCH HÀNG & ĐỊA CHỈ (ĐÃ BỎ ĐIỂM/HẠNG)
-- =====================================================
INSERT INTO nguoi_dung (email, mat_khau, ho_ten, so_dien_thoai, vai_tro, trang_thai) VALUES
('admin@primemobile.vn', 'hash_admin', N'Lê Quản Trị', '0901111111', 'Admin', 'hoat_dong'),
('nhanvien@primemobile.vn', 'hash_nv', N'Trần Bán Hàng', '0902222222', 'NhanVien', 'hoat_dong'),
('khach1@gmail.com', 'hash_kh1', N'Nguyễn Văn An', '0912345678', 'KhachHang', 'hoat_dong'),
('khach2@gmail.com', 'hash_kh2', N'Phạm Thị Hoa', '0987654321', 'KhachHang', 'hoat_dong');
GO

INSERT INTO khach_hang (nguoi_dung_id, ho_ten, email, so_dien_thoai) VALUES
(3, N'Nguyễn Văn An', 'khach1@gmail.com', '0912345678'),
(4, N'Phạm Thị Hoa', 'khach2@gmail.com', '0987654321'),
(NULL, N'Khách Lẻ Mặc Định', NULL, '0000000000');
GO

INSERT INTO dia_chi_khach_hang (khach_hang_id, loai_dia_chi, ho_ten_nguoi_nhan, so_dien_thoai_nguoi_nhan, dia_chi_chi_tiet, tinh_thanh_id, quan_huyen_id, phuong_xa_code, tinh_thanh_ten, quan_huyen_ten, phuong_xa_ten, mac_dinh) VALUES
(1, 'nha_rieng', N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', 201, 1482, '1A03', N'Hà Nội', N'Quận Cầu Giấy', N'Phường Dịch Vọng', 1),
(2, 'co_quan', N'Phạm Thị Hoa', '0987654321', N'Tòa nhà X, Đường Y', 202, 1442, '1B04', N'TP.HCM', N'Quận 1', N'Phường Bến Nghé', 1);
GO

-- =====================================================
-- 9. ĐƠN HÀNG (VNPay Compatible) & CHI TIẾT
-- Đã thay ma_giam_gia_id thành chuong_trinh_khuyen_mai_id
-- =====================================================
-- Đơn hàng 1: ĐÃ GIAO - Đã thanh toán qua VNPay thành công
INSERT INTO don_hang (ma_don_hang, khach_hang_id, nguoi_xu_ly_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, chuong_trinh_khuyen_mai_id, ngay_giao_du_kien, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH1', 1, 2, 'online', 1, N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', N'Phường Dịch Vọng', N'Quận Cầu Giấy', N'Hà Nội', 28990000, 500000, 30000, 1, '2026-06-25', 'da_giao', 'da_thanh_toan', '2026-05-10');

-- Đơn hàng 2: ĐANG GIAO - Thanh toán COD (Chưa thanh toán)
INSERT INTO don_hang (ma_don_hang, khach_hang_id, nguoi_xu_ly_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, ngay_giao_du_kien, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH2', 2, 2, 'online', 2, N'Phạm Thị Hoa', '0987654321', N'Tòa nhà X, Đường Y', N'Phường Bến Nghé', N'Quận 1', N'TP.HCM', 25490000, 0, 45000, '2026-06-22', 'dang_giao', 'chua_thanh_toan', GETDATE());

-- Đơn hàng 3: CHỜ XÁC NHẬN - Đang đợi khách thao tác trên cổng VNPay
INSERT INTO don_hang (ma_don_hang, khach_hang_id, kenh_ban, dia_chi_giao_id, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, trang_thai, trang_thai_thanh_toan, thoi_gian_het_han_tt, ngay_dat) VALUES
('DH3', 1, 'online', 1, N'Nguyễn Văn An', '0912345678', N'Số 1, Ngõ 2', N'Phường Dịch Vọng', N'Quận Cầu Giấy', N'Hà Nội', 19990000, 0, 25000, 'cho_xac_nhan', 'dang_chuyen_huong', DATEADD(MINUTE, 15, GETDATE()), GETDATE());

-- Đơn hàng 4: ĐÃ HỦY - Thanh toán VNPay thất bại/quá hạn
INSERT INTO don_hang (ma_don_hang, khach_hang_id, kenh_ban, ho_ten_nguoi_nhan, sdt_nguoi_nhan, dia_chi_giao_cu_the, phuong_xa_giao, quan_huyen_giao, tinh_thanh_giao, tong_tien_hang, tien_giam_gia, phi_ship, trang_thai, trang_thai_thanh_toan, ngay_dat) VALUES
('DH4', 3, 'online', N'Khách Lẻ Mặc Định', '0000000000', N'Tạm vắng', N'Phường X', N'Quận Y', N'Tỉnh Z', 10490000, 0, 30000, 'da_huy', 'that_bai', '2026-06-01');
GO

INSERT INTO chi_tiet_don_hang (don_hang_id, bien_the_san_pham_id, so_luong, don_gia_ban) VALUES
(1, 1, 1, 28990000), -- Đơn 1: IP15PM
(2, 4, 1, 25490000), -- Đơn 2: S24 Ultra
(3, 7, 1, 19990000), -- Đơn 3: Xiaomi 14
(4, 9, 1, 10490000); -- Đơn 4: OPPO Reno11
GO

-- =====================================================
-- 10. BẢNG THANH TOÁN (Cập nhật cho VNPay)
-- =====================================================
INSERT INTO thanh_toan (don_hang_id, phuong_thuc_thanh_toan_id, so_tien, so_tien_thuc_te, trang_thai, vnp_txn_ref, vnp_transaction_no) VALUES
(1, 3, 28520000, 28520000, 'thanh_cong', 'DH1', 'VNPAY_111222333'), -- Đơn 1 VNPay OK
(2, 1, 25535000, NULL, 'cho', NULL, NULL),                 -- Đơn 2 chờ COD
(3, 3, 20015000, NULL, 'cho', 'DH3', NULL);                  -- Đơn 3 đang chuyển hướng VNPay
GO

-- =====================================================
-- 11. IMEI (MÁY VẬT LÝ) & GẮN VÀO ĐƠN HÀNG
-- =====================================================
INSERT INTO may_dien_thoai (bien_the_san_pham_id, imei1, imei2, serial, tinh_trang, don_hang_id) VALUES
(1, '351111111111111', '861111111111111', 'SN_IP15_001', 'da_ban', 1), 
(4, '352222222222222', '862222222222222', 'SN_S24U_002', 'da_ban', 2), 
(7, '353333333333333', '863333333333333', 'SN_XM14_003', 'trong_kho', NULL),
(9, '354444444444444', '864444444444444', 'SN_RENO_004', 'trong_kho', NULL);
GO

-- =====================================================
-- 12. ĐÁNH GIÁ, BẢO HÀNH & WISHLIST
-- =====================================================
INSERT INTO danh_gia_san_pham (san_pham_id, khach_hang_id, don_hang_id, sao, tieu_de, noi_dung, trang_thai) VALUES
(1, 1, 1, 5, N'Điện thoại siêu xịn', N'Cầm rất nhẹ tay do khung titan, màu Tự Nhiên cực sang, ship siêu nhanh!', 'da_duyet');
GO

-- Sinh bảo hành cho máy đã bán ở Đơn 1
DECLARE @ActualMayId INT;
SELECT @ActualMayId = id FROM may_dien_thoai WHERE imei1 = '351111111111111';
INSERT INTO phieu_bao_hanh (ma_phieu, may_dien_thoai_id, khach_hang_id, don_hang_id, so_thang_bao_hanh, ngay_bat_dau, ngay_het_han) 
VALUES ('BH_IP15_001', @ActualMayId, 1, 1, 12, '2026-05-12', '2027-05-12');
GO

INSERT INTO yeu_thich (khach_hang_id, san_pham_id) VALUES
(1, 3), (1, 4), (2, 1);
GO

INSERT INTO hoi_dap_san_pham (san_pham_id, khach_hang_id, cau_hoi, tra_loi, nguoi_tra_loi_id) VALUES
(1, 2, N'Máy này dùng sạc 20W hay 30W vậy shop?', N'Dạ chào bạn, iPhone 15 Pro Max hỗ trợ sạc nhanh tối đa lên đến 27W ạ.', 1);
GO