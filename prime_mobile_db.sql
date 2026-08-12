IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'PrimeMobile')
    CREATE DATABASE PrimeMobile COLLATE Vietnamese_CI_AS;
GO

USE PrimeMobile;
GO

-- =====================================================
-- MODULE 1: NGƯỜI DÙNG & PHÂN QUYỀN
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
    CONSTRAINT uq_nd_email        UNIQUE (email),
    CONSTRAINT chk_nd_vai_tro     CHECK (vai_tro    IN ('Admin','NhanVien','KhachHang')),
    CONSTRAINT chk_nd_trang_thai  CHECK (trang_thai IN ('hoat_dong','khoa'))
);
GO

CREATE INDEX idx_nd_email ON nguoi_dung (email);
GO

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
    CONSTRAINT chk_kh_gioi_tinh  CHECK (gioi_tinh IN ('Nam','Nu','Khac')),
    CONSTRAINT fk_kh_nd          FOREIGN KEY (nguoi_dung_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_kh_sdt   ON khach_hang (so_dien_thoai);
CREATE INDEX idx_kh_email ON khach_hang (email);
CREATE UNIQUE NONCLUSTERED INDEX uq_kh_nguoi_dung ON khach_hang(nguoi_dung_id) WHERE nguoi_dung_id IS NOT NULL;
GO

-- =====================================================
-- MODULE 2: SẢN PHẨM & BIẾN THỂ
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
    id        INT           IDENTITY(1,1) PRIMARY KEY,
    ten_hang  NVARCHAR(100) NOT NULL,
    logo      VARCHAR(255),
    quoc_gia  NVARCHAR(50),
    CONSTRAINT uq_hsx_ten UNIQUE (ten_hang)
);
GO

CREATE TABLE mau_sac (
    id        INT           IDENTITY(1,1) PRIMARY KEY,
    ten_mau   NVARCHAR(100) NOT NULL,
    mo_ta     NVARCHAR(255) NULL,
    ngay_tao  DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT uq_ms_ten_mau UNIQUE (ten_mau)
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

CREATE TABLE bien_the_san_pham (
    id               INT           IDENTITY(1,1) PRIMARY KEY,
    san_pham_id      INT           NOT NULL,
    mau_sac_id       INT           NOT NULL,
    ma_sku           VARCHAR(100)  NOT NULL,
    ram_gb           INT           NOT NULL,
    luu_tru_gb       INT           NOT NULL,
    loai_luu_tru     VARCHAR(20)   NOT NULL DEFAULT 'UFS',
    gia_ban          DECIMAL(15,2) NOT NULL,
    trong_luong_gram INT,
    pin_mAh          INT,
    trang_thai       VARCHAR(20)   NOT NULL DEFAULT 'con_hang',
    ngay_tao         DATETIME2     NOT NULL DEFAULT GETDATE(),
    updated_at       DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT uq_bt_ma_sku      UNIQUE (ma_sku),
    CONSTRAINT chk_bt_trang_thai CHECK (trang_thai IN ('con_hang','het_hang','ngung_kinh_doanh')),
    CONSTRAINT fk_bt_sp          FOREIGN KEY (san_pham_id) REFERENCES san_pham(id) ON DELETE CASCADE,
    CONSTRAINT fk_bt_ms          FOREIGN KEY (mau_sac_id)  REFERENCES mau_sac(id)
);
GO

CREATE INDEX idx_bt_sp ON bien_the_san_pham (san_pham_id, trang_thai);
GO

-- =====================================================
-- BẢNG MÁY ĐIỆN THOẠI (ĐÃ CẬP NHẬT TÍNH NĂNG GIỮ MÁY)
-- =====================================================
CREATE TABLE may_dien_thoai (
    id                   INT           IDENTITY(1,1) PRIMARY KEY,
    bien_the_san_pham_id INT           NOT NULL,
    imei1                VARCHAR(15)   NOT NULL,
    imei2                VARCHAR(15)   NULL,
    tinh_trang           VARCHAR(15)   NOT NULL DEFAULT 'trong_kho',
    ngay_nhap_kho        DATETIME2     NOT NULL DEFAULT GETDATE(),
    don_hang_id          INT           NULL,
    ghi_chu              NVARCHAR(MAX) NULL,
    nguoi_giu_id         INT           NULL,
    thoi_gian_giu        DATETIME2     NULL,
    CONSTRAINT uq_may_imei1             UNIQUE (imei1),
    CONSTRAINT chk_may_tinh_trang       CHECK (tinh_trang IN ('trong_kho','dang_giu','da_ban','loi_hong')),
    CONSTRAINT fk_may_bt                FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id),
    CONSTRAINT fk_may_dien_thoai_nguoi_giu FOREIGN KEY (nguoi_giu_id) REFERENCES nguoi_dung(id) ON DELETE SET NULL
);
GO

CREATE UNIQUE NONCLUSTERED INDEX idx_uq_may_imei2
ON may_dien_thoai(imei2)
WHERE imei2 IS NOT NULL;
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
    dia_chi   NVARCHAR(255),
    kich_hoat BIT           NOT NULL DEFAULT 1
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
    CONSTRAINT fk_pnk_kho         FOREIGN KEY (kho_id)          REFERENCES kho(id),
    CONSTRAINT fk_pnk_nd          FOREIGN KEY (nguoi_tao_id)    REFERENCES nguoi_dung(id)
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
    ten_goi_nho              NVARCHAR(255) NULL,
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
    CONSTRAINT uq_ctgh      UNIQUE (gio_hang_id, bien_the_san_pham_id),
    CONSTRAINT chk_ctgh_sl  CHECK (so_luong > 0),
    CONSTRAINT fk_ctgh_gh   FOREIGN KEY (gio_hang_id)          REFERENCES gio_hang(id) ON DELETE CASCADE,
    CONSTRAINT fk_ctgh_bt   FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham(id)
);
GO

-- =====================================================
-- MODULE 9: KHUYẾN MÃI
-- =====================================================

CREATE TABLE chuong_trinh_khuyen_mai (
    id                 INT           IDENTITY(1,1) PRIMARY KEY,
    ten_ctkm           NVARCHAR(200) NOT NULL,
    mo_ta              NVARCHAR(MAX),
    loai               VARCHAR(25)   NOT NULL,
    gia_tri_uu_dai     DECIMAL(15,2) NOT NULL,
    giam_toi_da        DECIMAL(15,2) NULL,
    don_hang_toi_thieu DECIMAL(15,2) NULL,
    ngay_bat_dau       DATETIME2     NOT NULL,
    ngay_ket_thuc      DATETIME2     NOT NULL,
    trang_thai         VARCHAR(15)   NOT NULL DEFAULT 'chua_bat_dau',
    CONSTRAINT chk_ctkm_loai        CHECK (loai IN ('theo_don_hang','theo_san_pham')),
    CONSTRAINT chk_ctkm_trang_thai  CHECK (trang_thai IN ('chua_bat_dau','dang_dien_ra','da_ket_thuc','tam_dung'))
);
GO

CREATE INDEX idx_ctkm_tts ON chuong_trinh_khuyen_mai (trang_thai, ngay_bat_dau, ngay_ket_thuc);
GO

-- Bảng phạm vi khuyến mãi (đã sửa lại để map với biến thể)
CREATE TABLE pham_vi_khuyen_mai (
    id INT IDENTITY(1,1) PRIMARY KEY,
    ctkm_id INT NOT NULL,
    bien_the_id INT NOT NULL,
    CONSTRAINT fk_pvkm_ctkm FOREIGN KEY (ctkm_id) REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE CASCADE,
    CONSTRAINT fk_pvkm_bt    FOREIGN KEY (bien_the_id) REFERENCES bien_the_san_pham(id)
);
GO

-- =====================================================
-- MODULE 7: ĐƠN HÀNG BÁN
-- =====================================================

CREATE TABLE phuong_thuc_thanh_toan (
    id        INT IDENTITY (1, 1) PRIMARY KEY,
    ten_pttt  NVARCHAR(50) NOT NULL,
    mo_ta     NVARCHAR(255) NULL,
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
    id                         INT IDENTITY (1, 1) PRIMARY KEY,
    ma_don_hang                VARCHAR(50) NOT NULL,
    khach_hang_id              INT NOT NULL,
    nguoi_xu_ly_id             INT NULL,
    kenh_ban                   VARCHAR(10) NOT NULL DEFAULT 'online',
    ngay_dat                   DATETIME2 NOT NULL DEFAULT GETDATE(),
    dia_chi_giao_id            INT NULL,
    ho_ten_nguoi_nhan          NVARCHAR(100) NULL,
    sdt_nguoi_nhan             VARCHAR(20) NULL,
    email_nguoi_nhan           VARCHAR(100) NULL,
    dia_chi_giao_cu_the        NVARCHAR(255) NULL,
    phuong_xa_giao             NVARCHAR(100) NULL,
    quan_huyen_giao            NVARCHAR(100) NULL,
    tinh_thanh_giao            NVARCHAR(100) NULL,
    tong_tien_hang             DECIMAL(15, 2) NOT NULL,
    tien_giam_gia              DECIMAL(15, 2) NOT NULL DEFAULT 0,
    phi_ship                   DECIMAL(15, 2) NOT NULL DEFAULT 0,
    tong_thanh_toan            AS (tong_tien_hang - tien_giam_gia + phi_ship) PERSISTED,
    chuong_trinh_khuyen_mai_id INT NULL,
    ngay_giao_du_kien          DATE NULL,
    ngay_giao_thuc_te          DATETIME2 NULL,
    trang_thai                 VARCHAR(20) NOT NULL DEFAULT 'cho_xac_nhan',
    trang_thai_thanh_toan      VARCHAR(20) NOT NULL DEFAULT 'chua_thanh_toan',
    thoi_gian_het_han_tt       DATETIME2 NULL,
    ghi_chu                    NVARCHAR(MAX) NULL,
    updated_at                 DATETIME2 NOT NULL DEFAULT GETDATE(),
    CONSTRAINT uq_dh_ma UNIQUE (ma_don_hang),
    CONSTRAINT chk_dh_kenh             CHECK (kenh_ban IN ('online', 'tai_quay')),
    CONSTRAINT chk_dh_trang_thai       CHECK (trang_thai IN ('cho_xac_nhan', 'cho_hoan_tien', 'da_xac_nhan', 'dang_giao', 'da_hoan_thanh', 'da_huy', 'don_hang_cho', 'giao_that_bai')),
    CONSTRAINT chk_dh_trang_thai_tt    CHECK (trang_thai_thanh_toan IN ('chua_thanh_toan', 'dang_chuyen_huong', 'da_thanh_toan', 'that_bai', 'da_hoan_tien')),
    CONSTRAINT chk_dh_tong_tien_hang   CHECK (tong_tien_hang >= 0),
    CONSTRAINT chk_dh_tien_giam_gia    CHECK (tien_giam_gia >= 0),
    CONSTRAINT chk_dh_phi_ship         CHECK (phi_ship >= 0),
    CONSTRAINT fk_dh_kh                FOREIGN KEY (khach_hang_id)              REFERENCES khach_hang (id),
    CONSTRAINT fk_dh_nd                FOREIGN KEY (nguoi_xu_ly_id)             REFERENCES nguoi_dung (id),
    CONSTRAINT fk_dh_dc                FOREIGN KEY (dia_chi_giao_id)            REFERENCES dia_chi_khach_hang (id) ON DELETE SET NULL,
    CONSTRAINT fk_dh_ctkm              FOREIGN KEY (chuong_trinh_khuyen_mai_id) REFERENCES chuong_trinh_khuyen_mai(id) ON DELETE SET NULL
);
GO

CREATE INDEX idx_dh_kh   ON don_hang (khach_hang_id, ngay_dat);
CREATE INDEX idx_dh_tts  ON don_hang (trang_thai, ngay_dat);
CREATE INDEX idx_dh_ngay ON don_hang (ngay_dat);
CREATE INDEX idx_dh_tttt ON don_hang (trang_thai_thanh_toan, thoi_gian_het_han_tt);
GO

ALTER TABLE may_dien_thoai
    ADD CONSTRAINT fk_may_dh FOREIGN KEY (don_hang_id) REFERENCES don_hang (id);
GO

CREATE TABLE chi_tiet_don_hang (
    id                   INT IDENTITY (1, 1) PRIMARY KEY,
    don_hang_id          INT NOT NULL,
    bien_the_san_pham_id INT NOT NULL,
    so_luong             INT NOT NULL,
    don_gia_ban          DECIMAL(15, 2) NOT NULL,
    thanh_tien           AS (so_luong * don_gia_ban) PERSISTED,
    CONSTRAINT chk_ctdh_sl     CHECK (so_luong > 0),
    CONSTRAINT chk_ctdh_dongia CHECK (don_gia_ban >= 0),
    CONSTRAINT fk_ctdh_dh      FOREIGN KEY (don_hang_id)          REFERENCES don_hang (id) ON DELETE CASCADE,
    CONSTRAINT fk_ctdh_bt      FOREIGN KEY (bien_the_san_pham_id) REFERENCES bien_the_san_pham (id)
);
GO

CREATE INDEX idx_ctdh_dh ON chi_tiet_don_hang (don_hang_id);
GO

CREATE TABLE thanh_toan (
    id                        INT IDENTITY (1, 1) PRIMARY KEY,
    don_hang_id               INT NOT NULL,
    phuong_thuc_thanh_toan_id INT NOT NULL,
    so_tien                   DECIMAL(15, 2) NOT NULL,
    so_tien_thuc_te           DECIMAL(15, 2) NULL,
    ma_giao_dich              VARCHAR(100) NULL,
    trang_thai                VARCHAR(15) NOT NULL DEFAULT 'cho',
    thoi_gian_tao             DATETIME2 NOT NULL DEFAULT GETDATE(),
    thoi_gian_thanh_cong      DATETIME2 NULL,
    vnp_txn_ref               VARCHAR(100) NULL,
    vnp_transaction_no        VARCHAR(100) NULL,
    vnp_response_code         VARCHAR(10) NULL,
    vnp_bank_code             VARCHAR(20) NULL,
    vnp_bank_tran_no          VARCHAR(100) NULL,
    vnp_card_type             VARCHAR(20) NULL,
    vnp_pay_date              VARCHAR(20) NULL,
    vnp_secure_hash           VARCHAR(256) NULL,
    raw_ipn                   NVARCHAR(MAX) NULL,
    CONSTRAINT chk_tt_trang_thai      CHECK (trang_thai IN ('cho', 'thanh_cong', 'that_bai', 'da_hoan_tien')),
    CONSTRAINT chk_tt_so_tien         CHECK (so_tien >= 0),
    CONSTRAINT chk_tt_so_tien_thuc_te CHECK (so_tien_thuc_te >= 0),
    CONSTRAINT fk_tt_dh               FOREIGN KEY (don_hang_id)               REFERENCES don_hang (id),
    CONSTRAINT fk_tt_pttt             FOREIGN KEY (phuong_thuc_thanh_toan_id) REFERENCES phuong_thuc_thanh_toan (id)
);
GO

CREATE INDEX idx_tt_dh ON thanh_toan (don_hang_id);
GO

CREATE UNIQUE INDEX uq_tt_vnp_txn ON thanh_toan (vnp_transaction_no) WHERE vnp_transaction_no IS NOT NULL;
CREATE INDEX idx_tt_het_han       ON thanh_toan (thoi_gian_tao)      WHERE trang_thai = 'cho';
GO

-- =====================================================
-- MODULE 11: CHATBOT AI
-- =====================================================

CREATE TABLE cuoc_hoi_thoai (
    id            INT           IDENTITY(1,1) PRIMARY KEY,
    khach_hang_id INT           NULL,
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
    don_hang_id_ref   INT           NULL,
    san_pham_id_ref   INT           NULL,
    thoi_gian         DATETIME2     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT chk_tnc_vai    CHECK (vai    IN ('user','model','system')),
    CONSTRAINT chk_tnc_intent CHECK (intent IN ('tu_van_sp','tra_cuu_dh','khuyen_mai','chinh_sach','khac') OR intent IS NULL),
    CONSTRAINT fk_tnc_cht     FOREIGN KEY (cuoc_hoi_thoai_id) REFERENCES cuoc_hoi_thoai(id) ON DELETE CASCADE
);
GO

CREATE INDEX idx_tnc_cht ON tin_nhan_chat (cuoc_hoi_thoai_id, thoi_gian);
GO

-- =====================================================
-- MODULE 12: ĐÁNH GIÁ (đã xóa bảng hoi_dap_san_pham)
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

-- =====================================================
-- TRIGGER GIỚI HẠN 10 CUỘC HỘI THOẠI CHATBOT GẦN NHẤT
-- =====================================================

CREATE TRIGGER trg_limit_chatbot_history
    ON cuoc_hoi_thoai
    AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @khach_hang_id INT, @session_id VARCHAR(100);
    SELECT @khach_hang_id = khach_hang_id, @session_id = session_id FROM inserted;

    IF @khach_hang_id IS NOT NULL
    BEGIN
        DELETE FROM cuoc_hoi_thoai
        WHERE khach_hang_id = @khach_hang_id
          AND id NOT IN (
            SELECT TOP 10 id FROM cuoc_hoi_thoai
            WHERE khach_hang_id = @khach_hang_id
            ORDER BY updated_at DESC, id DESC
        );
    END
    ELSE IF @session_id IS NOT NULL
    BEGIN
        DELETE FROM cuoc_hoi_thoai
        WHERE session_id = @session_id
          AND id NOT IN (
            SELECT TOP 10 id FROM cuoc_hoi_thoai
            WHERE session_id = @session_id
            ORDER BY updated_at DESC, id DESC
        );
    END
END;
GO

-- =====================================================
-- CÁC THAY ĐỔI BỔ SUNG (giữ nguyên từ script gốc)
-- =====================================================

USE PrimeMobile;
GO

ALTER TABLE nguoi_dung
DROP CONSTRAINT uq_nd_email;
GO

ALTER TABLE khach_hang
ADD CONSTRAINT uq_kh_so_dien_thoai UNIQUE (so_dien_thoai);
GO

-- Cột giam_toi_da đã được thêm khi tạo bảng chuong_trinh_khuyen_mai ở trên
-- (đã bao gồm trong định nghĩa bảng)

-- Ràng buộc chk_dh_trang_thai đã được sửa khi tạo bảng don_hang
-- (đã bao gồm 'giao_that_bai')

-- Cột ten_goi_nho đã được thêm vào bảng dia_chi_khach_hang ở trên

USE PrimeMobile;
GO

-- =====================================================
-- 1. NGƯỜI DÙNG (Chỉ duy nhất Admin)
-- =====================================================
INSERT INTO nguoi_dung (email, mat_khau, ho_ten, vai_tro, trang_thai) VALUES
('admin@primemobile.vn', 'admin123', N'Admin PrimeMobile', 'Admin', 'hoat_dong');
GO

-- =====================================================
-- 2. DỮ LIỆU CƠ BẢN (Danh mục, Hãng, Kho, NCC)
-- =====================================================
INSERT INTO danh_muc (ten_danh_muc, slug, thu_tu) VALUES
(N'Điện thoại cao cấp', 'dien-thoai-cao-cap', 1),
(N'Điện thoại tầm trung', 'dien-thoai-tam-trung', 2),
(N'Điện thoại gập', 'dien-thoai-gap', 3),
(N'Điện thoại giá rẻ', 'dien-thoai-gia-re', 4);

INSERT INTO hang_san_xuat (ten_hang, logo, quoc_gia) VALUES
('Apple', 'https://upload.wikimedia.org/wikipedia/commons/f/fa/Apple_logo_black.svg', N'Mỹ'),
('Samsung', 'https://upload.wikimedia.org/wikipedia/commons/2/24/Samsung_Logo.svg', N'Hàn Quốc'),
('Xiaomi', 'https://upload.wikimedia.org/wikipedia/commons/2/29/Xiaomi_logo.svg', N'Trung Quốc'),
('OPPO', 'https://upload.wikimedia.org/wikipedia/commons/c/ca/OPPO_LOGO_2019.svg', N'Trung Quốc');

INSERT INTO kho (ten_kho, dia_chi, kich_hoat) VALUES
(N'Kho Tổng Miền Bắc', N'123 Cầu Giấy, Hà Nội', 1);

INSERT INTO nha_cung_cap (ma_ncc, ten_ncc, so_dien_thoai, dia_chi, nguoi_lien_he, trang_thai) VALUES
('NCC_FPT', N'FPT Synnex', '19006600', N'Cầu Giấy, Hà Nội', N'Mr. Hùng', 'dang_hop_tac'),
('NCC_DGW', N'Digiworld', '19001234', N'Quận 1, TP.HCM', N'Ms. Lan', 'dang_hop_tac');

-- (Đã xóa INSERT trung_tam_bao_hanh vì bảng không tồn tại)

-- =====================================================
-- 3. MÀU SẮC (đưa tất cả màu duy nhất vào bảng mau_sac)
-- =====================================================
INSERT INTO mau_sac (ten_mau) VALUES
(N'Titan Tự Nhiên'), (N'Titan Đen'), (N'Titan Xanh'), (N'Hồng'), (N'Đen'),
(N'Tím Đậm'), (N'Vàng'), (N'Trắng'), (N'Xanh Nhạt'), (N'Trắng Starlight'),
(N'Đen Midnight'), (N'Xám Titan'), (N'Đen Titan'), (N'Tím Cobalt'), (N'Đen Onyx'),
(N'Vàng Amber'), (N'Xám Marble'), (N'Xanh Icy'), (N'Đen Phantom'), (N'Xanh Mint'),
(N'Tím Lavender'), (N'Xanh Iceblue'), (N'Đen Navy'), (N'Trắng Da Nhám'), (N'Đen Da Nhám'),
(N'Xanh Ngọc'), (N'Đen Tuyền'), (N'Tím Aurora'), (N'Đen Bán Dạ'), (N'Vàng Champagne'),
(N'Đen Cổ Điển'), (N'Trắng Ngọc Trai'), (N'Xám Không Gian'), (N'Tím Phát Sáng'), (N'Đen Lấp Lánh');
GO

-- =====================================================
-- 4. SẢN PHẨM (bỏ cột bao_hanh_thang)
-- =====================================================
INSERT INTO san_pham (ma_san_pham, ten_san_pham, danh_muc_id, hang_san_xuat_id, mo_ta_ngan, nam_ra_mat) VALUES
-- APPLE (6 MÁY)
('SP_IP15PM', N'iPhone 15 Pro Max', 1, 1, N'Khung Titanium siêu nhẹ, Camera 5x zoom quang học.', 2023),
('SP_IP15P', N'iPhone 15 Pro', 1, 1, N'Hiệu năng mạnh mẽ với chip A17 Pro, thiết kế Titanium.', 2023),
('SP_IP15', N'iPhone 15', 1, 1, N'Dynamic Island hiện đại, camera chính 48MP sắc nét.', 2023),
('SP_IP14PM', N'iPhone 14 Pro Max', 1, 1, N'Màn hình Always-On Display, pin cực trâu.', 2022),
('SP_IP14', N'iPhone 14', 1, 1, N'Thiết kế nhôm nguyên khối, chip A15 Bionic ổn định.', 2022),
('SP_IP13', N'iPhone 13', 2, 1, N'Chiếc điện thoại quốc dân, giá tốt hiệu năng mượt.', 2021),

-- SAMSUNG (6 MÁY)
('SP_S24U', N'Samsung Galaxy S24 Ultra', 1, 2, N'Quyền năng Galaxy AI, bút S-Pen thông minh.', 2024),
('SP_S24P', N'Samsung Galaxy S24+', 1, 2, N'Màn hình lớn QHD+, viền siêu mỏng, tích hợp AI.', 2024),
('SP_S24', N'Samsung Galaxy S24', 1, 2, N'Nhỏ gọn, mạnh mẽ, thiết kế hiện đại vuông vức.', 2024),
('SP_ZFOLD5', N'Samsung Galaxy Z Fold5', 3, 2, N'Bản lề Flex mượt mà, trải nghiệm màn hình lớn.', 2023),
('SP_ZFLIP5', N'Samsung Galaxy Z Flip5', 3, 2, N'Màn hình phụ Flex Window lớn, gập mở linh hoạt.', 2023),
('SP_A55', N'Samsung Galaxy A55 5G', 2, 2, N'Khung kim loại, mặt lưng kính sang trọng, pin 5000mAh.', 2024),

-- XIAOMI (3 MÁY)
('SP_XM14U', N'Xiaomi 14 Ultra', 1, 3, N'Đỉnh cao nhiếp ảnh di động với cụm 4 ống kính Leica.', 2024),
('SP_XM14', N'Xiaomi 14', 1, 3, N'Ống kính Leica, cấu hình mạnh mẽ, Snapdragon 8 Gen 3.', 2024),
('SP_RMN13', N'Xiaomi Redmi Note 13 Pro+', 2, 3, N'Camera 200MP, sạc siêu tốc 120W, màn hình cong.', 2023),

-- OPPO (3 MÁY)
('SP_FN3', N'OPPO Find N3', 3, 4, N'Điện thoại gập siêu mỏng nhẹ, camera Hasselblad.', 2023),
('SP_RENO11P', N'OPPO Reno11 Pro 5G', 2, 4, N'Chuyên gia chân dung, thiết kế viền cong mỏng.', 2024),
('SP_A79', N'OPPO A79 5G', 4, 4, N'Màn hình lớn 90Hz, loa kép âm thanh sống động.', 2023);
GO

-- =====================================================
-- 5. BIẾN THỂ SẢN PHẨM (sử dụng mau_sac_id, bỏ ma_mau_hex)
-- =====================================================
INSERT INTO bien_the_san_pham (san_pham_id, mau_sac_id, ma_sku, ram_gb, luu_tru_gb, gia_ban, trang_thai) VALUES
-- 1. SP_IP15PM (iPhone 15 Pro Max)
(1, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Tự Nhiên'), 'IP15PM-256-NAT', 8, 256, 29990000, 'con_hang'),
(1, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Đen'),     'IP15PM-256-BLK', 8, 256, 29990000, 'con_hang'),
(1, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Đen'),     'IP15PM-512-BLK', 8, 512, 35990000, 'con_hang'),

-- 2. SP_IP15P (iPhone 15 Pro)
(2, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Xanh'),    'IP15P-128-BLU',  8, 128, 25990000, 'con_hang'),
(2, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Tự Nhiên'),'IP15P-128-NAT',  8, 128, 25990000, 'con_hang'),
(2, (SELECT id FROM mau_sac WHERE ten_mau = N'Titan Tự Nhiên'),'IP15P-256-NAT',  8, 256, 28990000, 'con_hang'),

-- 3. SP_IP15 (iPhone 15)
(3, (SELECT id FROM mau_sac WHERE ten_mau = N'Hồng'),          'IP15-128-PNK',   6, 128, 19990000, 'con_hang'),
(3, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen'),           'IP15-128-BLK',   6, 128, 19990000, 'con_hang'),
(3, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen'),           'IP15-256-BLK',   6, 256, 22990000, 'con_hang'),

-- 4. SP_IP14PM (iPhone 14 Pro Max)
(4, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Đậm'),       'IP14PM-128-PUR', 6, 128, 25490000, 'con_hang'),
(4, (SELECT id FROM mau_sac WHERE ten_mau = N'Vàng'),          'IP14PM-128-GLD', 6, 128, 25490000, 'con_hang'),
(4, (SELECT id FROM mau_sac WHERE ten_mau = N'Vàng'),          'IP14PM-256-GLD', 6, 256, 27490000, 'con_hang'),

-- 5. SP_IP14 (iPhone 14)
(5, (SELECT id FROM mau_sac WHERE ten_mau = N'Trắng'),         'IP14-128-WHT',   6, 128, 16990000, 'con_hang'),
(5, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Nhạt'),     'IP14-128-BLU',   6, 128, 16990000, 'con_hang'),
(5, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Nhạt'),     'IP14-256-BLU',   6, 256, 19990000, 'con_hang'),

-- 6. SP_IP13 (iPhone 13)
(6, (SELECT id FROM mau_sac WHERE ten_mau = N'Trắng Starlight'),'IP13-128-STA',   6, 128, 13990000, 'con_hang'),
(6, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Midnight'),  'IP13-128-MID',   6, 128, 13990000, 'con_hang'),
(6, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Midnight'),  'IP13-256-MID',   6, 256, 16990000, 'het_hang'),

-- 7. SP_S24U (Samsung Galaxy S24 Ultra)
(7, (SELECT id FROM mau_sac WHERE ten_mau = N'Xám Titan'),     'S24U-256-GRY',   12, 256, 26990000, 'con_hang'),
(7, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Titan'),     'S24U-256-BLK',   12, 256, 26990000, 'con_hang'),
(7, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Titan'),     'S24U-512-BLK',   12, 512, 30490000, 'con_hang'),

-- 8. SP_S24P (Samsung Galaxy S24+)
(8, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Cobalt'),    'S24P-256-VIO',   12, 256, 23990000, 'con_hang'),
(8, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Onyx'),      'S24P-256-BLK',   12, 256, 23990000, 'con_hang'),
(8, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Onyx'),      'S24P-512-BLK',   12, 512, 27490000, 'con_hang'),

-- 9. SP_S24 (Samsung Galaxy S24)
(9, (SELECT id FROM mau_sac WHERE ten_mau = N'Vàng Amber'),    'S24-256-YEL',    8, 256, 19990000, 'con_hang'),
(9, (SELECT id FROM mau_sac WHERE ten_mau = N'Xám Marble'),    'S24-256-GRY',    8, 256, 19990000, 'con_hang'),
(9, (SELECT id FROM mau_sac WHERE ten_mau = N'Xám Marble'),    'S24-512-GRY',    8, 512, 23490000, 'con_hang'),

-- 10. SP_ZFOLD5 (Samsung Galaxy Z Fold5)
(10, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Icy'),     'ZF5-256-BLU',    12, 256, 34990000, 'con_hang'),
(10, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Phantom'),  'ZF5-256-BLK',    12, 256, 34990000, 'con_hang'),
(10, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Phantom'),  'ZF5-512-BLK',    12, 512, 38990000, 'con_hang'),

-- 11. SP_ZFLIP5 (Samsung Galaxy Z Flip5)
(11, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Mint'),    'ZFL5-256-MNT',   8, 256, 17990000, 'con_hang'),
(11, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Lavender'), 'ZFL5-256-PUR',   8, 256, 17990000, 'con_hang'),
(11, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Lavender'), 'ZFL5-512-PUR',   8, 512, 21990000, 'con_hang'),

-- 12. SP_A55 (Samsung Galaxy A55 5G)
(12, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Iceblue'), 'A55-128-ICE',    8, 128, 8990000,  'con_hang'),
(12, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Navy'),     'A55-128-NAV',    8, 128, 8990000,  'con_hang'),
(12, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Navy'),     'A55-256-NAV',    12, 256, 10990000, 'con_hang'),

-- 13. SP_XM14U (Xiaomi 14 Ultra)
(13, (SELECT id FROM mau_sac WHERE ten_mau = N'Trắng Da Nhám'),'XM14U-256-WHT',  12, 256, 29990000, 'con_hang'),
(13, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Da Nhám'),  'XM14U-256-BLK',  12, 256, 29990000, 'con_hang'),
(13, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Da Nhám'),  'XM14U-512-BLK',  12, 512, 32990000, 'het_hang'),

-- 14. SP_XM14 (Xiaomi 14)
(14, (SELECT id FROM mau_sac WHERE ten_mau = N'Xanh Ngọc'),    'XM14-256-GRN',   8, 256, 19990000, 'con_hang'),
(14, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Tuyền'),    'XM14-256-BLK',   8, 256, 19990000, 'con_hang'),
(14, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Tuyền'),    'XM14-512-BLK',   12, 512, 22990000, 'con_hang'),

-- 15. SP_RMN13 (Xiaomi Redmi Note 13 Pro+)
(15, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Aurora'),   'RMN13P-128-PUR', 8, 128, 8490000,  'con_hang'),
(15, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Bán Dạ'),   'RMN13P-128-BLK', 8, 128, 8490000,  'con_hang'),
(15, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Bán Dạ'),   'RMN13P-256-BLK', 12, 256, 10490000, 'con_hang'),

-- 16. SP_FN3 (OPPO Find N3)
(16, (SELECT id FROM mau_sac WHERE ten_mau = N'Vàng Champagne'),'FN3-256-GLD',    12, 256, 36990000, 'con_hang'),
(16, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Cổ Điển'),  'FN3-256-BLK',    12, 256, 36990000, 'con_hang'),
(16, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Cổ Điển'),  'FN3-512-BLK',    12, 512, 39990000, 'con_hang'),

-- 17. SP_RENO11P (OPPO Reno11 Pro 5G)
(17, (SELECT id FROM mau_sac WHERE ten_mau = N'Trắng Ngọc Trai'),'R11P-256-WHT',  8, 256, 12490000, 'con_hang'),
(17, (SELECT id FROM mau_sac WHERE ten_mau = N'Xám Không Gian'),'R11P-256-GRY',  8, 256, 12490000, 'con_hang'),
(17, (SELECT id FROM mau_sac WHERE ten_mau = N'Xám Không Gian'),'R11P-512-GRY',  12, 512, 14990000, 'con_hang'),

-- 18. SP_A79 (OPPO A79 5G)
(18, (SELECT id FROM mau_sac WHERE ten_mau = N'Tím Phát Sáng'), 'A79-128-PUR',   6, 128, 5990000,  'con_hang'),
(18, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Lấp Lánh'),  'A79-128-BLK',   6, 128, 5990000,  'con_hang'),
(18, (SELECT id FROM mau_sac WHERE ten_mau = N'Đen Lấp Lánh'),  'A79-256-BLK',   8, 256, 6990000,  'con_hang');
GO