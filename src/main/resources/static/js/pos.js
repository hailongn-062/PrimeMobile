/**
 * pos.js — POS (Bán hàng tại quầy) JavaScript Logic
 * PrimeMobile Admin · ES6 · Vanilla JS · Fetch API
 *
 * ═══════════════════════════════════════════════════════════
 * ARCHITECTURE:
 *   State  → allProducts[], cart[], promoState{}
 *   Modules (IIFE-style sections):
 *     1. STATE & CONSTANTS
 *     2. DOM UTILITIES
 *     3. CLOCK
 *     4. SIDEBAR TOGGLE
 *     5. LOAD PRODUCTS  (GET /api/admin/pos/san-pham)
 *     6. CART CRUD      (add, remove, changeQty, clear)
 *     7. RENDER CART
 *     8. IMEI MANAGEMENT (chọn từ dropdown, tự động lọc IMEI đã dùng)
 *     9. AUTO-PROMO     (GET /api/admin/pos/tinh-khuyen-mai)
 *    10. SUMMARY UI
 *    11. VALIDATE & CHECKOUT (POST /api/admin/pos/thanh-toan)
 *    12. BILL / INVOICE MODAL
 *    13. KHÁCH HÀNG SEARCH (tùy chọn mở rộng)
 *    14. INIT
 * ═══════════════════════════════════════════════════════════
 */

'use strict';

/* ════════════════════════════════════════════════════════════
   1. STATE & CONSTANTS
════════════════════════════════════════════════════════════ */
let allProducts = [];   // Danh sách sản phẩm từ API (không đổi sau load)
let filteredProducts = []; // Sau khi search filter

/**
 * Cart item structure:
 * {
 *   bienTheId  : Integer,
 *   tenSanPham : String,
 *   maSku      : String,
 *   mauSac     : String,
 *   ramGb      : Integer,
 *   luuTruGb   : Integer,
 *   soLuong    : Integer (>= 1),
 *   donGia     : Number,   // giá tại thời điểm thêm vào giỏ (price snapshot)
 *   imeis      : String    // raw string, cách nhau bằng dấu phẩy
 * }
 */
let cart = [];

/**
 * Promo state — cập nhật mỗi khi gọi API tinh-khuyen-mai
 * {
 *   ctkmId     : Integer | null,
 *   tenCtkm    : String  | null,
 *   giaTriUuDai: Number  (phần trăm),
 *   tienGiam   : Number  (VNĐ)
 * }
 */
let promoState = {
    ctkmId: null,
    tenCtkm: null,
    giaTriUuDai: 0,
    tienGiam: 0
};

let posKhuyenMais = [];

let promoDebounceTimer = null;  // setTimeout ref cho auto-promo
let isFetchingPromo = false; // Prevent concurrent promo fetches

// API endpoints
const API = {
    SAN_PHAM: '/api/admin/pos/san-pham',
    TINH_KM: '/api/admin/pos/tinh-khuyen-mai',
    THANH_TOAN: '/api/admin/pos/thanh-toan',
    IMEI_DANH_SACH: '/api/admin/imei/danh-sach',
    KHACH_HANG: '/api/admin/khach-hang',
    KHACH_HANG_VANG_LAI: '/api/admin/khach-hang/vang-lai',  // Tạo khách vãng lai
    LUU_DON_CHO: '/api/admin/ban-hang/luu-don-cho',
    DS_DON_CHO: '/api/admin/ban-hang/danh-sach-cho',
    HUY_DON_CHO: '/api/admin/ban-hang/huy-don-cho',
    TIEP_TUC_DON_CHO: '/api/admin/ban-hang/tiep-tuc-don',
};

/**
 * Customer mode — quản lý 2 chế độ chọn khách hàng:
 *   'vang_lai'  → Nhập thông tin khách vãng lai mới
 *   'chon_cu'   → Chọn khách hàng đã có từ dropdown
 */
let customerMode = 'vang_lai'; // vang_lai | chon_cu
let vlKhachHangId = null;  // ID khách vãng lai đã tạo (cache để không tạo lại)

// Hằng số: ID của Kho Tổng (theo dữ liệu mẫu)
const KHO_TONG_ID = 1;

const LOAI_KHO_TONG = 'kho_tong';
const PROMO_DEBOUNCE_MS = 450;
const SEARCH_DEBOUNCE_MS = 220;
const CUSTOMER_SEARCH_DEBOUNCE_MS = 300;

/* ════════════════════════════════════════════════════════════
   2. DOM UTILITIES
════════════════════════════════════════════════════════════ */

const $ = (sel) => document.querySelector(sel);
const $$ = (sel) => document.querySelectorAll(sel);
const el = (id) => document.getElementById(id);

/** Format tiền VNĐ */
const fmt = (n) =>
    new Intl.NumberFormat('vi-VN', {
        style: 'currency',
        currency: 'VND',
        maximumFractionDigits: 0
    }).format(n ?? 0);

/** Debounce factory */
const debounce = (fn, ms) => {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), ms);
    };
};

/** Parse chuỗi IMEI thô → mảng string đã trim, bỏ rỗng */
const parseImeis = (raw = '') =>
    String(raw)
        .split(',')
        .map(s => s.trim())
        .filter(s => s.length > 0);

/** DOM refs — tất cả được resolve sau DOMContentLoaded */
let DOM = {};

function resolveDOM() {
    DOM = {
        inputSearch: el('inputSearch'),
        productGrid: el('product-grid'),
        productStatus: el('product-status'),
        cartItemsWrap: el('cart-items-wrap'),
        cartEmptyMsg: el('cart-empty-msg'),
        cartCount: el('cart-count'),
        btnClearCart: el('btnClearCart'),
        btnCheckout: el('btnCheckout'),
        btnSavePending: el('btnSavePending'),
        btnOpenPendingOrders: el('btnOpenPendingOrders'),
        pendingOrdersCount: el('pendingOrdersCount'),
        pendingOrdersContent: el('pending-orders-content'),
        modalPendingOrders: el('modalPendingOrders'),
        selectKhachHang: el('selectKhachHang'),
        searchCustomer: el('searchCustomer'),
        posClock: el('pos-clock'),
        btnToggleSidebar: el('btnToggleSidebar'),
        // Khách hàng
        tabVangLai: el('tabVangLai'),
        tabChonCu: el('tabChonCu'),
        panelVangLai: el('panelVangLai'),
        panelChonCu: el('panelChonCu'),
        vlHoTen: el('vlHoTen'),
        vlSoDienThoai: el('vlSoDienThoai'),
        vlEmail: el('vlEmail'),
        vlGioiTinh: el('vlGioiTinh'),
        vlHoTenErr: el('vlHoTenErr'),
        vlSdtErr: el('vlSdtErr'),
        vlStatus: el('vlStatus'),
        // Summary
        sumTongTien: el('sum-tong-tien'),
        sumTienGiam: el('sum-tien-giam'),
        sumCanTra: el('sum-can-tra'),
        rowGiam: el('row-giam'),
        rowPromoName: el('row-promo-name'),
        sumPromoName: el('sum-promo-name'),
        sumPromoPct: el('sum-promo-pct'),
        posCtkmSelect: el('posCtkmSelect'),
        // Bill modal
        billContent: el('bill-content'),
        btnPrintBill: el('btnPrintBill'),
        btnNewOrder: el('btnNewOrder'),
    };
}

/* ════════════════════════════════════════════════════════════
   3. CLOCK — cập nhật mỗi giây
════════════════════════════════════════════════════════════ */
function startClock() {
    if (!DOM.posClock) return;
    const tick = () => {
        DOM.posClock.textContent = new Date().toLocaleTimeString('vi-VN', {
            hour: '2-digit', minute: '2-digit', second: '2-digit'
        });
    };
    tick();
    setInterval(tick, 1000);
}

/* ════════════════════════════════════════════════════════════
   4. SIDEBAR TOGGLE
════════════════════════════════════════════════════════════ */
function initSidebar() {
    document.body.classList.add('sidebar-collapsed');
}

/* ════════════════════════════════════════════════════════════
   5. LOAD SẢN PHẨM  →  GET /api/admin/pos/san-pham
════════════════════════════════════════════════════════════ */
async function loadProducts() {
    showProductStatus('loading');

    try {
        const res = await fetch(API.SAN_PHAM, { credentials: 'include' });

        if (!res.ok) {
            const msg = await res.text().catch(() => `HTTP ${res.status}`);
            throw new Error(msg);
        }

        allProducts = await res.json();
        filteredProducts = [...allProducts];
        showProductStatus('hidden');
        renderProducts(filteredProducts);

    } catch (err) {
        console.error('[POS] loadProducts error:', err);
        showProductStatus('error', err.message);
    }
}

/** Hiển thị trạng thái loading / error / empty / hidden */
function showProductStatus(state, msg = '') {
    const s = DOM.productStatus;
    if (!s) return;

    switch (state) {
        case 'loading':
            s.style.display = '';
            s.innerHTML = `
                <div class="spinner-border text-primary" style="width:2rem;height:2rem;" role="status"></div>
                <p class="mt-2 mb-0 text-muted">Đang tải danh sách sản phẩm...</p>`;
            DOM.productGrid.innerHTML = '';
            break;

        case 'error':
            s.style.display = '';
            s.innerHTML = `
                <i class="fa fa-exclamation-triangle text-danger" style="font-size:1.8rem;"></i>
                <p class="mt-2 text-danger">${msg || 'Không thể tải sản phẩm.'}</p>
                <button class="btn btn-sm btn-outline-primary mt-1" onclick="loadProducts()">
                    <i class="fa fa-redo me-1"></i>Thử lại
                </button>`;
            break;

        case 'hidden':
        default:
            s.style.display = 'none';
    }
}

/** Gom nhóm sản phẩm theo sanPhamId */
function groupProducts(list) {
    const groupsMap = new Map();
    list.forEach(p => {
        const id = p.sanPhamId || p.tenSanPham; // Fallback to name if sanPhamId is null somehow
        if (!groupsMap.has(id)) {
            groupsMap.set(id, {
                sanPhamId: p.sanPhamId,
                tenSanPham: p.tenSanPham,
                anhDaiDien: p.anhDaiDien,
                variants: [],
                tonKhoTotal: 0,
                giaTuBao: p.giaBan ?? p.giaGoc,
                giaGocToiThieu: p.giaGoc ?? p.giaBan // Thêm để lưu giá gốc của biến thể rẻ nhất
            });
        }
        const g = groupsMap.get(id);
        g.variants.push(p);
        g.tonKhoTotal += (p.tonKho ?? 0);
        
        const giaBan = p.giaBan ?? p.giaGoc;
        const giaGoc = p.giaGoc ?? p.giaBan;
        if (giaBan < g.giaTuBao) {
            g.giaTuBao = giaBan;
            g.giaGocToiThieu = giaGoc; // Cập nhật lại giá gốc tương ứng khi tìm thấy giá bán rẻ hơn
        }
    });
    return Array.from(groupsMap.values());
}

/** Render danh sách sản phẩm ra grid (sau khi gom nhóm) */
function renderProducts(list) {
    if (!DOM.productGrid) return;

    if (!list || list.length === 0) {
        DOM.productGrid.innerHTML = `
            <div style="grid-column:1/-1;text-align:center;padding:3rem 1rem;color:#6B7280;">
                <i class="fa fa-box-open" style="font-size:2rem;opacity:.35;display:block;margin-bottom:.75rem;"></i>
                Không tìm thấy sản phẩm nào phù hợp.
            </div>`;
        return;
    }

    const grouped = groupProducts(list);
    DOM.productGrid.innerHTML = grouped.map(g => buildGroupCardHtml(g)).join('');
}

/** Tạo HTML cho 1 group card (Layout ngang) */
function buildGroupCardHtml(g) {
    const isOutOfStock = g.tonKhoTotal <= 0;

    const imgFallbackHtml = `<span class="prod-emoji">
            <i class="fa fa-mobile-alt"></i>
        </span>`;
    const imgHtml = g.anhDaiDien
        ? `<img src="${escHtml(g.anhDaiDien)}" alt="${escHtml(g.tenSanPham)}" loading="lazy" referrerpolicy="no-referrer"
               onerror="this.style.display='none';this.nextElementSibling.style.display='inline-flex';">${imgFallbackHtml.replace('class="prod-emoji"', 'class="prod-emoji" style="display:none"')}`
        : imgFallbackHtml;

    // Hiển thị giá gốc gạch ngang nếu có khuyến mãi
    const hasDiscount = g.giaGocToiThieu > g.giaTuBao;
    const priceDisplay = hasDiscount 
        ? `<div style="font-size: .75rem; color: #9CA3AF; text-decoration: line-through; margin-bottom: -2px;">Từ ${fmt(g.giaGocToiThieu)}</div>
           <div class="prod-price-from">Từ ${fmt(g.giaTuBao)}</div>`
        : `<div class="prod-price-from">Từ ${fmt(g.giaTuBao)}</div>`;

    return `
    <div class="prod-card-h${isOutOfStock ? ' out-of-stock' : ''}" data-sp-id="${g.sanPhamId}" title="${escHtml(g.tenSanPham)}">
        <div class="prod-card-h-img">${imgHtml}</div>
        <div class="prod-card-h-body">
            <div class="prod-name">${escHtml(g.tenSanPham)}</div>
            <div class="prod-meta">
                <span class="badge-variants">${g.variants.length} biến thể</span>
                <span class="prod-stock">Tồn: <span>${g.tonKhoTotal}</span> máy</span>
            </div>
            ${priceDisplay}
        </div>
        <button class="btn-add-group" onclick='openVariantDialog(${JSON.stringify(g).replace(/'/g, "&#39;")})'
                ${isOutOfStock ? 'disabled' : ''}>
            <i class="fa fa-plus"></i>
        </button>
    </div>`;
}

/** Popup chọn cấu hình (biến thể) */
async function openVariantDialog(group) {
    // 1. Phân loại biến thể theo dung lượng và màu sắc
    const capacities = [...new Set(group.variants.map(v => v.luuTruGb))].sort((a,b) => a-b);
    
    // Giao diện cấu hình
    const containerId = `variant-picker-${group.sanPhamId || Date.now()}`;
    
    const html = `
        <div id="${containerId}" class="variant-picker-container" style="text-align: left; padding: 0 10px;">
            <div style="margin-bottom: 15px;">
                <label style="font-weight: 700; color: #E5E7EB; margin-bottom: 8px; display: block;">Dung lượng</label>
                <div class="capacity-chips" style="display: flex; gap: 8px; flex-wrap: wrap;">
                    ${capacities.map(cap => `
                        <button type="button" class="chip-btn chip-cap" data-cap="${cap}">
                            ${cap}GB
                        </button>
                    `).join('')}
                </div>
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="font-weight: 700; color: #E5E7EB; margin-bottom: 8px; display: block;">Màu sắc</label>
                <div class="color-chips" style="display: flex; gap: 8px; flex-wrap: wrap;">
                    <!-- Sẽ được fill bằng JS khi chọn dung lượng -->
                </div>
            </div>

            <div class="price-display" style="margin-top: 20px; padding-top: 15px; border-top: 1px solid rgba(255,255,255,0.1);">
                <div style="font-size: 0.85rem; color: #9CA3AF;">Giá sản phẩm</div>
                <div class="price-val" style="font-size: 1.2rem; font-weight: bold; color: #0d9488;">Vui lòng chọn cấu hình</div>
                <div class="stock-val" style="font-size: 0.8rem; color: #10B981; margin-top: 2px;"></div>
                <input type="hidden" id="selectedVariantId" value="">
            </div>
        </div>
        
        <style>
            .chip-btn {
                background: #0d6b5e;
                border: 2px solid rgba(255,255,255,0.1);
                color: #fff;
                padding: 6px 12px;
                border-radius: 20px;
                cursor: pointer;
                font-size: 0.85rem;
                font-weight: 600;
                transition: all 0.2s;
            }
            .chip-btn:hover:not(:disabled) {
                background: #0a5a4e;
                border-color: rgba(255,255,255,0.3);
            }
            .chip-btn.selected {
                background: rgba(59, 130, 246, 0.15);
                border-color: #0d9488;
                color: #60A5FA;
            }
            .chip-btn:disabled {
                opacity: 0.4;
                cursor: not-allowed;
            }
            .color-dot {
                display: inline-block;
                width: 12px;
                height: 12px;
                border-radius: 50%;
                margin-right: 6px;
                vertical-align: middle;
                border: 1px solid rgba(255,255,255,0.2);
            }
        </style>
    `;

    const { value: selectedBienTheId } = await Swal.fire({
        title: `Cấu hình ${escHtml(group.tenSanPham)}`,
        html: html,
        width: 500,
        showCancelButton: true,
        cancelButtonText: 'Hủy',
        confirmButtonText: 'Tiếp theo &rarr;',
        customClass: {
            confirmButton: 'btn btn-primary',
            cancelButton: 'btn btn-secondary'
        },
        didOpen: () => {
            const container = document.getElementById(containerId);
            const capChips = container.querySelectorAll('.chip-cap');
            const colorContainer = container.querySelector('.color-chips');
            const priceVal = container.querySelector('.price-val');
            const stockVal = container.querySelector('.stock-val');
            const selectedVariantIdInput = container.querySelector('#selectedVariantId');
            
            let selectedCap = null;
            let selectedColor = null;

            // Hàm cập nhật màu sắc khả dụng
            const updateColors = () => {
                if (!selectedCap) return;
                
                // Lọc biến thể theo dung lượng
                const availableVariants = group.variants.filter(v => v.luuTruGb === selectedCap);
                const colors = [...new Set(availableVariants.map(v => v.mauSac))];
                
                // Render chips màu
                colorContainer.innerHTML = colors.map(color => {
                    const variant = availableVariants.find(v => v.mauSac === color);
                    const isOutOfStock = (variant.tonKho ?? 0) <= 0;
                    
                    // Simple color mapping for dot
                    let dotColor = '#9CA3AF';
                    const cLow = color.toLowerCase();
                    if(cLow.includes('đen') || cLow.includes('black')) dotColor = '#000000';
                    else if(cLow.includes('trắng') || cLow.includes('white')) dotColor = '#FFFFFF';
                    else if(cLow.includes('xanh') || cLow.includes('blue')) dotColor = '#0d9488';
                    else if(cLow.includes('vàng') || cLow.includes('gold')) dotColor = '#F59E0B';
                    else if(cLow.includes('đỏ') || cLow.includes('red')) dotColor = '#EF4444';
                    else if(cLow.includes('tím') || cLow.includes('purple')) dotColor = '#8B5CF6';
                    else if(cLow.includes('hồng') || cLow.includes('pink')) dotColor = '#EC4899';
                    else if(cLow.includes('xám') || cLow.includes('grey') || cLow.includes('gray') || cLow.includes('titan')) dotColor = '#6B7280';
                    
                    return `
                        <button type="button" class="chip-btn chip-color" data-color="${escHtml(color)}" ${isOutOfStock ? 'disabled' : ''}>
                            <span class="color-dot" style="background-color: ${dotColor};"></span>${escHtml(color)}
                        </button>
                    `;
                }).join('');
                
                // Gán event cho các chip màu mới
                container.querySelectorAll('.chip-color').forEach(btn => {
                    btn.addEventListener('click', (e) => {
                        if (btn.disabled) return;
                        
                        // Cập nhật UI màu
                        container.querySelectorAll('.chip-color').forEach(b => b.classList.remove('selected'));
                        btn.classList.add('selected');
                        
                        selectedColor = btn.getAttribute('data-color');
                        updatePrice();
                    });
                });
                
                // Reset chọn màu nếu màu đang chọn không có trong dung lượng mới
                if (selectedColor && !colors.includes(selectedColor)) {
                    selectedColor = null;
                }
                
                // Nếu chọn màu khả dụng thì re-select
                if (selectedColor) {
                    const btn = container.querySelector(`.chip-color[data-color="${selectedColor}"]`);
                    if (btn && !btn.disabled) {
                        btn.classList.add('selected');
                    } else {
                        selectedColor = null; // Màu này hết hàng ở dung lượng mới
                    }
                }
                
                updatePrice();
            };

            // Hàm cập nhật giá và ID
            const updatePrice = () => {
                if (selectedCap && selectedColor) {
                    const variant = group.variants.find(v => v.luuTruGb === selectedCap && v.mauSac === selectedColor);
                    if (variant) {
                        const giaGoc = variant.giaGoc ?? variant.giaBan;
                        const giaBan = variant.giaBan ?? giaGoc;
                        
                        if (giaBan < giaGoc) {
                            priceVal.innerHTML = `<span style="font-size: 0.9rem; color: #9CA3AF; text-decoration: line-through; margin-right: 8px; font-weight: normal;">${fmt(giaGoc)}</span>${fmt(giaBan)}`;
                        } else {
                            priceVal.textContent = fmt(giaBan);
                        }
                        
                        stockVal.textContent = `Còn ${variant.tonKho} máy trong kho`;
                        selectedVariantIdInput.value = variant.bienTheId;
                    }
                } else {
                    priceVal.textContent = 'Vui lòng chọn cấu hình';
                    stockVal.textContent = '';
                    selectedVariantIdInput.value = '';
                }
            };

            // Event chọn dung lượng
            capChips.forEach(btn => {
                btn.addEventListener('click', (e) => {
                    // Update UI dung lượng
                    capChips.forEach(b => b.classList.remove('selected'));
                    btn.classList.add('selected');
                    
                    selectedCap = parseInt(btn.getAttribute('data-cap'), 10);
                    updateColors();
                });
            });
            
            // Tự động click dung lượng đầu tiên (nếu có) để mồi
            if (capChips.length > 0) {
                capChips[0].click();
            }
        },
        preConfirm: () => {
            const val = document.getElementById('selectedVariantId')?.value;
            if (!val) {
                Swal.showValidationMessage('Vui lòng chọn đầy đủ dung lượng và màu sắc');
                return false;
            }
            return parseInt(val, 10);
        }
    });

    if (selectedBienTheId) {
        // Proceed to next step: add to cart
        addToCart(selectedBienTheId);
    }
}

/** Escape HTML để tránh XSS trong innerHTML */
function escHtml(str) {
    const d = document.createElement('div');
    d.textContent = str ?? '';
    return d.innerHTML;
}

/* ── Scan IMEI To Cart ──────────────────────────────────────── */
async function scanImeiToCart() {
    // ID dùng cho vùng scanner
    const scannerId = `scanner-inline-${Date.now()}`;
    let isCameraActive = false;

    const html = `
        <div style="text-align:left; margin-bottom:15px;">
            <label style="font-weight:600; color:#E5E7EB; margin-bottom:8px; display:block;">Nhập hoặc quét IMEI</label>
            <div style="display:flex; gap:8px;">
                <input type="text" id="manualImeiInput" class="form-control" placeholder="Ví dụ: 351234567890123" style="flex:1; background:#115e59; color:#fff; border:1px solid rgba(255,255,255,0.1);">
                <button type="button" id="btnManualSearchImei" class="btn btn-primary"><i class="fa fa-search"></i></button>
            </div>
            
            <div style="text-align:center; margin:15px 0;">
                <span style="color:#9CA3AF; font-size:0.85rem;">Hoặc</span>
            </div>

            <div style="text-align:center;">
                <button type="button" id="btnToggleCamera" class="btn btn-outline-primary w-100">
                    <i class="fa fa-camera me-1"></i> Bật Camera Quét Mã
                </button>
            </div>
            
            <div id="${scannerId}" style="width:100%; max-width:400px; margin: 15px auto 0; border-radius:8px; overflow:hidden;"></div>
        </div>
    `;

    let isProcessing = false;

    const handleImei = async (imei) => {
        if (!imei) return false;
        imei = imei.trim();
        
        // Hiện loading trên Swal
        Swal.showLoading();
        
        try {
            // Tra cứu IMEI
            const result = await (typeof BarcodeScanner !== 'undefined' ? BarcodeScanner.lookup(imei) : fetch(`/api/admin/imei/tra-cuu?imei1=${encodeURIComponent(imei)}`).then(r => r.json()));
            
            if (!result.success) {
                Swal.showValidationMessage(result.message || 'Không tìm thấy IMEI.');
                Swal.hideLoading();
                return false;
            }
            if (result.tinhTrang !== 'trong_kho') {
                Swal.showValidationMessage(`IMEI đang ở trạng thái: ${result.tinhTrang}`);
                Swal.hideLoading();
                return false;
            }

            // Kiểm tra xem sản phẩm có trong danh sách đang bán không
            const p = allProducts.find(x => x.bienTheId === result.bienTheSanPhamId);
            if (!p) {
                Swal.showValidationMessage(`Không tìm thấy sản phẩm đang bán khớp với IMEI này.`);
                Swal.hideLoading();
                return false;
            }

            // Kiểm tra xem IMEI đã có trong giỏ hàng chưa
            for (const item of cart) {
                if (item.imeis) {
                    const list = parseImeis(item.imeis);
                    if (list.includes(imei)) {
                        Swal.showValidationMessage(`IMEI ${imei} đã có trong giỏ hàng.`);
                        Swal.hideLoading();
                        return false;
                    }
                }
            }

            // Gọi API giữ IMEI
            const res = await fetch(`/api/admin/pos/giu-imei?imei=${imei}`, { method: 'POST' });
            if (!res.ok) {
                const err = await res.text();
                Swal.showValidationMessage(`Lỗi giữ IMEI: ${err}`);
                Swal.hideLoading();
                return false;
            }

            // Thêm vào giỏ hàng
            let existing = cart.find(x => x.bienTheId === p.bienTheId);
            if (existing) {
                existing.soLuong += 1;
                const list = parseImeis(existing.imeis);
                list.push(imei);
                existing.imeis = list.join(', ');
            } else {
                cart.push({
                    bienTheId: p.bienTheId,
                    tenSanPham: p.tenSanPham,
                    maSku: p.maSku,
                    mauSac: p.mauSac,
                    ramGb: p.ramGb,
                    luuTruGb: p.luuTruGb,
                    soLuong: 1,
                    donGia: p.giaBan ?? p.giaGoc,
                    imeis: imei,
                });
            }

            onCartChanged();
            
            // Dừng camera nếu đang chạy vì đã thành công
            if (isCameraActive && typeof BarcodeScanner !== 'undefined') {
                BarcodeScanner.stop();
                isCameraActive = false;
            }
            
            // Đóng popup và thông báo
            Swal.close();
            
            toastSuccess(`Đã thêm ${escHtml(p.tenSanPham)} (Màu ${escHtml(p.mauSac)}, ${p.luuTruGb}GB)<br><small>IMEI: ${imei}</small>`);
            return true;

        } catch (error) {
            Swal.showValidationMessage('Lỗi hệ thống: ' + error.message);
            Swal.hideLoading();
            return false;
        }
    };

    await Swal.fire({
        title: 'Quét IMEI',
        html: html,
        showConfirmButton: false,
        showCancelButton: true,
        cancelButtonText: 'Đóng',
        didOpen: () => {
            const btnManual = document.getElementById('btnManualSearchImei');
            const inputManual = document.getElementById('manualImeiInput');
            const btnCamera = document.getElementById('btnToggleCamera');
            
            btnManual.addEventListener('click', () => handleImei(inputManual.value));
            inputManual.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') handleImei(inputManual.value);
            });

            if (typeof BarcodeScanner !== 'undefined') {
                btnCamera.addEventListener('click', () => {
                    if (isCameraActive) {
                        BarcodeScanner.stop();
                        isCameraActive = false;
                        btnCamera.innerHTML = '<i class="fa fa-camera me-1"></i> Bật Camera Quét Mã';
                        btnCamera.classList.replace('btn-danger', 'btn-outline-primary');
                    } else {
                        isCameraActive = true;
                        btnCamera.innerHTML = '<i class="fa fa-stop me-1"></i> Dừng Camera';
                        btnCamera.classList.replace('btn-outline-primary', 'btn-danger');
                        BarcodeScanner.openInline(scannerId, async (decodedText) => {
                            if (isCameraActive && !isProcessing) {
                                isProcessing = true;
                                await handleImei(decodedText);
                                setTimeout(() => { isProcessing = false; }, 1500); // Đợi 1.5s trước khi cho phép quét lại để tránh spam lỗi
                            }
                        });
                    }
                });
            } else {
                btnCamera.style.display = 'none'; // Không có module BarcodeScanner
            }
        },
        willClose: () => {
            if (isCameraActive && typeof BarcodeScanner !== 'undefined') {
                BarcodeScanner.stop();
            }
        }
    });
}

/* ── Search realtime ──────────────────────────────────────── */
function initSearch() {
    DOM.inputSearch?.addEventListener('input', debounce(() => {
        const q = (DOM.inputSearch.value ?? '').trim().toLowerCase();

        if (!q) {
            filteredProducts = [...allProducts];
        } else {
            filteredProducts = allProducts.filter(p =>
                p.tenSanPham.toLowerCase().includes(q)
            );
        }

        renderProducts(filteredProducts);
    }, SEARCH_DEBOUNCE_MS));
}

/* ════════════════════════════════════════════════════════════
   6. CART CRUD
════════════════════════════════════════════════════════════ */

/**
 * Lấy danh sách IMEI từ API (trạng thái 'trong_kho') - CHỈ LẤY IMEI Ở KHO TỔNG
 * Trả về mảng các đối tượng IMEI, hoặc throw Error.
 */
async function fetchImeiList(bienTheId) {
    // Chỉ lấy IMEI ở Kho Tổng (id=1) để phục vụ bán hàng offline tại quầy
    const url = `${API.IMEI_DANH_SACH}?bienTheSanPhamId=${bienTheId}&tinhTrang=trong_kho&khoId=${KHO_TONG_ID}`;
    console.log('[POS] fetchImeiList url:', url);
    const res = await fetch(url, { credentials: 'include' });
    if (!res.ok) {
        const errText = await res.text().catch(() => `HTTP ${res.status}`);
        throw new Error(`Lỗi API IMEI (${res.status}): ${errText}`);
    }
    const data = await res.json();
    console.log('[POS] fetchImeiList response:', data);
    if (Array.isArray(data)) {
        return data;
    } else if (data && Array.isArray(data.data)) {
        return data.data;
    } else if (data && Array.isArray(data.content)) {
        return data.content;
    } else {
        throw new Error('Dữ liệu IMEI trả về không đúng định dạng.');
    }
}

/**
 * Lấy danh sách tất cả IMEI đã được chọn trong giỏ (từ tất cả các item).
 * Dùng để lọc ra những IMEI không cho phép chọn lại.
 */
function getSelectedImeis() {
    const allImeis = [];
    cart.forEach(item => {
        const imeis = parseImeis(item.imeis);
        allImeis.push(...imeis);
    });
    return allImeis;
}

/**
 * Mở modal SweetAlert2 để chọn IMEI cho 1 item (lần đầu tiên).
 * Trả về true nếu người dùng chọn thành công, false nếu hủy hoặc không có IMEI.
 */
async function openImeiSelector(item) {
    try {
        const imeiList = await fetchImeiList(item.bienTheId);
        if (!imeiList || imeiList.length === 0) {
            toastWarn(`Không có IMEI nào trong kho cho "${item.tenSanPham}". Vui lòng nhập kho trước.`);
            return false;
        }

        const selectedImeis = getSelectedImeis();
        const available = imeiList.filter(imei => !selectedImeis.includes(imei.imei1));

        const soLuong = 0; // 0 = unlimited
        let html = `
            <div style="margin-bottom:1rem; text-align: left; display: flex; gap: 0.5rem; justify-content: center;">
                <button type="button" class="btn btn-sm btn-primary btn-scan-imei-pos">
                    <i class="fa fa-barcode me-1"></i> Quét IMEI
                </button>
                <button type="button" class="btn btn-sm btn-danger btn-stop-scan-pos" style="display:none;">
                    <i class="fa fa-times me-1"></i> Dừng quét
                </button>
            </div>
            <div id="scanner-area-inline-${item.bienTheId}" style="width:100%; display:none; margin-bottom: 1rem;"></div>
        `;
        html += ImeiPickerUI.buildHtml(item.bienTheId, soLuong, available);

        const { value: imeis } = await Swal.fire({
            title: `Chọn IMEI cho ${item.tenSanPham}`,
            html: html,
            width: 600,
            confirmButtonText: 'Xác nhận',
            cancelButtonText: 'Hủy',
            showCancelButton: true,
            didDestroy: () => {
                if (typeof BarcodeScanner !== 'undefined') BarcodeScanner.stop();
            },
            didOpen: () => {
                const pickerContainer = document.getElementById(`imei-picker-container-${item.bienTheId}`);
                if (pickerContainer && typeof ImeiPickerUI !== 'undefined') {
                    ImeiPickerUI.init(pickerContainer);
                }

                const btnScan = document.querySelector('.btn-scan-imei-pos');
                const btnStop = document.querySelector('.btn-stop-scan-pos');
                const scannerAreaId = `scanner-area-inline-${item.bienTheId}`;
                
                if (btnStop) {
                    btnStop.addEventListener('click', () => {
                        if (typeof BarcodeScanner !== 'undefined') BarcodeScanner.stop();
                        btnScan.style.display = 'inline-block';
                        btnStop.style.display = 'none';
                    });
                }

                if (btnScan && typeof BarcodeScanner !== 'undefined') {
                    btnScan.addEventListener('click', () => {
                        btnScan.style.display = 'none';
                        btnStop.style.display = 'inline-block';
                        BarcodeScanner.openInline(scannerAreaId, async (decodedText) => {
                            try {
                                const result = await BarcodeScanner.lookup(decodedText);
                                if (!result.success) {
                                    Swal.showValidationMessage(result.message);
                                    return;
                                }
                                if (result.tinhTrang !== 'trong_kho') {
                                    Swal.showValidationMessage(`IMEI ${decodedText} đang ở trạng thái: ${result.tinhTrang}`);
                                    return;
                                }
                                if (result.bienTheSanPhamId !== item.bienTheId) {
                                    Swal.showValidationMessage(`IMEI ${decodedText} thuộc sản phẩm ${result.tenSanPham || 'khác'}, không khớp.`);
                                    return;
                                }
                                
                                if (pickerContainer) {
                                    const addResult = pickerContainer.addImei(decodedText);
                                    if (addResult === 'invalid') {
                                        Swal.showValidationMessage(`IMEI ${decodedText} không khả dụng (hoặc đã được chọn trước đó).`);
                                    } else if (addResult === 'exists') {
                                        Swal.showValidationMessage(`IMEI ${decodedText} đã được chọn.`);
                                    } else if (addResult === 'full') {
                                        Swal.showValidationMessage(`Đã chọn đủ số lượng IMEI cần thiết.`);
                                        BarcodeScanner.stop();
                                        btnScan.style.display = 'inline-block';
                                        btnStop.style.display = 'none';
                                    } else if (addResult === 'added') {
                                        Swal.resetValidationMessage();
                                    }
                                }

                            } catch (e) {
                                console.error(e);
                                Swal.showValidationMessage("Lỗi xử lý mã quét.");
                            }
                        });
                    });
                }
            },
            preConfirm: async () => {
                const pickerContainer = document.getElementById(`imei-picker-container-${item.bienTheId}`);
                if (!pickerContainer) return false;
                
                const imeisSelected = pickerContainer.getSelected();
                
                if (imeisSelected.length === 0) {
                    Swal.showValidationMessage('Vui lòng chọn ít nhất 1 IMEI.');
                    return false;
                }
                const unique = new Set(imeisSelected);
                if (unique.size !== imeisSelected.length) {
                    Swal.showValidationMessage('Không được chọn trùng IMEI.');
                    return false;
                }
                
                // Call API to reserve
                for (let imei of imeisSelected) {
                    try {
                        const res = await fetch(`/api/admin/pos/giu-imei?imei=${imei}`, { method: 'POST' });
                        if (!res.ok) {
                            // Undo previous reservations in this batch
                            for (let undoImei of imeisSelected) {
                                if (undoImei === imei) break;
                                await fetch(`/api/admin/pos/nha-imei?imei=${undoImei}`, { method: 'POST' });
                            }
                            const err = await res.text();
                            Swal.showValidationMessage(`Lỗi giữ IMEI ${imei}: ${err}`);
                            return false;
                        }
                    } catch (e) {
                        Swal.showValidationMessage(`Lỗi mạng khi giữ IMEI: ${e.message}`);
                        return false;
                    }
                }
                
                return imeisSelected;
            }
        });

        if (imeis) {
            item.imeis = imeis.join(', ');
            item.soLuong = imeis.length;
            return true;
        }
        return false;

    } catch (error) {
        console.error('[POS] openImeiSelector error:', error);
        toastWarn('Lỗi khi tải danh sách IMEI: ' + error.message);
        return false;
    }
}

/**
 * Mở modal để chọn thêm IMEI cho số lượng tăng thêm (giữ nguyên IMEI cũ).
 * Trả về true nếu chọn thành công, false nếu hủy hoặc không đủ IMEI.
 */
async function selectAdditionalImeis(item, count) {
    try {
        const imeiList = await fetchImeiList(item.bienTheId);
        if (!imeiList || imeiList.length === 0) {
            toastWarn(`Không có IMEI nào trong kho cho "${item.tenSanPham}".`);
            return false;
        }

        const selectedImeis = getSelectedImeis();
        const available = imeiList.filter(imei => !selectedImeis.includes(imei.imei1));

        if (available.length === 0) {
            toastWarn(`Không có IMEI khả dụng nào khác.`);
            return false;
        }

        let html = `
            <div style="margin-bottom:1rem; text-align: left; display: flex; gap: 0.5rem; justify-content: center;">
                <button type="button" class="btn btn-sm btn-primary btn-scan-imei-pos">
                    <i class="fa fa-barcode me-1"></i> Quét IMEI
                </button>
                <button type="button" class="btn btn-sm btn-danger btn-stop-scan-pos" style="display:none;">
                    <i class="fa fa-times me-1"></i> Dừng quét
                </button>
            </div>
            <div id="scanner-area-inline-${item.bienTheId}" style="width:100%; display:none; margin-bottom: 1rem;"></div>
        `;
        html += ImeiPickerUI.buildHtml(item.bienTheId, 0, available);

        const { value: newImeis } = await Swal.fire({
            title: `Chọn thêm IMEI cho ${item.tenSanPham}`,
            html: html,
            width: 600,
            confirmButtonText: 'Xác nhận',
            cancelButtonText: 'Hủy',
            showCancelButton: true,
            didDestroy: () => {
                if (typeof BarcodeScanner !== 'undefined') BarcodeScanner.stop();
            },
            didOpen: () => {
                const pickerContainer = document.getElementById(`imei-picker-container-${item.bienTheId}`);
                if (pickerContainer && typeof ImeiPickerUI !== 'undefined') {
                    ImeiPickerUI.init(pickerContainer);
                }

                const btnScan = document.querySelector('.btn-scan-imei-pos');
                const btnStop = document.querySelector('.btn-stop-scan-pos');
                const scannerAreaId = `scanner-area-inline-${item.bienTheId}`;
                
                if (btnStop) {
                    btnStop.addEventListener('click', () => {
                        if (typeof BarcodeScanner !== 'undefined') BarcodeScanner.stop();
                        btnScan.style.display = 'inline-block';
                        btnStop.style.display = 'none';
                    });
                }

                if (btnScan && typeof BarcodeScanner !== 'undefined') {
                    btnScan.addEventListener('click', () => {
                        btnScan.style.display = 'none';
                        btnStop.style.display = 'inline-block';
                        BarcodeScanner.openInline(scannerAreaId, async (decodedText) => {
                            try {
                                const result = await BarcodeScanner.lookup(decodedText);
                                if (!result.success) {
                                    Swal.showValidationMessage(result.message);
                                    return;
                                }
                                if (result.tinhTrang !== 'trong_kho') {
                                    Swal.showValidationMessage(`IMEI ${decodedText} đang ở trạng thái: ${result.tinhTrang}`);
                                    return;
                                }
                                if (result.bienTheSanPhamId !== item.bienTheId) {
                                    Swal.showValidationMessage(`IMEI ${decodedText} thuộc sản phẩm ${result.tenSanPham || 'khác'}, không khớp.`);
                                    return;
                                }
                                
                                if (pickerContainer) {
                                    const addResult = pickerContainer.addImei(decodedText);
                                    if (addResult === 'invalid') {
                                        Swal.showValidationMessage(`IMEI ${decodedText} không khả dụng (hoặc đã được chọn trước đó).`);
                                    } else if (addResult === 'exists') {
                                        Swal.showValidationMessage(`IMEI ${decodedText} đã được chọn.`);
                                    } else if (addResult === 'full') {
                                        Swal.showValidationMessage(`Đã chọn đủ số lượng IMEI cần thiết.`);
                                        BarcodeScanner.stop();
                                        btnScan.style.display = 'inline-block';
                                        btnStop.style.display = 'none';
                                    } else if (addResult === 'added') {
                                        Swal.resetValidationMessage();
                                    }
                                }

                            } catch (e) {
                                console.error(e);
                                Swal.showValidationMessage("Lỗi xử lý mã quét.");
                            }
                        });
                    });
                }
            },
            preConfirm: async () => {
                const pickerContainer = document.getElementById(`imei-picker-container-${item.bienTheId}`);
                if (!pickerContainer) return false;
                
                const selected = pickerContainer.getSelected();
                
                if (selected.length === 0) {
                    Swal.showValidationMessage('Vui lòng chọn ít nhất 1 IMEI.');
                    return false;
                }
                const unique = new Set(selected);
                if (unique.size !== selected.length) {
                    Swal.showValidationMessage('Không được chọn trùng IMEI.');
                    return false;
                }
                
                // Call API to reserve
                for (let imei of selected) {
                    try {
                        const res = await fetch(`/api/admin/pos/giu-imei?imei=${imei}`, { method: 'POST' });
                        if (!res.ok) {
                            // Undo previous reservations in this batch
                            for (let undoImei of selected) {
                                if (undoImei === imei) break;
                                await fetch(`/api/admin/pos/nha-imei?imei=${undoImei}`, { method: 'POST' });
                            }
                            const err = await res.text();
                            Swal.showValidationMessage(`Lỗi giữ IMEI ${imei}: ${err}`);
                            return false;
                        }
                    } catch (e) {
                        Swal.showValidationMessage(`Lỗi mạng khi giữ IMEI: ${e.message}`);
                        return false;
                    }
                }
                
                return selected;
            }
        });

        if (newImeis) {
            const current = parseImeis(item.imeis);
            const allImeis = [...current, ...newImeis];
            item.imeis = allImeis.join(', ');
            return newImeis.length;
        }
        return false;

    } catch (error) {
        console.error('[POS] selectAdditionalImeis error:', error);
        toastWarn('Lỗi khi tải IMEI: ' + error.message);
        return false;
    }
}

/** Thêm sản phẩm vào giỏ hoặc tăng số lượng nếu đã có */
async function addToCart(bienTheId) {
    const p = allProducts.find(x => x.bienTheId === bienTheId);
    if (!p) return;

    // Lấy giá bán hiện tại (sau khuyến mãi)
    const donGia = p.giaBan ?? p.giaGoc;
    let existing = cart.find(x => x.bienTheId === bienTheId);

    if (existing) {
        if (existing.soLuong >= (p.tonKho ?? 0)) {
            toastWarn(`Tồn kho Kho Tổng cho sản phẩm này chỉ còn ${p.tonKho} máy.`);
            return;
        }
        const addedCount = await selectAdditionalImeis(existing, 1);
        if (addedCount !== false && addedCount > 0) {
            existing.soLuong += addedCount;
            onCartChanged();
        }
        return;
    }

    const newItem = {
        bienTheId: p.bienTheId,
        tenSanPham: p.tenSanPham,
        maSku: p.maSku,
        mauSac: p.mauSac,
        ramGb: p.ramGb,
        luuTruGb: p.luuTruGb,
        soLuong: 1,
        donGia: donGia,
        imeis: '',
    };
    cart.push(newItem);

    const success = await openImeiSelector(newItem);
    if (!success) {
        removeFromCart(bienTheId);
        return;
    }

    flashProductCard(bienTheId, 'success');
    onCartChanged();
}

/** Xoá 1 sản phẩm khỏi giỏ */
async function removeFromCart(bienTheId) {
    const item = cart.find(x => x.bienTheId === bienTheId);
    if (item && item.imeis) {
        const imeiList = parseImeis(item.imeis);
        for (const imei of imeiList) {
            try {
                await fetch(`/api/admin/pos/nha-imei?imei=${imei}`, { method: 'POST' });
            } catch (e) { console.error('Lỗi nhả IMEI', e); }
        }
    }
    cart = cart.filter(x => x.bienTheId !== bienTheId);
    onCartChanged();
}

/**
 * Thay đổi số lượng (+delta).
 * delta = +1 tăng | delta = -1 giảm
 * Nếu soLuong về 0 → hỏi có muốn xoá không
 */
async function changeQty(bienTheId, delta) {
    const item = cart.find(x => x.bienTheId === bienTheId);
    if (!item) return;

    const newQty = item.soLuong + delta;

    if (newQty <= 0) {
        const result = await Swal.fire({
            icon: 'question',
            title: 'Xoá sản phẩm?',
            text: `Bỏ "${item.tenSanPham}" khỏi giỏ hàng?`,
            showCancelButton: true,
            confirmButtonText: 'Xoá',
            cancelButtonText: 'Huỷ',
            confirmButtonColor: '#ef4444',
        });
        if (result.isConfirmed) removeFromCart(bienTheId);
        return;
    }

    const stock = allProducts.find(p => p.bienTheId === bienTheId)?.tonKho ?? 999;
    if (newQty > stock) {
        toastWarn(`Tồn kho chỉ còn ${stock} máy.`);
        return;
    }

    if (delta < 0) {
        const imeiList = parseImeis(item.imeis);
        if (imeiList.length > newQty) {
            if (imeiList.length === 1) {
                // Chỉ có 1 IMEI, xóa luôn không cần hỏi
                const removedImei = imeiList[0];
                item.imeis = "";
                try {
                    fetch(`/api/admin/pos/nha-imei?imei=${removedImei}`, { method: 'POST' });
                } catch (e) { console.error('Lỗi nhả IMEI', e); }
                item.soLuong = newQty;
                onCartChanged();
            } else {
                // Hiện popup cho phép người dùng chọn IMEI để nhả
                let cardsHtml = imeiList.map(imei => `
                    <label class="imei-return-card">
                        <input type="radio" name="returnImeiRadio" value="${imei}">
                        <span class="imei-text">${imei}</span>
                    </label>
                `).join('');

                const result = await Swal.fire({
                    title: '<span style="color:white; font-size: 1.25rem;"><i class="fa fa-trash text-danger me-2"></i>Bỏ IMEI khỏi giỏ hàng</span>',
                    html: `
                        <style>
                            .swal2-popup.dark-imei-popup {
                                border-radius: 16px;
                                padding: 24px 20px 20px;
                            }
                            .dark-imei-popup .swal2-title {
                                margin-bottom: 5px;
                            }
                            .dark-imei-popup .swal2-html-container {
                                margin: 0;
                            }
                            .imei-return-subtitle {
                                color: #9CA3AF;
                                font-size: 0.9rem;
                                margin-bottom: 24px;
                            }
                            .imei-cards-container {
                                text-align: left;
                                display: flex;
                                flex-direction: column;
                                gap: 12px;
                                margin-bottom: 10px;
                            }
                            .imei-return-card {
                                display: flex;
                                align-items: center;
                                gap: 14px;
                                background: #0d6b5e;
                                padding: 16px;
                                border-radius: 12px;
                                border: 1px solid rgba(255,255,255,0.08);
                                cursor: pointer;
                                transition: all 0.2s ease;
                                margin: 0;
                            }
                            .imei-return-card:has(input:checked) {
                                border-color: #0d9488;
                                box-shadow: 0 0 0 1px #0d9488;
                                background: rgba(21, 101, 192, 0.15);
                            }
                            .imei-return-card input[type="radio"] {
                                accent-color: #0d9488;
                                transform: scale(1.3);
                                margin: 0;
                                cursor: pointer;
                            }
                            .imei-return-card .imei-text {
                                color: white;
                                font-family: monospace;
                                font-size: 1.05rem;
                            }
                            .dark-imei-popup .swal2-actions {
                                gap: 12px;
                                width: 100%;
                                margin-top: 24px;
                            }
                            .dark-imei-popup .btn-confirm {
                                flex: 1;
                                background: linear-gradient(135deg, #0d9488 0%, #115e59 100%);
                                color: white;
                                border: none;
                                padding: 12px 0;
                                border-radius: 10px;
                                font-weight: 600;
                            }
                            .dark-imei-popup .btn-cancel {
                                flex: 1;
                                background: #374151;
                                color: #D1D5DB;
                                border: none;
                                padding: 12px 0;
                                border-radius: 10px;
                                font-weight: 600;
                            }
                        </style>
                        <div class="imei-return-subtitle">Chọn thiết bị cần bỏ ra (còn lại sẽ giữ nguyên)</div>
                        <div class="imei-cards-container">
                            ${cardsHtml}
                        </div>
                    `,
                    background: '#115e59',
                    showCancelButton: true,
                    confirmButtonText: 'Xác nhận',
                    cancelButtonText: 'Huỷ',
                    customClass: {
                        popup: 'dark-imei-popup',
                        confirmButton: 'btn-confirm',
                        cancelButton: 'btn-cancel'
                    },
                    buttonsStyling: false,
                    preConfirm: () => {
                        const selected = document.querySelector('input[name="returnImeiRadio"]:checked');
                        if (!selected) {
                            Swal.showValidationMessage('Bạn cần chọn 1 mã IMEI');
                            return false;
                        }
                        return selected.value;
                    }
                });
                
                if (result.isConfirmed && result.value) {
                    const removedImei = result.value;
                    item.imeis = imeiList.filter(i => i !== removedImei).join(', ');
                    try {
                        fetch(`/api/admin/pos/nha-imei?imei=${removedImei}`, { method: 'POST' });
                    } catch (e) { console.error('Lỗi nhả IMEI', e); }
                    item.soLuong = newQty;
                    onCartChanged();
                }
            }
            // Trả về ngay vì onCartChanged đã được gọi trong các nhánh con
            return; 
        }
    }

    if (delta > 0) {
        const addedCount = await selectAdditionalImeis(item, delta);
        if (addedCount !== false && addedCount > 0) {
            item.soLuong += addedCount;
            onCartChanged();
        }
    } else {
        item.soLuong = newQty;
        onCartChanged();
    }
}

/** Xoá toàn bộ giỏ hàng (dùng sau khi confirm) */
function clearCart() {
    // Nhả tất cả IMEI đang giữ
    if (cart.length > 0) {
        navigator.sendBeacon('/api/admin/pos/nha-tat-ca-imei');
    }
    cart = [];
    promoState = { ctkmId: null, tenCtkm: null, giaTriUuDai: 0, tienGiam: 0 };
    resetVangLaiForm(); // Reset form khách vãng lai khi xoá giỏ
    currentPendingOrderId = null; // Reset pending order id
    onCartChanged();
}

/** Hook gọi mỗi khi cart state thay đổi */
function onCartChanged() {
    renderCart();
    updateSummaryUI();
    triggerAutoPromo();
}

/* ════════════════════════════════════════════════════════════
   7. RENDER GIỎ HÀNG
════════════════════════════════════════════════════════════ */
function renderCart() {
    if (!DOM.cartItemsWrap) return;

    const totalQty = cart.reduce((s, x) => s + x.soLuong, 0);
    if (DOM.cartCount) DOM.cartCount.textContent = totalQty;

    if (DOM.btnCheckout) DOM.btnCheckout.disabled = (cart.length === 0);
    if (DOM.btnSavePending) DOM.btnSavePending.disabled = (cart.length === 0);

    if (cart.length === 0) {
        DOM.cartItemsWrap.innerHTML = '';
        if (DOM.cartEmptyMsg) {
            DOM.cartEmptyMsg.style.display = '';
            DOM.cartItemsWrap.appendChild(DOM.cartEmptyMsg);
        }
        return;
    }

    if (DOM.cartEmptyMsg) DOM.cartEmptyMsg.style.display = 'none';

    DOM.cartItemsWrap.innerHTML = cart.map(item => buildCartItemHtml(item)).join('');

    cart.forEach(item => {
        const btn = el(`btn-rechoose-imei-${item.bienTheId}`);
        if (btn) {
            btn.addEventListener('click', () => reopenImeiSelector(item.bienTheId));
        }
    });
}

/** Tạo HTML cho 1 dòng sản phẩm trong giỏ */
function buildCartItemHtml(item) {
    const subtotal = item.donGia * item.soLuong;
    const imeiList = parseImeis(item.imeis);
    const imeiStatus = imeiList.length === item.soLuong ? '✅' : '⚠️';
    const imeiDisplay = imeiList.length > 0 ? imeiList.join(', ') : 'Chưa chọn IMEI';

    return `
    <div class="cart-item" id="cart-item-${item.bienTheId}">
        <div class="cart-item-top">
            <div class="cart-item-info">
                <div class="cart-item-name" title="${escHtml(item.tenSanPham)}">${escHtml(item.tenSanPham)}</div>
                <div class="cart-item-sku">${escHtml(item.maSku)}</div>
                <div class="qty-stepper">
                    <button class="qty-btn" onclick="changeQty(${item.bienTheId}, -1)"
                            title="Giảm số lượng" aria-label="Giảm">−</button>
                    <span class="qty-display" id="qty-${item.bienTheId}">${item.soLuong}</span>
                    <button class="qty-btn" onclick="changeQty(${item.bienTheId}, +1)"
                            title="Tăng số lượng" aria-label="Tăng">+</button>
                    <span class="cart-item-subtotal" id="subtotal-${item.bienTheId}">${fmt(subtotal)}</span>
                </div>
            </div>
            <div style="display:flex;flex-direction:column;align-items:flex-end;gap:.2rem;">
                <div class="cart-item-price">${fmt(item.donGia)}</div>
                <button class="btn-remove-item" onclick="removeFromCart(${item.bienTheId})"
                        title="Xoá sản phẩm" aria-label="Xoá">
                    <i class="fa fa-times-circle"></i>
                </button>
            </div>
        </div>

        <div class="imei-section" role="group" aria-label="Mã IMEI">
            <div class="imei-label">
                <i class="fa fa-barcode" aria-hidden="true"></i>
                Mã IMEI
                <span class="imei-count-indicator ${imeiList.length === item.soLuong ? 'ok' : 'err'}"
                      id="imei-ind-${item.bienTheId}"
                      title="Số IMEI đã chọn / cần chọn">
                    ${imeiList.length}/${item.soLuong}
                </span>
                <span style="font-size:.61rem;color:rgba(255,255,255,.28);font-weight:400;margin-left:.15rem;">
                    ${imeiStatus}
                </span>
            </div>
            <div style="font-size:.75rem;color:var(--pos-text-dim);word-break:break-all;margin-bottom:.2rem;">
                ${imeiDisplay}
            </div>
            <button class="btn-rechoose-imei" id="btn-rechoose-imei-${item.bienTheId}">
                <i class="fa fa-edit"></i> Chọn lại IMEI
            </button>
        </div>
    </div>`;
}

/** Feedback visual khi thêm vào giỏ thành công */
function flashProductCard(bienTheId, type = 'success') {
    const card = DOM.productGrid?.querySelector(`[data-id="${bienTheId}"]`);
    if (!card) return;
    const color = type === 'success' ? '#22c55e' : '#ef4444';
    card.style.transition = 'border-color .1s';
    card.style.borderColor = color;
    setTimeout(() => { card.style.borderColor = ''; }, 700);
}

/* ════════════════════════════════════════════════════════════
   8. IMEI MANAGEMENT — CHỌN LẠI IMEI
════════════════════════════════════════════════════════════ */

/** Mở lại modal chọn IMEI cho một item (dùng trong nút "Chọn lại IMEI") */
async function reopenImeiSelector(bienTheId) {
    const item = cart.find(x => x.bienTheId === bienTheId);
    if (!item) return;
    
    // Nhả các IMEI cũ trước khi chọn lại
    if (item.imeis) {
        const oldImeis = parseImeis(item.imeis);
        for (const imei of oldImeis) {
            try {
                await fetch(`/api/admin/pos/nha-imei?imei=${imei}`, { method: 'POST' });
            } catch (e) { console.error('Lỗi nhả IMEI', e); }
        }
    }
    item.imeis = '';
    
    const success = await openImeiSelector(item);
    if (!success) {
        removeFromCart(bienTheId);
        toastWarn('Không chọn được IMEI, đã xóa sản phẩm khỏi giỏ.');
    } else {
        onCartChanged();
    }
}

/* ════════════════════════════════════════════════════════════
   9. AUTO-PROMO  →  GET /api/admin/pos/tinh-khuyen-mai
════════════════════════════════════════════════════════════ */

/** Trigger debounced promo fetch sau khi cart thay đổi */
function triggerAutoPromo() {
    clearTimeout(promoDebounceTimer);
    promoDebounceTimer = setTimeout(fetchPromo, PROMO_DEBOUNCE_MS);
}

async function loadKhuyenMaiPos() {
    try {
        const res = await fetch('/api/admin/khuyen-mai/don-hang', { credentials: 'include' });
        if (res.ok) {
            posKhuyenMais = await res.json();
            const sel = DOM.posCtkmSelect;
            if(sel) {
                const html = ['<option value="">-- Tự động chọn mã --</option>'];
                posKhuyenMais.forEach(km => {
                    let label = `[Giảm ${km.giaTriUuDai}%`;
                    if (km.giamToiDa) label += ` - Tối đa ${fmt(km.giamToiDa)}`;
                    label += `] ${escHtml(km.tenCtkm)} (Từ ${fmt(km.donHangToiThieu)})`;
                    html.push(`<option value="${km.id}">${label}</option>`);
                });
                sel.innerHTML = html.join('');
            }
        }
    } catch(e) {
        console.error('Lỗi tải mã giảm giá POS', e);
    }
}

/**
 * Gọi API tính khuyến mãi tự động.
 * Chỉ gọi khi cart không rỗng. Nếu tongTien = 0 → reset promoState.
 */
async function fetchPromo() {
    if (isFetchingPromo) return;

    const tongTien = calcTongTien();

    if (tongTien <= 0) {
        promoState = { ctkmId: null, tenCtkm: null, giaTriUuDai: 0, tienGiam: 0 };
        updateSummaryUI();
        return;
    }

    const manualCtkmId = DOM.posCtkmSelect ? Number(DOM.posCtkmSelect.value) : null;
    
    isFetchingPromo = true;

    try {
        let url = `${API.TINH_KM}?tongTien=${encodeURIComponent(tongTien)}`;
        if (manualCtkmId) {
            url += `&ctkmId=${encodeURIComponent(manualCtkmId)}`;
        }
        
        const res = await fetch(url, { credentials: 'include' });

        if (!res.ok) {
            const errData = await res.json().catch(() => null);
            throw new Error(errData?.message || `HTTP ${res.status}`);
        }

        const data = await res.json();

        promoState = {
            ctkmId: data.ctkmId ?? null,
            tenCtkm: data.tenCtkm ?? null,
            giaTriUuDai: Number(data.giaTriUuDai ?? 0),
            tienGiam: Number(data.tienGiam ?? 0),
        };

    } catch (err) {
        if (manualCtkmId) {
            toastWarn(err.message);
            if (DOM.posCtkmSelect) DOM.posCtkmSelect.value = '';
        } else {
            console.warn('[POS] fetchPromo error (silent):', err.message);
        }
        promoState = { ctkmId: null, tenCtkm: null, giaTriUuDai: 0, tienGiam: 0 };
    } finally {
        isFetchingPromo = false;
        updateSummaryUI();
    }
}

/* ════════════════════════════════════════════════════════════
   10. SUMMARY UI
════════════════════════════════════════════════════════════ */

/** Tính tổng tiền hàng từ cart */
function calcTongTien() {
    return cart.reduce((sum, x) => sum + (x.donGia * x.soLuong), 0);
}

/** Cập nhật bảng tổng kết bên phải */
function updateSummaryUI() {
    const tongTien = calcTongTien();
    const tienGiam = promoState.tienGiam ?? 0;
    const canTra = Math.max(0, tongTien - tienGiam);

    if (DOM.sumTongTien) DOM.sumTongTien.textContent = fmt(tongTien);
    if (DOM.sumCanTra) DOM.sumCanTra.textContent = fmt(canTra);

    if (tienGiam > 0 && promoState.ctkmId) {
        if (DOM.sumTienGiam) DOM.sumTienGiam.textContent = `− ${fmt(tienGiam)}`;
        if (DOM.rowGiam) DOM.rowGiam.style.display = '';
        if (DOM.rowPromoName) DOM.rowPromoName.style.display = '';
        if (DOM.sumPromoName) DOM.sumPromoName.textContent = promoState.tenCtkm ?? 'Khuyến mãi';
        if (DOM.sumPromoPct) DOM.sumPromoPct.textContent = `${promoState.giaTriUuDai}%`;
    } else {
        if (DOM.rowGiam) DOM.rowGiam.style.display = 'none';
        if (DOM.rowPromoName) DOM.rowPromoName.style.display = 'none';
    }
}

/* ════════════════════════════════════════════════════════════
   11. VALIDATE & CHECKOUT  →  POST /api/admin/pos/thanh-toan
════════════════════════════════════════════════════════════ */
function initCheckout() {
    DOM.btnCheckout?.addEventListener('click', handleCheckout);
    DOM.btnSavePending?.addEventListener('click', handleSavePending);
    DOM.btnClearCart?.addEventListener('click', handleClearCart);
    if (DOM.posCtkmSelect) {
        DOM.posCtkmSelect.addEventListener('change', triggerAutoPromo);
    }
}

async function handleClearCart() {
    if (!cart.length) return;

    const result = await Swal.fire({
        title: 'Xoá giỏ hàng?',
        text: 'Toàn bộ sản phẩm và IMEI đã chọn sẽ bị xoá.',
        icon: 'warning',
        showCancelButton: true,
        confirmButtonText: '<i class="fa fa-trash me-1"></i>Xoá tất cả',
        cancelButtonText: 'Giữ lại',
        confirmButtonColor: '#ef4444',
    });

    if (result.isConfirmed) clearCart();
}

async function handleCheckout() {
    if (!cart.length) return;

    const imeisOk = validateAllImeis();
    if (!imeisOk) {
        await Swal.fire({
            icon: 'warning',
            title: 'Thiếu hoặc trùng mã IMEI',
            html: `Vui lòng chọn đủ IMEI cho tất cả sản phẩm và đảm bảo không trùng lặp.<br>
                                <small style="color:#6B7280;">
                                Các sản phẩm chưa đủ IMEI sẽ hiển thị dấu ⚠️.
                                </small>`,
            confirmButtonText: 'Kiểm tra lại',
            confirmButtonColor: '#0d9488',
        });
        return;
    }

    const tongTien = calcTongTien();
    const tienGiam = promoState.tienGiam ?? 0;
    const canTra = tongTien - tienGiam;

    const promoRow = tienGiam > 0
        ? `<div style="display:flex;justify-content:space-between;margin:.3rem 0;color:#16a34a;">
               <span>Giảm giá <small>(${promoState.tenCtkm ?? ''})</small>:</span>
               <strong>− ${fmt(tienGiam)}</strong>
           </div>`
        : '';

    const confirm = await Swal.fire({
        icon: 'question',
        title: 'Xác nhận thanh toán?',
        html: `
            <div style="text-align:left;font-size:.88rem;line-height:1.7;">
                <div style="display:flex;justify-content:space-between;margin:.3rem 0;">
                    <span>Tổng tiền hàng:</span><strong>${fmt(tongTien)}</strong>
                </div>
                ${promoRow}
                <hr style="margin:.4rem 0;border-color:#E8EDF5;">
                <div style="display:flex;justify-content:space-between;font-size:1rem;">
                    <strong>Khách cần trả:</strong>
                    <strong style="color:#0d9488;font-size:1.05rem;">${fmt(canTra)}</strong>
                </div>
            </div>`,
        showCancelButton: true,
        confirmButtonText: '<i class="fa fa-check me-1"></i>Xác nhận thanh toán',
        cancelButtonText: 'Huỷ',
        confirmButtonColor: '#0d9488',
        width: '440px',
        focusConfirm: true,
    });

    if (!confirm.isConfirmed) return;

    // ── BƯỚC 3: Xử lý khách hàng theo mode ────────────────
    let khachHangId = null;

    if (customerMode === 'vang_lai') {
        // Validate form khách vãng lai
        if (!validateVangLaiForm()) return;
        // Tạo/lấy khách vãng lai từ API
        const vlResult = await createOrGetVangLai();
        if (!vlResult) return; // Lỗi đã được hiển thị
        khachHangId = vlResult;
    } else if (customerMode === 'chon_cu') {
        khachHangId = DOM.selectKhachHang?.value
            ? parseInt(DOM.selectKhachHang.value, 10)
            : null;
        if (!khachHangId) {
            Swal.fire({
                icon: 'warning',
                title: 'Chưa chọn khách hàng',
                text: 'Vui lòng chọn khách hàng từ danh sách hoặc tạo khách vãng lai.',
                confirmButtonColor: '#F57F17'
            });
            return;
        }
    }


    const payload = {
        donHangId: currentPendingOrderId,
        khachHangId: khachHangId,
        tongTien: tongTien,
        tienGiam: tienGiam,
        ctkmId: promoState.ctkmId,
        chiTiets: cart.map(item => ({
            bienTheId: item.bienTheId,
            soLuong: item.soLuong,
            donGia: item.donGia,
            imeis: parseImeis(item.imeis)
        }))
    };

    setCheckoutLoading(true);

    try {
        const res = await fetch(API.THANH_TOAN, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errText = await res.text().catch(() => `Lỗi server (HTTP ${res.status})`);
            throw new Error(errText || `HTTP ${res.status}`);
        }

        const result = await res.json();
        
        // Cập nhật lại danh sách đơn chờ phòng khi đây là đơn tiếp tục
        updatePendingOrdersCount();

        showBillModal(result, payload);

    } catch (err) {
        console.error('[POS] checkout error:', err);
        Swal.fire({
            icon: 'error',
            title: 'Thanh toán thất bại',
            html: `<div style="font-size:.88rem;text-align:left;">${escHtml(err.message)}</div>`,
            confirmButtonColor: '#0d9488',
        });
    } finally {
        setCheckoutLoading(false);
    }
}

/** Toggle trạng thái loading của nút Thanh toán */
function setCheckoutLoading(isLoading) {
    if (!DOM.btnCheckout) return;
    DOM.btnCheckout.disabled = isLoading;
    DOM.btnCheckout.innerHTML = isLoading
        ? `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Đang xử lý...`
        : `<i class="fa fa-cash-register me-2"></i>Thanh toán &amp; In Bill`;
}

/**
 * Validate tất cả IMEI trong giỏ (đảm bảo đủ và không trùng)
 */
function validateAllImeis() {
    let allOk = true;
    const seen = new Set();
    cart.forEach(item => {
        const imeis = parseImeis(item.imeis);
        if (imeis.length !== item.soLuong) {
            allOk = false;
            return;
        }
        for (const imei of imeis) {
            const norm = imei.toUpperCase().replace(/\s/g, '');
            if (seen.has(norm)) {
                allOk = false;
                break;
            }
            seen.add(norm);
        }
    });
    return allOk;
}

/* ════════════════════════════════════════════════════════════
   12. BILL / INVOICE MODAL
════════════════════════════════════════════════════════════ */

let _lastPayload = null;

let _billJustPaid = false;

function showBillModal(apiResult, payload) {
    _lastPayload = payload;
    _billJustPaid = true;

    const now = new Date().toLocaleString('vi-VN');
    const tongTien = payload.tongTien;
    const tienGiam = payload.tienGiam ?? 0;
    const canTra = tongTien - tienGiam;

    const linesHtml = payload.chiTiets.map(ct => {
        const p = allProducts.find(x => x.bienTheId === ct.bienTheId);
        const name = p
            ? `${escHtml(p.tenSanPham)}<br><small style="color:#6B7280;">${escHtml(p.maSku)}</small>`
            : `SKU #${ct.bienTheId}`;
        const imeiHtml = ct.imeis && ct.imeis.length > 0
            ? `<div style="font-size:.68rem;color:#6B7280;margin-top:.15rem;">
                   IMEI: ${ct.imeis.map(escHtml).join(', ')}
               </div>`
            : '';

        return `
            <div style="display:flex;justify-content:space-between;align-items:flex-start;
                        margin:.3rem 0;gap:.5rem;">
                <div>
                    <div style="font-weight:600;">${name}</div>
                    <div style="font-size:.72rem;color:#6B7280;">
                        SL: ${ct.soLuong} × ${fmt(ct.donGia)}
                    </div>
                    ${imeiHtml}
                </div>
                <div style="font-weight:700;white-space:nowrap;padding-left:.5rem;">
                    ${fmt(ct.donGia * ct.soLuong)}
                </div>
            </div>`;
    }).join('<hr style="margin:.3rem 0;border-color:#E8EDF5;border-width:1px;">');

    const promoLine = tienGiam > 0
        ? `<div style="display:flex;justify-content:space-between;margin:.2rem 0;color:#16a34a;">
               <span>Giảm giá (${escHtml(promoState.tenCtkm ?? '')}):</span>
               <strong>− ${fmt(tienGiam)}</strong>
           </div>`
        : '';

    if (DOM.billContent) {
        DOM.billContent.innerHTML = `
            <div style="text-align:center;margin-bottom:1rem;padding-bottom:.75rem;
                        border-bottom:2px dashed #E8EDF5;">
                <div style="font-size:1.2rem;font-weight:800;color:#0d9488;">📱 PrimeMobile</div>
                <div style="font-size:.72rem;color:#9CA3AF;text-transform:uppercase;
                            letter-spacing:1px;margin:.2rem 0;">Hóa đơn bán hàng tại quầy</div>
                <div style="font-size:.7rem;color:#9CA3AF;">${now}</div>
                <div style="font-size:.75rem;font-weight:700;color:#0d9488;margin-top:.25rem;">
                    Mã ĐH: ${escHtml(apiResult.maDonHang ?? '')}
                </div>
            </div>

            <div style="margin-bottom:.5rem;font-size:.82rem;">
                <strong>Khách hàng:</strong> ${escHtml(apiResult.khachHang ?? 'Khách lẻ')}
            </div>
            <hr style="margin:.5rem 0;border-color:#E8EDF5;">

            ${linesHtml}

            <hr style="margin:.6rem 0;border-top:2px solid #0d9488;">

            <div style="font-size:.83rem;">
                <div style="display:flex;justify-content:space-between;margin:.2rem 0;">
                    <span>Tổng tiền hàng:</span><strong>${fmt(tongTien)}</strong>
                </div>
                ${promoLine}
                <div style="display:flex;justify-content:space-between;margin:.4rem 0;
                            font-size:1rem;font-weight:800;color:#0d9488;
                            padding-top:.3rem;border-top:1px solid #E8EDF5;">
                    <span>Khách thanh toán:</span><span>${fmt(canTra)}</span>
                </div>
            </div>

            <hr style="margin:.65rem 0;border-color:#E8EDF5;">
            <div style="text-align:center;font-size:.7rem;color:#9CA3AF;line-height:1.7;">
                🙏 Cảm ơn quý khách đã mua hàng tại PrimeMobile!<br>
                Bảo hành chính hãng · Đổi trả trong 7 ngày
            </div>`;
    }

    const modal = new bootstrap.Modal(el('modalBill'), { backdrop: 'static' });
    modal.show();
}

function initBillModal() {
    DOM.btnPrintBill?.addEventListener('click', () => {
        const content = DOM.billContent?.innerHTML ?? '';
        const win = window.open('', '_blank', 'width=540,height=720,menubar=no,toolbar=no');
        if (!win) {
            toastWarn('Trình duyệt đã chặn popup. Vui lòng cho phép popup để in hóa đơn.');
            return;
        }
        win.document.write(`<!DOCTYPE html>
<html lang="vi"><head>
<meta charset="UTF-8">
<title>Hóa đơn PrimeMobile</title>
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;600;700;800&display=swap" rel="stylesheet">
<style>
    body { font-family:'Inter',sans-serif; padding:20px 24px; font-size:13px; color:#1A1A2E; }
    @media print { body { margin:0; -webkit-print-color-adjust:exact; } }
</style>
</head><body>${content}</body></html>`);
        win.document.close();
        win.focus();
        setTimeout(() => { win.print(); }, 600);
    });

    DOM.btnNewOrder?.addEventListener('click', () => {
        bootstrap.Modal.getInstance(el('modalBill'))?.hide();
        toastSuccess('Sẵn sàng tạo đơn mới!');
    });

    el('modalBill')?.addEventListener('hidden.bs.modal', () => {
        if (_billJustPaid) {
            clearCart();
            loadProducts();
            _billJustPaid = false;
        }
    });
}

/* ════════════════════════════════════════════════════════════
   13. KHÁCH HÀNG — TAB SWITCHING + SEARCH + VÃNG LAI
════════════════════════════════════════════════════════════ */

/** Khởi tạo 3-tab chọn khách hàng */
function initCustomerTabs() {
    const tabs = [DOM.tabVangLai, DOM.tabChonCu];
    const panels = [DOM.panelVangLai, DOM.panelChonCu];
    const modes = ['vang_lai', 'chon_cu'];

    tabs.forEach((tab, i) => {
        if (!tab) return;
        tab.addEventListener('click', () => {
            // Deactivate all
            tabs.forEach(t => t?.classList.remove('active'));
            panels.forEach(p => p?.classList.remove('active'));
            // Activate selected
            tab.classList.add('active');
            panels[i]?.classList.add('active');
            customerMode = modes[i];
            // Reset vãng lai cache khi chuyển tab
            if (customerMode !== 'vang_lai') {
                vlKhachHangId = null;
                clearVlStatus();
            }
            console.log('[POS] Customer mode →', customerMode);
        });
    });
}

/** Validate form khách vãng lai — return true nếu hợp lệ */
function validateVangLaiForm() {
    let ok = true;

    // Validate họ tên
    const hoTen = DOM.vlHoTen?.value?.trim() || '';
    if (!hoTen) {
        DOM.vlHoTen?.classList.add('error');
        DOM.vlHoTenErr?.classList.add('show');
        ok = false;
    } else {
        DOM.vlHoTen?.classList.remove('error');
        DOM.vlHoTenErr?.classList.remove('show');
    }

    // Validate SĐT
    const sdt = DOM.vlSoDienThoai?.value?.trim() || '';
    if (!sdt) {
        DOM.vlSoDienThoai?.classList.add('error');
        DOM.vlSdtErr?.classList.add('show');
        ok = false;
    } else {
        DOM.vlSoDienThoai?.classList.remove('error');
        DOM.vlSdtErr?.classList.remove('show');
    }

    if (!ok) {
        Swal.fire({
            icon: 'warning',
            title: 'Thiếu thông tin khách hàng',
            text: 'Vui lòng nhập Họ tên và Số điện thoại cho khách vãng lai.',
            confirmButtonColor: '#0d9488',
        });
    }

    return ok;
}

/**
 * Gọi API tạo hoặc lấy khách vãng lai.
 * Trả về khachHangId (Integer) nếu thành công, null nếu lỗi.
 */
async function createOrGetVangLai() {
    // Nếu đã tạo trước đó (cache) → dùng lại
    if (vlKhachHangId) return vlKhachHangId;

    const payload = {
        hoTen: DOM.vlHoTen?.value?.trim(),
        soDienThoai: DOM.vlSoDienThoai?.value?.trim()
    };

    try {
        showVlStatus('info', '<i class="fa fa-spinner fa-spin"></i> Đang lưu thông tin khách...');

        const res = await fetch(API.KHACH_HANG_VANG_LAI, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errText = await res.text().catch(() => `Lỗi server (HTTP ${res.status})`);
            throw new Error(errText);
        }

        const khachHang = await res.json();
        vlKhachHangId = khachHang.id;

        showVlStatus('success',
            `<i class="fa fa-check-circle"></i> Đã lưu: ${escHtml(khachHang.hoTen)} (${escHtml(khachHang.soDienThoai)})`);

        console.log('[POS] Khách vãng lai tạo/lấy thành công — id=', vlKhachHangId);
        return vlKhachHangId;

    } catch (err) {
        console.error('[POS] createOrGetVangLai error:', err);
        showVlStatus('info', '');
        clearVlStatus();
        Swal.fire({
            icon: 'error',
            title: 'Lỗi tạo khách hàng',
            html: `<div style="font-size:.88rem;text-align:left;">${escHtml(err.message)}</div>`,
            confirmButtonColor: '#0d9488',
        });
        return null;
    }
}

/** Hiển thị trạng thái trên form vãng lai */
function showVlStatus(type, html) {
    if (!DOM.vlStatus) return;
    DOM.vlStatus.className = 'vl-status ' + type;
    DOM.vlStatus.innerHTML = html;
}

/** Xóa trạng thái vãng lai */
function clearVlStatus() {
    if (!DOM.vlStatus) return;
    DOM.vlStatus.className = 'vl-status';
    DOM.vlStatus.innerHTML = '';
    DOM.vlStatus.style.display = 'none';
}

/** Reset form vãng lai về trạng thái ban đầu */
function resetVangLaiForm() {
    if (DOM.vlHoTen) DOM.vlHoTen.value = '';
    if (DOM.vlSoDienThoai) DOM.vlSoDienThoai.value = '';
    if (DOM.vlEmail) DOM.vlEmail.value = '';
    if (DOM.vlGioiTinh) DOM.vlGioiTinh.value = '';
    DOM.vlHoTen?.classList.remove('error');
    DOM.vlSoDienThoai?.classList.remove('error');
    DOM.vlHoTenErr?.classList.remove('show');
    DOM.vlSdtErr?.classList.remove('show');
    vlKhachHangId = null;
    clearVlStatus();
}

/** Tải danh sách khách hàng từ API (có từ khóa tìm kiếm) */
async function loadCustomers(keyword = '') {
    try {
        const trimmed = keyword.trim();
        const url = trimmed
            ? `${API.KHACH_HANG}?tuKhoa=${encodeURIComponent(trimmed)}`
            : API.KHACH_HANG;
        const res = await fetch(url, { credentials: 'include' });
        if (!res.ok) throw new Error('Không thể tải danh sách khách hàng');
        const customers = await res.json();
        populateCustomerDropdown(customers);
    } catch (err) {
        console.warn('[POS] loadCustomers error:', err);
    }
}

/** Đổ dữ liệu khách hàng vào dropdown select */
function populateCustomerDropdown(customers) {
    const select = DOM.selectKhachHang;
    if (!select) return;
    select.innerHTML = '<option value="">-- Chọn khách hàng --</option>';
    if (Array.isArray(customers)) {
        customers.forEach(c => {
            const opt = document.createElement('option');
            opt.value = c.id;
            const display = `${c.hoTen} (${c.soDienThoai || c.email || 'Không có SĐT'})`;
            opt.textContent = display;
            select.appendChild(opt);
        });
    }
}

/** Khởi tạo tìm kiếm khách hàng với debounce */
function initCustomerSearch() {
    const input = DOM.searchCustomer;
    if (!input) return;
    input.addEventListener('input', debounce((e) => {
        const keyword = e.target.value.trim();
        loadCustomers(keyword);
    }, CUSTOMER_SEARCH_DEBOUNCE_MS));

    // Clear validation errors khi user bắt đầu gõ vào form vãng lai
    DOM.vlHoTen?.addEventListener('input', () => {
        DOM.vlHoTen?.classList.remove('error');
        DOM.vlHoTenErr?.classList.remove('show');
        vlKhachHangId = null; // Reset cache vì thông tin thay đổi
    });
    DOM.vlSoDienThoai?.addEventListener('input', () => {
        DOM.vlSoDienThoai?.classList.remove('error');
        DOM.vlSdtErr?.classList.remove('show');
        vlKhachHangId = null;
    });
}

/* ════════════════════════════════════════════════════════════
   13. TOAST SHORTCUTS
════════════════════════════════════════════════════════════ */
function toastSuccess(msg) {
    Swal.fire({
        icon: 'success', text: msg, toast: true, position: 'top-end',
        showConfirmButton: false, timer: 2000, timerProgressBar: true
    });
}

function toastWarn(msg) {
    Swal.fire({
        icon: 'warning', text: msg, toast: true, position: 'top-end',
        showConfirmButton: false, timer: 3000, timerProgressBar: true
    });
}

/* ════════════════════════════════════════════════════════════
   15. ĐƠN HÀNG CHỜ
════════════════════════════════════════════════════════════ */

// Lưu orderId đang được mở (nếu là tiếp tục đơn chờ)
let currentPendingOrderId = null;

async function handleSavePending() {
    if (!cart.length) return;

    const imeisOk = validateAllImeis();
    if (!imeisOk) {
        await Swal.fire({
            icon: 'warning',
            title: 'Thiếu hoặc trùng mã IMEI',
            html: `Vui lòng chọn đủ IMEI cho tất cả sản phẩm và đảm bảo không trùng lặp để lưu đơn chờ.`,
            confirmButtonText: 'Kiểm tra lại',
            confirmButtonColor: '#F57F17',
        });
        return;
    }

    const tongTien = calcTongTien();
    const tienGiam = promoState.tienGiam ?? 0;

    let khachHangId = null;
    if (customerMode === 'vang_lai') {
        if (!validateVangLaiForm()) return;
        const vlResult = await createOrGetVangLai();
        if (!vlResult) return;
        khachHangId = vlResult;
    } else if (customerMode === 'chon_cu') {
        khachHangId = DOM.selectKhachHang?.value ? parseInt(DOM.selectKhachHang.value, 10) : null;
        if (!khachHangId) {
            Swal.fire({
                icon: 'warning',
                title: 'Chưa chọn khách hàng',
                text: 'Vui lòng chọn khách hàng từ danh sách hoặc tạo khách vãng lai.',
                confirmButtonColor: '#F57F17'
            });
            setSavePendingLoading(false);
            return;
        }
    }

    const payload = {
        donHangId: currentPendingOrderId, // Null nếu là đơn chờ mới
        khachHangId: khachHangId,
        tongTien: tongTien,
        tienGiam: tienGiam,
        ctkmId: promoState.ctkmId,
        chiTiets: cart.map(item => ({
            bienTheId: item.bienTheId,
            soLuong: item.soLuong,
            donGia: item.donGia,
            imeis: parseImeis(item.imeis)
        }))
    };

    setSavePendingLoading(true);

    try {
        const res = await fetch(API.LUU_DON_CHO, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify(payload)
        });

        if (!res.ok) {
            const errText = await res.text().catch(() => `Lỗi server (HTTP ${res.status})`);
            throw new Error(errText || `HTTP ${res.status}`);
        }

        toastSuccess('Đã lưu đơn hàng chờ thành công!');
        clearCart();
        loadProducts(); // Reload kho
        currentPendingOrderId = null;
        updatePendingOrdersCount();
    } catch (err) {
        console.error('[POS] save pending error:', err);
        Swal.fire({
            icon: 'error',
            title: 'Lỗi lưu đơn chờ',
            html: `<div style="font-size:.88rem;text-align:left;">${escHtml(err.message)}</div>`,
            confirmButtonColor: '#F57F17',
        });
    } finally {
        setSavePendingLoading(false);
    }
}

function setSavePendingLoading(isLoading) {
    if (!DOM.btnSavePending) return;
    DOM.btnSavePending.disabled = isLoading;
    DOM.btnSavePending.innerHTML = isLoading
        ? `<span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>Lưu...`
        : `<i class="fa fa-pause"></i> Lưu đơn chờ`;
}

async function updatePendingOrdersCount() {
    try {
        const res = await fetch(API.DS_DON_CHO, { credentials: 'include' });
        if (!res.ok) throw new Error('Không thể tải DS đơn chờ');
        const orders = await res.json();
        if (DOM.pendingOrdersCount) {
            DOM.pendingOrdersCount.textContent = orders.length;
        }
    } catch (e) {
        console.warn(e);
    }
}

async function loadPendingOrders() {
    if (!DOM.pendingOrdersContent) return;
    DOM.pendingOrdersContent.innerHTML = `
        <div class="text-center py-4 text-muted">
            <div class="spinner-border text-warning" role="status"></div>
            <p class="mt-2 mb-0">Đang tải...</p>
        </div>`;
        
    try {
        const res = await fetch(API.DS_DON_CHO, { credentials: 'include' });
        if (!res.ok) throw new Error('Không thể tải DS đơn chờ');
        const orders = await res.json();
        
        if (DOM.pendingOrdersCount) {
            DOM.pendingOrdersCount.textContent = orders.length;
        }

        if (orders.length === 0) {
            DOM.pendingOrdersContent.innerHTML = `
                <div class="text-center py-5 text-muted">
                    <i class="fa fa-clipboard-list" style="font-size: 3rem; opacity: 0.3;"></i>
                    <p class="mt-3">Không có hóa đơn chờ nào.</p>
                </div>`;
            return;
        }

        DOM.pendingOrdersContent.innerHTML = orders.map(o => {
            const timeStr = new Date(o.ngayDat).toLocaleString('vi-VN');
            const total = fmt(o.tongTien - (o.tienGiam || 0));
            return `
            <div class="card mb-3 shadow-sm border-0" style="border-radius: 12px; overflow: hidden;">
                <div class="card-header bg-white d-flex justify-content-between align-items-center py-2" style="border-bottom: 1px dashed #E8EDF5;">
                    <div>
                        <strong class="text-primary">#${o.maDonHang}</strong>
                        <span class="text-muted ms-2" style="font-size: .8rem;"><i class="fa fa-clock me-1"></i>${timeStr}</span>
                    </div>
                    <div class="fw-bold text-success">${total}</div>
                </div>
                <div class="card-body py-2">
                    <div class="d-flex justify-content-between align-items-end">
                        <div>
                            <div style="font-size:.9rem;"><i class="fa fa-user me-2 text-muted"></i><strong>${escHtml(o.tenKhachHang || 'Khách lẻ')}</strong></div>
                            <div style="font-size:.85rem; color:#6B7280;"><i class="fa fa-phone-alt me-2"></i>${escHtml(o.soDienThoaiKhachHang || '---')}</div>
                        </div>
                        <div class="mt-2 text-muted" style="font-size: .85rem;">
                            ${(o.chiTiets && o.chiTiets.length > 0) ? o.chiTiets.map(ct => {
                                const bt = ct.bienTheSanPham;
                                return `<div>- ${escHtml(bt?.sanPham?.tenSanPham || '')} ${escHtml(bt?.ramGb || '')}GB/${escHtml(bt?.luuTruGb || '')}GB ${escHtml(bt?.mauSac || '')} (x${ct.soLuong})</div>`;
                            }).join('') : '<em>Không có sản phẩm</em>'}
                        </div>
                    </div>
                    <div class="d-flex justify-content-end align-items-end mt-2">
                        <div class="d-flex gap-2">
                            <button class="btn btn-outline-danger btn-sm" onclick="cancelPendingOrder(${o.id})" style="border-radius: 8px;">
                                <i class="fa fa-times me-1"></i> Hủy đơn
                            </button>
                            <button class="btn btn-warning btn-sm text-white fw-bold" onclick="continuePendingOrder(${o.id})" style="border-radius: 8px;">
                                Tiếp tục <i class="fa fa-arrow-right ms-1"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>`;
        }).join('');
    } catch (err) {
        DOM.pendingOrdersContent.innerHTML = `
            <div class="text-center py-4 text-danger">
                <i class="fa fa-exclamation-triangle fa-2x"></i>
                <p class="mt-2 mb-0">${err.message}</p>
            </div>`;
    }
}

async function cancelPendingOrder(id) {
    // Tạm thời đóng Bootstrap Modal để tránh Focus Trap khóa input của SweetAlert2
    const modalEl = document.getElementById('modalPendingOrders');
    const bsModal = bootstrap.Modal.getInstance(modalEl);
    if (bsModal) bsModal.hide();

    const { value: reason, isDismissed } = await Swal.fire({
        title: 'Hủy đơn hàng chờ?',
        input: 'text',
        inputLabel: 'Lý do hủy',
        inputPlaceholder: 'Khách đổi ý, v.v...',
        showCancelButton: true,
        confirmButtonColor: '#ef4444',
        confirmButtonText: 'Xác nhận hủy',
        cancelButtonText: 'Đóng',
        inputValidator: (value) => {
            if (!value) return 'Vui lòng nhập lý do hủy';
        }
    });

    // Nếu người dùng bấm "Đóng" (hủy thao tác), thì mở lại modal chờ
    if (isDismissed) {
        if (bsModal) bsModal.show();
        return;
    }

    if (reason) {
        try {
            const res = await fetch(`${API.HUY_DON_CHO}?donHangId=${id}&lyDoHuy=${encodeURIComponent(reason)}`, {
                method: 'POST',
                credentials: 'include'
            });
            if (!res.ok) {
                const errText = await res.text().catch(() => `HTTP ${res.status}`);
                throw new Error(errText);
            }
            toastSuccess('Đã hủy đơn hàng chờ và giải phóng IMEI.');
            loadPendingOrders(); // Reload the list
            loadProducts(); // Reload stock
        } catch (err) {
            Swal.fire('Lỗi', err.message, 'error');
        }
    }
}

async function continuePendingOrder(id) {
    if (cart.length > 0) {
        const confirm = await Swal.fire({
            title: 'Ghi đè giỏ hàng?',
            text: 'Bạn đang có sản phẩm trong giỏ. Tiếp tục đơn chờ sẽ xóa giỏ hàng hiện tại. Bạn có chắc chắn?',
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Đồng ý',
            cancelButtonText: 'Hủy'
        });
        if (!confirm.isConfirmed) return;
    }

    try {
        const res = await fetch(`${API.TIEP_TUC_DON_CHO}?donHangId=${id}`, {
            method: 'POST',
            credentials: 'include'
        });
        if (!res.ok) {
            const errText = await res.text().catch(() => `HTTP ${res.status}`);
            throw new Error(errText);
        }
        
        const data = await res.json();
        
        // Đóng modal
        bootstrap.Modal.getInstance(DOM.modalPendingOrders)?.hide();
        
        // Khôi phục giỏ hàng
        clearCart(); // Reset trước
        currentPendingOrderId = id;
        
        // Khôi phục giỏ
        data.chiTiets.forEach(ct => {
            const item = {
                bienTheId: ct.bienTheSanPham.id,
                tenSanPham: ct.bienTheSanPham.sanPham.tenSanPham,
                maSku: ct.bienTheSanPham.sku,
                mauSac: ct.bienTheSanPham.mauSac,
                ramGb: ct.bienTheSanPham.ramGb,
                luuTruGb: ct.bienTheSanPham.luuTruGb,
                soLuong: ct.soLuong,
                donGia: ct.donGia,
                imeis: ct.danhSachImeiDaBan ? ct.danhSachImeiDaBan.map(i => i.imei1).join(', ') : ''
            };
            cart.push(item);
        });

        // Khôi phục thông tin khách hàng
        if (data.khachHang) {
            if (data.khachHang.tenKhachHang === 'Khách vãng lai' || !data.khachHang.id) {
                // Khách vãng lai
                DOM.tabVangLai.click();
                if (DOM.vlHoTen) DOM.vlHoTen.value = data.tenNguoiNhan || '';
                if (DOM.vlSoDienThoai) DOM.vlSoDienThoai.value = data.soDienThoaiNguoiNhan || '';
                // Simulate focus out or validation to trigger state
            } else {
                // Khách cũ
                DOM.tabChonCu.click();
                if (DOM.selectKhachHang) {
                    DOM.selectKhachHang.value = data.khachHang.id;
                }
            }
        } else {
            DOM.tabKhachLe.click();
        }

        // Kích hoạt auto-promo lại
        onCartChanged();
        toastSuccess('Đã khôi phục đơn hàng chờ!');

    } catch (err) {
        Swal.fire({
            icon: 'error',
            title: 'Lỗi',
            html: `<div style="text-align:left;">${escHtml(err.message)}</div>`
        });
    }
}

function initPendingOrders() {
    DOM.btnOpenPendingOrders?.addEventListener('click', () => {
        loadPendingOrders();
        const modal = bootstrap.Modal.getOrCreateInstance(DOM.modalPendingOrders);
        modal.show();
    });
}

/* ════════════════════════════════════════════════════════════
   16. INIT — DOMContentLoaded
════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    resolveDOM();
    startClock();
    initSidebar();
    initSearch();
    initCheckout();
    initBillModal();
    initCustomerTabs();       // Khởi tạo 3-tab khách hàng
    initCustomerSearch();     // Tìm kiếm khách hàng + validate vãng lai
    initPendingOrders();      // Khởi tạo các event cho Đơn chờ
    updatePendingOrdersCount(); // Cập nhật số lượng đơn chờ trên topbar
    loadProducts();
    loadCustomers();          // Load danh sách khách hàng mặc định
    loadKhuyenMaiPos();

    document.getElementById('btnScanImeiPos')?.addEventListener('click', scanImeiToCart);
});

/* Expose tới onclick="" attributes trong HTML */
window.addToCart = addToCart;
window.removeFromCart = removeFromCart;
window.changeQty = changeQty;
window.loadProducts = loadProducts;
window.addEventListener('beforeunload', () => { if (cart.length > 0) navigator.sendBeacon('/api/admin/pos/nha-tat-ca-imei'); });


