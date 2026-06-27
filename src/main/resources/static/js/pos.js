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
 *     8. IMEI MANAGEMENT
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

let promoDebounceTimer = null;  // setTimeout ref cho auto-promo
let isFetchingPromo = false; // Prevent concurrent promo fetches

// API endpoints
const API = {
    SAN_PHAM: '/api/admin/pos/san-pham',
    TINH_KM: '/api/admin/pos/tinh-khuyen-mai',
    THANH_TOAN: '/api/admin/pos/thanh-toan',
};

const LOAI_KHO_TONG = 'kho_tong';
const PROMO_DEBOUNCE_MS = 450;   // ms chờ sau khi cart thay đổi trước khi gọi promo API
const SEARCH_DEBOUNCE_MS = 220;   // ms debounce cho search input

/* ════════════════════════════════════════════════════════════
   2. DOM UTILITIES
════════════════════════════════════════════════════════════ */

/** Lấy element an toàn — trả về null thay vì throw nếu không tìm thấy */
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
        selectKhachHang: el('selectKhachHang'),
        posClock: el('pos-clock'),
        btnToggleSidebar: el('btnToggleSidebar'),
        // Summary
        sumTongTien: el('sum-tong-tien'),
        sumTienGiam: el('sum-tien-giam'),
        sumCanTra: el('sum-can-tra'),
        rowGiam: el('row-giam'),
        rowPromoName: el('row-promo-name'),
        sumPromoName: el('sum-promo-name'),
        sumPromoPct: el('sum-promo-pct'),
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
    // Mặc định collapse sidebar để tối đa diện tích POS
    document.body.classList.add('pos-sidebar-mini');

    DOM.btnToggleSidebar?.addEventListener('click', () => {
        document.body.classList.toggle('pos-sidebar-mini');
    });
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

/** Render danh sách sản phẩm ra grid */
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

    DOM.productGrid.innerHTML = list.map(p => buildProductCardHtml(p)).join('');
}

/** Tạo HTML cho 1 product card */
function buildProductCardHtml(p) {
    const donGia = p.giaKhuyenMai ?? p.giaBan;
    const isOnSale = p.giaKhuyenMai != null;
    const isOutOfStock = (p.tonKho ?? 0) <= 0;

    const imgHtml = p.anhDaiDien
        ? `<img src="${p.anhDaiDien}" alt="${escHtml(p.tenSanPham)}" loading="lazy"
               style="max-height:100%;max-width:100%;object-fit:contain;">`
        : `<span style="font-size:2.8rem;">📱</span>`;

    const saleStrike = isOnSale
        ? `<div style="font-size:.68rem;color:#9CA3AF;text-decoration:line-through;">${fmt(p.giaBan)}</div>`
        : '';

    const stockLabel = isOutOfStock
        ? `<div class="prod-stock">Hết hàng</div>`
        : `<div class="prod-stock">Tồn: <span>${p.tonKho}</span> máy</div>`;

    return `
    <div class="prod-card${isOutOfStock ? ' out-of-stock' : ''}" data-id="${p.bienTheId}" title="${escHtml(p.tenSanPham)}">
        <div class="prod-img">${imgHtml}</div>
        <div class="prod-body">
            <div class="prod-name">${escHtml(p.tenSanPham)}</div>
            <div class="prod-sku">${escHtml(p.maSku)} · ${p.ramGb}GB · ${p.luuTruGb}GB</div>
            ${saleStrike}
            <div class="prod-price${isOnSale ? ' prod-price-sale' : ''}">${fmt(donGia)}</div>
            ${stockLabel}
        </div>
        <button class="btn-add" onclick="addToCart(${p.bienTheId})"
                ${isOutOfStock ? 'disabled' : ''}>
            <i class="fa fa-plus me-1"></i>Thêm vào giỏ
        </button>
    </div>`;
}

/** Escape HTML để tránh XSS trong innerHTML */
function escHtml(str) {
    const d = document.createElement('div');
    d.textContent = str ?? '';
    return d.innerHTML;
}

/* ── Search realtime ──────────────────────────────────────── */
function initSearch() {
    DOM.inputSearch?.addEventListener('input', debounce(() => {
        const q = (DOM.inputSearch.value ?? '').trim().toLowerCase();

        if (!q) {
            filteredProducts = [...allProducts];
        } else {
            filteredProducts = allProducts.filter(p =>
                p.tenSanPham.toLowerCase().includes(q) ||
                p.maSku.toLowerCase().includes(q) ||
                (p.mauSac ?? '').toLowerCase().includes(q)
            );
        }

        renderProducts(filteredProducts);
    }, SEARCH_DEBOUNCE_MS));
}

/* ════════════════════════════════════════════════════════════
   6. CART CRUD
════════════════════════════════════════════════════════════ */

/** Thêm sản phẩm vào giỏ hoặc tăng số lượng nếu đã có */
function addToCart(bienTheId) {
    const p = allProducts.find(x => x.bienTheId === bienTheId);
    if (!p) return;

    const donGia = p.giaKhuyenMai ?? p.giaBan;
    const existing = cart.find(x => x.bienTheId === bienTheId);

    if (existing) {
        // Kiểm tra không vượt quá tồn kho
        if (existing.soLuong >= (p.tonKho ?? 0)) {
            Swal.fire({
                icon: 'warning',
                title: 'Không đủ hàng',
                text: `Tồn kho Kho Tổng cho sản phẩm này chỉ còn ${p.tonKho} máy.`,
                confirmButtonColor: '#1565C0',
                timer: 2500,
                showConfirmButton: false,
                toast: true,
                position: 'top-end',
            });
            return;
        }
        existing.soLuong++;
    } else {
        cart.push({
            bienTheId: p.bienTheId,
            tenSanPham: p.tenSanPham,
            maSku: p.maSku,
            mauSac: p.mauSac,
            ramGb: p.ramGb,
            luuTruGb: p.luuTruGb,
            soLuong: 1,
            donGia: donGia,
            imeis: ''
        });
    }

    onCartChanged();
    flashProductCard(bienTheId, 'success');
}

/** Xoá 1 sản phẩm khỏi giỏ */
function removeFromCart(bienTheId) {
    cart = cart.filter(x => x.bienTheId !== bienTheId);
    onCartChanged();
}

/**
 * Thay đổi số lượng (+delta).
 * delta = +1 tăng | delta = -1 giảm
 * Nếu soLuong về 0 → hỏi có muốn xoá không
 */
function changeQty(bienTheId, delta) {
    const item = cart.find(x => x.bienTheId === bienTheId);
    if (!item) return;

    const newQty = item.soLuong + delta;

    if (newQty <= 0) {
        // Hỏi xác nhận xoá
        Swal.fire({
            icon: 'question',
            title: 'Xoá sản phẩm?',
            text: `Bỏ "${item.tenSanPham}" khỏi giỏ hàng?`,
            showCancelButton: true,
            confirmButtonText: 'Xoá',
            cancelButtonText: 'Huỷ',
            confirmButtonColor: '#ef4444',
        }).then(r => {
            if (r.isConfirmed) removeFromCart(bienTheId);
        });
        return;
    }

    // Kiểm tra không vượt tồn kho
    const stock = allProducts.find(p => p.bienTheId === bienTheId)?.tonKho ?? 999;
    if (newQty > stock) {
        toastWarn(`Tồn kho chỉ còn ${stock} máy.`);
        return;
    }

    item.soLuong = newQty;

    // Nếu giảm số lượng → cắt bớt IMEI tương ứng
    if (delta < 0) {
        const imeiList = parseImeis(item.imeis);
        if (imeiList.length > newQty) {
            item.imeis = imeiList.slice(0, newQty).join(', ');
        }
    }

    onCartChanged();
}

/** Xoá toàn bộ giỏ hàng (dùng sau khi confirm) */
function clearCart() {
    cart = [];
    promoState = { ctkmId: null, tenCtkm: null, giaTriUuDai: 0, tienGiam: 0 };
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

    // Cập nhật badge đếm
    const totalQty = cart.reduce((s, x) => s + x.soLuong, 0);
    if (DOM.cartCount) DOM.cartCount.textContent = totalQty;

    // Bật/tắt nút thanh toán
    if (DOM.btnCheckout) DOM.btnCheckout.disabled = (cart.length === 0);

    // Empty state
    if (cart.length === 0) {
        DOM.cartItemsWrap.innerHTML = '';
        if (DOM.cartEmptyMsg) {
            DOM.cartEmptyMsg.style.display = '';
            DOM.cartItemsWrap.appendChild(DOM.cartEmptyMsg);
        }
        return;
    }

    if (DOM.cartEmptyMsg) DOM.cartEmptyMsg.style.display = 'none';

    // Lưu lại giá trị IMEI hiện tại (để không mất khi re-render)
    cart.forEach(item => {
        const existingInput = el(`imei-input-${item.bienTheId}`);
        if (existingInput) item.imeis = existingInput.value;
    });

    DOM.cartItemsWrap.innerHTML = cart.map(item => buildCartItemHtml(item)).join('');

    // Gán event listeners cho IMEI inputs
    cart.forEach(item => {
        const input = el(`imei-input-${item.bienTheId}`);
        if (input) {
            // Đồng bộ giá trị (tránh mất khi re-render)
            input.value = item.imeis;
            input.addEventListener('input', () => onImeiInput(item.bienTheId));
            input.addEventListener('paste', () => setTimeout(() => onImeiInput(item.bienTheId), 0));
            input.addEventListener('change', () => onImeiInput(item.bienTheId));
        }
        updateImeiIndicator(item.bienTheId);
    });
}

/** Tạo HTML cho 1 dòng sản phẩm trong giỏ */
function buildCartItemHtml(item) {
    const subtotal = item.donGia * item.soLuong;
    return `
    <div class="cart-item" id="cart-item-${item.bienTheId}">
        <!-- Top row: thông tin + stepper -->
        <div class="cart-item-top">
            <div class="cart-item-info">
                <div class="cart-item-name" title="${escHtml(item.tenSanPham)}">${escHtml(item.tenSanPham)}</div>
                <div class="cart-item-sku">${escHtml(item.maSku)}</div>
                <!-- Qty stepper -->
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

        <!-- ══ IMEI SECTION — CỰC KỲ QUAN TRỌNG ══ -->
        <div class="imei-section" role="group" aria-label="Nhập mã IMEI">
            <div class="imei-label">
                <i class="fa fa-barcode" aria-hidden="true"></i>
                Mã IMEI
                <span class="imei-count-indicator err" id="imei-ind-${item.bienTheId}"
                      title="Số IMEI đã nhập / cần nhập">
                    0/${item.soLuong}
                </span>
                <span style="font-size:.61rem;color:rgba(255,255,255,.28);font-weight:400;margin-left:.15rem;">
                    (${item.soLuong} máy • cách nhau bằng dấu phẩy)
                </span>
            </div>
            <textarea
                class="imei-input"
                id="imei-input-${item.bienTheId}"
                rows="1"
                placeholder="VD: 356938035643809${item.soLuong > 1 ? ', 356938035643810' : ''}"
                autocomplete="off"
                spellcheck="false"
                aria-label="Nhập mã IMEI cho ${escHtml(item.tenSanPham)}"
            ></textarea>
            <div class="imei-err-msg" id="imei-err-${item.bienTheId}" role="alert"></div>
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
   8. IMEI MANAGEMENT
════════════════════════════════════════════════════════════ */

/** Gọi khi nội dung IMEI input thay đổi */
function onImeiInput(bienTheId) {
    const input = el(`imei-input-${bienTheId}`);
    const item = cart.find(x => x.bienTheId === bienTheId);
    if (!input || !item) return;

    item.imeis = input.value;

    // Xoá lỗi nếu đang có (user đang sửa)
    clearImeiError(bienTheId);
    updateImeiIndicator(bienTheId);
}

/** Cập nhật badge đếm IMEI (OK/ERR) */
function updateImeiIndicator(bienTheId) {
    const item = cart.find(x => x.bienTheId === bienTheId);
    const indEl = el(`imei-ind-${bienTheId}`);
    if (!item || !indEl) return;

    const imeiVal = el(`imei-input-${bienTheId}`)?.value ?? item.imeis;
    const count = parseImeis(imeiVal).length;
    const needed = item.soLuong;
    const isOk = count === needed;

    indEl.textContent = `${count}/${needed}`;
    indEl.className = `imei-count-indicator ${isOk ? 'ok' : 'err'}`;
}

function showImeiError(bienTheId, msg) {
    const inputEl = el(`imei-input-${bienTheId}`);
    const errEl = el(`imei-err-${bienTheId}`);
    if (inputEl) inputEl.classList.add('imei-error');
    if (errEl) { errEl.style.display = ''; errEl.textContent = msg; }
}

function clearImeiError(bienTheId) {
    const inputEl = el(`imei-input-${bienTheId}`);
    const errEl = el(`imei-err-${bienTheId}`);
    if (inputEl) inputEl.classList.remove('imei-error');
    if (errEl) { errEl.style.display = 'none'; errEl.textContent = ''; }
}

/**
 * Validate tất cả IMEI trong giỏ.
 * @returns {boolean} true nếu tất cả hợp lệ
 *
 * Các quy tắc kiểm tra:
 *   1. Số IMEI == soLuong sản phẩm
 *   2. Không có IMEI trùng nhau trong cùng 1 đơn
 *   3. Mỗi IMEI đủ 15 ký tự số (chuẩn GSMA) — cảnh báo mềm
 */
function validateAllImeis() {
    let allOk = true;
    const seenImeis = new Set();   // Phát hiện IMEI trùng trong đơn

    cart.forEach(item => {
        const imeiRaw = el(`imei-input-${item.bienTheId}`)?.value ?? item.imeis ?? '';
        const imeiList = parseImeis(imeiRaw);
        const needed = item.soLuong;

        // ── Kiểm tra số lượng ────────────────────────────────
        if (imeiList.length !== needed) {
            allOk = false;
            const diff = needed - imeiList.length;
            showImeiError(
                item.bienTheId,
                imeiList.length === 0
                    ? `⚠ Chưa nhập IMEI. Cần nhập ${needed} mã.`
                    : `⚠ Nhập ${imeiList.length}/${needed} IMEI — còn thiếu ${diff > 0 ? diff : 0} mã${diff < 0 ? ` (thừa ${-diff} mã)` : ''}.`
            );
            return; // continue forEach
        }

        // ── Kiểm tra trùng trong đơn ─────────────────────────
        for (const imei of imeiList) {
            const normalized = imei.toUpperCase().replace(/\s/g, '');
            if (seenImeis.has(normalized)) {
                allOk = false;
                showImeiError(item.bienTheId, `⚠ IMEI [${imei}] bị trùng trong đơn hàng.`);
                break;
            }
            seenImeis.add(normalized);
        }

        if (allOk) clearImeiError(item.bienTheId);
    });

    return allOk;
}

/* ════════════════════════════════════════════════════════════
   9. AUTO-PROMO  →  GET /api/admin/pos/tinh-khuyen-mai
════════════════════════════════════════════════════════════ */

/** Trigger debounced promo fetch sau khi cart thay đổi */
function triggerAutoPromo() {
    clearTimeout(promoDebounceTimer);
    promoDebounceTimer = setTimeout(fetchPromo, PROMO_DEBOUNCE_MS);
}

/**
 * Gọi API tính khuyến mãi tự động.
 * Chỉ gọi khi cart không rỗng. Nếu tongTien = 0 → reset promoState.
 */
async function fetchPromo() {
    if (isFetchingPromo) return;  // Không gọi concurrent

    const tongTien = calcTongTien();

    if (tongTien <= 0) {
        promoState = { ctkmId: null, tenCtkm: null, giaTriUuDai: 0, tienGiam: 0 };
        updateSummaryUI();
        return;
    }

    isFetchingPromo = true;

    try {
        const url = `${API.TINH_KM}?tongTien=${encodeURIComponent(tongTien)}`;
        const res = await fetch(url, { credentials: 'include' });

        if (!res.ok) throw new Error(`HTTP ${res.status}`);

        const data = await res.json();

        promoState = {
            ctkmId: data.ctkmId ?? null,
            tenCtkm: data.tenCtkm ?? null,
            giaTriUuDai: Number(data.giaTriUuDai ?? 0),
            tienGiam: Number(data.tienGiam ?? 0),
        };

    } catch (err) {
        // Không hiện lỗi cho user — chỉ log internal
        console.warn('[POS] fetchPromo error (silent):', err.message);
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
    DOM.btnClearCart?.addEventListener('click', handleClearCart);
}

async function handleClearCart() {
    if (!cart.length) return;

    const result = await Swal.fire({
        title: 'Xoá giỏ hàng?',
        text: 'Toàn bộ sản phẩm và IMEI đã nhập sẽ bị xoá.',
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

    // ── BƯỚC 1: Validate IMEI ────────────────────────────────
    const imeisOk = validateAllImeis();
    if (!imeisOk) {
        await Swal.fire({
            icon: 'warning',
            title: 'Thiếu hoặc sai mã IMEI',
            html: `Vui lòng nhập đủ mã IMEI cho tất cả sản phẩm.<br>
                                <small style="color:#6B7280;">
                                Ô IMEI bị lỗi đã được viền đỏ.
                                </small>`,
            confirmButtonText: 'Kiểm tra lại',
            confirmButtonColor: '#1565C0',
        });

        // Scroll đến lỗi đầu tiên
        const firstErr = $('.imei-input.imei-error');
        firstErr?.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return;
    }

    // ── BƯỚC 2: Confirm dialog ──────────────────────────────
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
                    <strong style="color:#1565C0;font-size:1.05rem;">${fmt(canTra)}</strong>
                </div>
            </div>`,
        showCancelButton: true,
        confirmButtonText: '<i class="fa fa-check me-1"></i>Xác nhận thanh toán',
        cancelButtonText: 'Huỷ',
        confirmButtonColor: '#1565C0',
        width: '440px',
        focusConfirm: true,
    });

    if (!confirm.isConfirmed) return;

    // ── BƯỚC 3: Build payload ────────────────────────────────
    // Đọc lại giá trị IMEI mới nhất từ DOM trước khi submit
    cart.forEach(item => {
        const input = el(`imei-input-${item.bienTheId}`);
        if (input) item.imeis = input.value;
    });

    const khachHangId = DOM.selectKhachHang?.value
        ? parseInt(DOM.selectKhachHang.value, 10)
        : null;

    const payload = {
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

    // ── BƯỚC 4: Gửi POST ────────────────────────────────────
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

        // ── BƯỚC 5: Thành công → Hiển thị bill ──────────────
        showBillModal(result, payload);

    } catch (err) {
        console.error('[POS] checkout error:', err);
        Swal.fire({
            icon: 'error',
            title: 'Thanh toán thất bại',
            html: `<div style="font-size:.88rem;text-align:left;">${escHtml(err.message)}</div>`,
            confirmButtonColor: '#1565C0',
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

/* ════════════════════════════════════════════════════════════
   12. BILL / INVOICE MODAL
════════════════════════════════════════════════════════════ */

let _lastPayload = null; // Lưu payload để dùng lại khi in

function showBillModal(apiResult, payload) {
    _lastPayload = payload; // Lưu lại để in

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
            <!-- Header -->
            <div style="text-align:center;margin-bottom:1rem;padding-bottom:.75rem;
                        border-bottom:2px dashed #E8EDF5;">
                <div style="font-size:1.2rem;font-weight:800;color:#1565C0;">📱 PrimeMobile</div>
                <div style="font-size:.72rem;color:#9CA3AF;text-transform:uppercase;
                            letter-spacing:1px;margin:.2rem 0;">Hóa đơn bán hàng tại quầy</div>
                <div style="font-size:.7rem;color:#9CA3AF;">${now}</div>
                <div style="font-size:.75rem;font-weight:700;color:#1565C0;margin-top:.25rem;">
                    Mã ĐH: ${escHtml(apiResult.maDonHang ?? '')}
                </div>
            </div>

            <!-- Khách hàng -->
            <div style="margin-bottom:.5rem;font-size:.82rem;">
                <strong>Khách hàng:</strong> ${escHtml(apiResult.khachHang ?? 'Khách lẻ')}
            </div>
            <hr style="margin:.5rem 0;border-color:#E8EDF5;">

            <!-- Chi tiết hàng -->
            ${linesHtml}

            <hr style="margin:.6rem 0;border-top:2px solid #1565C0;">

            <!-- Tổng -->
            <div style="font-size:.83rem;">
                <div style="display:flex;justify-content:space-between;margin:.2rem 0;">
                    <span>Tổng tiền hàng:</span><strong>${fmt(tongTien)}</strong>
                </div>
                ${promoLine}
                <div style="display:flex;justify-content:space-between;margin:.4rem 0;
                            font-size:1rem;font-weight:800;color:#1565C0;
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
    // Nút In hóa đơn
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
        // Đợi font load rồi mới in
        setTimeout(() => { win.print(); }, 600);
    });

    // Nút Đơn hàng mới
    DOM.btnNewOrder?.addEventListener('click', () => {
        bootstrap.Modal.getInstance(el('modalBill'))?.hide();
        clearCart();
        // Reload stock để cập nhật tồn kho mới nhất
        loadProducts();
        // Toast thông báo
        toastSuccess('Sẵn sàng tạo đơn mới!');
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
   14. INIT — DOMContentLoaded
════════════════════════════════════════════════════════════ */
document.addEventListener('DOMContentLoaded', () => {
    resolveDOM();
    startClock();
    initSidebar();
    initSearch();
    initCheckout();
    initBillModal();
    loadProducts();
});

/* Expose tới onclick="" attributes trong HTML */
window.addToCart = addToCart;
window.removeFromCart = removeFromCart;
window.changeQty = changeQty;
window.loadProducts = loadProducts;
