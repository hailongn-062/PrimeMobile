/**
 * imei-picker.js
 * Quản lý UI chọn IMEI dạng Chip + Danh sách có tìm kiếm
 */

const ImeiPickerUI = {
    /**
     * Tạo mã HTML cho khung chọn IMEI
     * @param {number|string} id - ID định danh (thường là bienTheId hoặc chiTietId)
     * @param {number} maxCount - Số lượng tối đa được chọn
     * @param {Array} imeiList - Danh sách IMEI khả dụng
     */
    buildHtml: function(id, maxCount, imeiList) {
        // Lưu dữ liệu vào biến global tạm hoặc data attribute để dùng sau
        const dataJson = encodeURIComponent(JSON.stringify(imeiList));
        
        return `
            <div class="imei-picker-container" id="imei-picker-container-${id}" data-id="${id}" data-max="${maxCount}" data-imeis="${dataJson}">
                <div class="mb-2">
                    <input type="text" class="form-control form-control-sm imei-search-input" placeholder="🔍 Tìm kiếm IMEI...">
                </div>
                
                <div class="mb-2">
                    <div class="small fw-bold text-success mb-1">
                        Đã chọn (<span class="imei-selected-count">0</span>${maxCount > 0 ? `/${maxCount}` : ''}):
                    </div>
                    <div class="imei-chips-area d-flex flex-wrap gap-1" style="min-height: 32px; border: 1px dashed #ccc; padding: 4px; border-radius: 4px;">
                        <!-- Chips sẽ render vào đây -->
                    </div>
                </div>

                <div class="small fw-bold text-muted mb-1">Danh sách khả dụng:</div>
                <div class="imei-list-area list-group" style="max-height: 200px; overflow-y: auto;">
                    <!-- Items sẽ render vào đây -->
                </div>
            </div>
        `;
    },

    /**
     * Khởi tạo các sự kiện cho một container
     * @param {HTMLElement} container 
     */
    init: function(container) {
        const id = container.dataset.id;
        const maxCount = parseInt(container.dataset.max) || 0;
        const imeiList = JSON.parse(decodeURIComponent(container.dataset.imeis));
        
        const searchInput = container.querySelector('.imei-search-input');
        const chipsArea = container.querySelector('.imei-chips-area');
        const listArea = container.querySelector('.imei-list-area');
        const countSpan = container.querySelector('.imei-selected-count');
        
        let selectedImeis = new Set();
        let searchTerm = '';

        const render = () => {
            // Lọc
            const filtered = imeiList.filter(item => item.imei1.toLowerCase().includes(searchTerm.toLowerCase()));
            
            // Render list
            if (filtered.length === 0) {
                listArea.innerHTML = '<div class="list-group-item text-muted small text-center">Không tìm thấy IMEI phù hợp</div>';
            } else {
                listArea.innerHTML = filtered.map(item => {
                    const isSelected = selectedImeis.has(item.imei1);
                    const bgClass = isSelected ? 'bg-success-subtle border-success' : '';
                    const icon = isSelected ? '<i class="fa fa-check text-success"></i>' : '';
                    return `
                        <button type="button" class="list-group-item list-group-item-action d-flex justify-content-between align-items-center py-1 px-2 ${bgClass}" data-imei="${item.imei1}">
                            <span class="small fw-semibold ${isSelected ? 'text-success' : ''}">${item.imei1}</span>
                            ${icon}
                        </button>
                    `;
                }).join('');
            }
            
            // Render chips
            if (selectedImeis.size === 0) {
                chipsArea.innerHTML = '<span class="text-muted small">Chưa chọn IMEI nào</span>';
            } else {
                chipsArea.innerHTML = Array.from(selectedImeis).map(imei => `
                    <span class="badge bg-success d-flex align-items-center gap-1" style="font-size: 0.85rem;">
                        ${imei}
                        <i class="fa fa-times imei-chip-remove" data-imei="${imei}" style="cursor: pointer;"></i>
                    </span>
                `).join('');
            }
            
            countSpan.textContent = selectedImeis.size;
        };

        // Event listener cho tìm kiếm
        searchInput.addEventListener('input', (e) => {
            searchTerm = e.target.value;
            render();
        });

        // Event listener cho việc click chọn item trong list
        listArea.addEventListener('click', (e) => {
            const btn = e.target.closest('.list-group-item');
            if (!btn) return;
            const imei = btn.dataset.imei;
            
            if (selectedImeis.has(imei)) {
                selectedImeis.delete(imei);
            } else {
                if (maxCount > 0 && selectedImeis.size >= maxCount) {
                    if (typeof Swal !== 'undefined') {
                        Swal.fire({ icon: 'warning', title: 'Vượt quá số lượng', text: `Chỉ được chọn tối đa ${maxCount} IMEI.`, timer: 2000, showConfirmButton: false });
                    } else {
                        alert(`Chỉ được chọn tối đa ${maxCount} IMEI.`);
                    }
                    return;
                }
                selectedImeis.add(imei);
            }
            render();
        });

        // Event listener cho việc click nút X trên chip
        chipsArea.addEventListener('click', (e) => {
            if (e.target.classList.contains('imei-chip-remove')) {
                const imei = e.target.dataset.imei;
                selectedImeis.delete(imei);
                render();
            }
        });

        // Hàm helper bên ngoài gọi vào để set imei (khi quét barcode)
        container.addImei = (imei) => {
            if (!imeiList.find(x => x.imei1 === imei)) return 'invalid';
            if (selectedImeis.has(imei)) return 'exists';
            if (maxCount > 0 && selectedImeis.size >= maxCount) return 'full';
            
            selectedImeis.add(imei);
            render();
            return 'added';
        };

        container.getSelected = () => Array.from(selectedImeis);
        container.getAvailableCount = () => imeiList.length;

        // Render lần đầu
        render();
    }
};
