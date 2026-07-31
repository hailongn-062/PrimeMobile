const ThongKeSanPham = (function() {
    
    // State
    let currentDateRange = {
        fromDate: moment().startOf('month').format('YYYY-MM-DD'),
        toDate: moment().format('YYYY-MM-DD')
    };
    let chartInstance = null;
    let topSpPage = 0;
    let topSpSortBy = 'soLuong';

    // Formatters
    const formatCurrency = (value) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    };

    const formatNumber = (value) => {
        return new Intl.NumberFormat('vi-VN').format(value);
    };

    // Loaders
    const loadKpis = () => {
        $('.kpi-skeleton').removeClass('d-none');
        $('.kpi-real').addClass('d-none');

        $.ajax({
            url: '/api/admin/thong-ke/san-pham/kpi',
            method: 'GET',
            data: currentDateRange,
            success: function(data) {
                $('#kpiTongSp').text(formatNumber(data.tongSanPham));
                $('#kpiSpDaBan').text(formatNumber(data.sanPhamDaBan));
                $('#kpiSapHetHang').text(formatNumber(data.sapHetHang));
                $('#kpiTonKhoLau').text(formatNumber(data.tonKhoLau));

                $('.kpi-skeleton').addClass('d-none');
                $('.kpi-real').removeClass('d-none');
            },
            error: function(err) {
                console.error("Lỗi khi tải KPI", err);
                $('.kpi-skeleton').addClass('d-none');
            }
        });
    };

    const loadTopProducts = (append = false) => {
        if (!append) {
            topSpPage = 0;
            $('#topSpTableBody').empty();
            $('#topSpSkeleton').removeClass('d-none');
            $('#topSpContent').addClass('d-none');
        }

        $.ajax({
            url: '/api/admin/thong-ke/san-pham/top',
            method: 'GET',
            data: {
                ...currentDateRange,
                page: topSpPage,
                size: 10,
                sortBy: topSpSortBy
            },
            success: function(response) {
                if (!append) {
                    $('#topSpSkeleton').addClass('d-none');
                    $('#topSpContent').removeClass('d-none');
                }

                let html = '';
                response.content.forEach(item => {
                    html += `
                        <tr>
                            <td>
                                <div class="d-flex align-items-center">
                                    <img src="${item.hinhAnh}" class="rounded me-3" width="48" height="48" style="object-fit: cover" alt="${item.tenSanPham}">
                                    <div>
                                        <h6 class="mb-0 fw-semibold">${item.tenSanPham}</h6>
                                        <small class="text-muted">Mã SP: ${item.sanPhamId}</small>
                                    </div>
                                </div>
                            </td>
                            <td class="text-end fw-semibold">${formatNumber(item.soLuongBan)}</td>
                            <td class="text-end fw-semibold text-primary">${formatCurrency(item.doanhThu)}</td>
                            <td>
                                <div class="progress progress-bar-thin">
                                    <div class="progress-bar ${topSpSortBy === 'doanhThu' ? 'bg-primary' : 'bg-info'}" 
                                         role="progressbar" 
                                         style="width: ${item.phanTramDoanhThu}%" 
                                         aria-valuenow="${item.phanTramDoanhThu}" 
                                         aria-valuemin="0" 
                                         aria-valuemax="100">
                                    </div>
                                </div>
                            </td>
                            <td class="text-center">
                                <button class="btn btn-sm btn-outline-primary btn-xem-bien-the" data-id="${item.sanPhamId}">Xem biến thể</button>
                            </td>
                        </tr>
                    `;
                });

                if (response.content.length === 0 && !append) {
                    html = `<tr><td colspan="5" class="text-center text-muted py-4">Không có dữ liệu trong khoảng thời gian này</td></tr>`;
                }

                $('#topSpTableBody').append(html);

                if (response.last) {
                    $('#btnLoadMoreTopSp').hide();
                } else {
                    $('#btnLoadMoreTopSp').show();
                }
            },
            error: function(err) {
                console.error("Lỗi khi tải Top SP", err);
                $('#topSpSkeleton').addClass('d-none');
            }
        });
    };

    const loadCategoryChart = () => {
        $('#catChartSkeleton').removeClass('d-none');
        $('#catChartContent').addClass('d-none');
        $('#catLegend').addClass('d-none').empty();

        $.ajax({
            url: '/api/admin/thong-ke/san-pham/danh-muc',
            method: 'GET',
            data: currentDateRange,
            success: function(data) {
                $('#catChartSkeleton').addClass('d-none');
                $('#catChartContent').removeClass('d-none');
                $('#catLegend').removeClass('d-none');

                const labels = data.map(d => d.tenDanhMuc);
                const values = data.map(d => d.doanhThu);
                
                const backgroundColors = [
                    '#4e73df', '#1cc88a', '#36b9cc', '#f6c23e', '#e74a3b', 
                    '#858796', '#5a5c69', '#2e59d9', '#17a673', '#2c9faf'
                ];

                if (chartInstance) {
                    chartInstance.destroy();
                }

                const ctx = document.getElementById('categoryChart').getContext('2d');
                chartInstance = new Chart(ctx, {
                    type: 'doughnut',
                    data: {
                        labels: labels,
                        datasets: [{
                            data: values,
                            backgroundColor: backgroundColors.slice(0, labels.length),
                            hoverBackgroundColor: backgroundColors.slice(0, labels.length),
                            hoverBorderColor: "rgba(234, 236, 244, 1)",
                        }],
                    },
                    options: {
                        maintainAspectRatio: false,
                        cutout: '70%',
                        plugins: {
                            legend: { display: false },
                            tooltip: {
                                callbacks: {
                                    label: function(context) {
                                        let label = context.label || '';
                                        if (label) label += ': ';
                                        if (context.parsed !== null) {
                                            label += formatCurrency(context.parsed);
                                        }
                                        return label;
                                    }
                                }
                            }
                        }
                    }
                });

                // Custom Legend
                let legendHtml = '';
                data.forEach((item, index) => {
                    let color = backgroundColors[index % backgroundColors.length];
                    legendHtml += `
                        <div class="d-flex justify-content-between align-items-center mb-2">
                            <div class="d-flex align-items-center">
                                <span class="badge rounded-circle p-1 me-2" style="background-color: ${color}">&nbsp;</span>
                                <span class="small font-weight-bold">${item.tenDanhMuc}</span>
                            </div>
                            <span class="small font-weight-bold text-gray-800">${item.phanTram.toFixed(1)}%</span>
                        </div>
                    `;
                });
                $('#catLegend').html(legendHtml);
            }
        });
    };

    const loadDropdownSanPham = () => {
        $.ajax({
            url: '/api/admin/thong-ke/san-pham/dropdown',
            method: 'GET',
            success: function(data) {
                let options = '<option value="">Chọn 1 sản phẩm để xem chi tiết biến thể</option>';
                data.forEach(item => {
                    options += `<option value="${item.value}">${item.label}</option>`;
                });
                $('#selectSanPham').html(options);
                
                $('#selectSanPham').select2({
                    theme: 'bootstrap-5',
                    placeholder: 'Chọn 1 sản phẩm để xem chi tiết biến thể',
                    allowClear: true
                });
            }
        });
    };

    const loadVariantStats = (sanPhamId) => {
        if (!sanPhamId) {
            $('#variantPlaceholder').removeClass('d-none');
            $('#variantContent').addClass('d-none');
            return;
        }

        $('#variantPlaceholder').addClass('d-none');
        $('#variantContent').addClass('d-none');
        $('#variantSkeleton').removeClass('d-none');

        $.ajax({
            url: `/api/admin/thong-ke/san-pham/bien-the/${sanPhamId}`,
            method: 'GET',
            data: currentDateRange,
            success: function(data) {
                $('#variantSkeleton').addClass('d-none');
                $('#variantContent').removeClass('d-none');

                let html = '';
                data.forEach(item => {
                    html += `
                        <tr>
                            <td class="fw-semibold">${item.tenBienThe}</td>
                            <td class="text-end">${formatNumber(item.soLuongBan)}</td>
                            <td class="text-end text-primary">${formatCurrency(item.doanhThu)}</td>
                            <td class="text-end ${item.tonKho < 5 ? 'text-danger fw-bold' : ''}">${formatNumber(item.tonKho)}</td>
                        </tr>
                    `;
                });

                if (data.length === 0) {
                    html = `<tr><td colspan="4" class="text-center text-muted py-4">Không có biến thể nào</td></tr>`;
                }

                $('#variantTableBody').html(html);
            }
        });
    };

    const loadLowStockWarnings = (page = 0) => {
        $('#lowStockSkeleton').removeClass('d-none');
        $('#lowStockContent').addClass('d-none');

        $.ajax({
            url: '/api/admin/thong-ke/san-pham/canh-bao/sap-het-hang',
            method: 'GET',
            data: { page: page, size: 5 },
            success: function(response) {
                $('#lowStockSkeleton').addClass('d-none');
                $('#lowStockContent').removeClass('d-none');

                let html = '';
                response.content.forEach(item => {
                    html += `
                        <tr>
                            <td class="fw-semibold">${item.tenSanPham}</td>
                            <td class="text-center text-danger fw-bold">${formatNumber(item.tonKho)}</td>
                            <td class="text-center">${item.soLuongBanTB.toFixed(1)}</td>
                            <td class="text-center fw-semibold text-warning">${item.soNgayDuKienHetHang > 100 ? '> 100' : item.soNgayDuKienHetHang}</td>
                        </tr>
                    `;
                });
                
                if (response.content.length === 0) {
                    html = `<tr><td colspan="4" class="text-center text-muted py-3">Không có cảnh báo sắp hết hàng</td></tr>`;
                }
                
                $('#lowStockTableBody').html(html);
                $('#lowStockTotal').text(`Hiển thị ${response.numberOfElements} / ${response.totalElements} sản phẩm`);
                
                // Pagination basic
                let paginationHtml = '';
                if (response.totalPages > 1) {
                    for (let i = 0; i < response.totalPages; i++) {
                        paginationHtml += `<li class="page-item ${i === response.number ? 'active' : ''}"><a class="page-link low-stock-page" href="#" data-page="${i}">${i + 1}</a></li>`;
                    }
                }
                $('#lowStockPagination').html(paginationHtml);
            }
        });
    };

    const loadOldStockWarnings = (page = 0) => {
        $('#oldStockSkeleton').removeClass('d-none');
        $('#oldStockContent').addClass('d-none');

        $.ajax({
            url: '/api/admin/thong-ke/san-pham/canh-bao/ton-kho-lau',
            method: 'GET',
            data: { page: page, size: 5 },
            success: function(response) {
                $('#oldStockSkeleton').addClass('d-none');
                $('#oldStockContent').removeClass('d-none');

                let html = '';
                response.content.forEach(item => {
                    html += `
                        <tr>
                            <td class="fw-semibold">${item.tenSanPham}</td>
                            <td class="text-center fw-bold">${formatNumber(item.tonKho)}</td>
                            <td class="text-center text-danger fw-bold">${item.soNgayKhongBan} ngày</td>
                            <td class="text-end text-danger">${formatCurrency(item.giaTriVonTonDong)}</td>
                        </tr>
                    `;
                });
                
                if (response.content.length === 0) {
                    html = `<tr><td colspan="4" class="text-center text-muted py-3">Không có cảnh báo tồn kho lâu</td></tr>`;
                }
                
                $('#oldStockTableBody').html(html);
                $('#oldStockTotal').text(`Hiển thị ${response.numberOfElements} / ${response.totalElements} sản phẩm`);
                
                // Pagination basic
                let paginationHtml = '';
                if (response.totalPages > 1) {
                    for (let i = 0; i < response.totalPages; i++) {
                        paginationHtml += `<li class="page-item ${i === response.number ? 'active' : ''}"><a class="page-link old-stock-page" href="#" data-page="${i}">${i + 1}</a></li>`;
                    }
                }
                $('#oldStockPagination').html(paginationHtml);
            }
        });
    };

    const initDateRangePicker = () => {
        $('#dateRangePicker').daterangepicker({
            startDate: moment().startOf('month'),
            endDate: moment(),
            ranges: {
                'Hôm nay': [moment(), moment()],
                'Hôm qua': [moment().subtract(1, 'days'), moment().subtract(1, 'days')],
                '7 Ngày qua': [moment().subtract(6, 'days'), moment()],
                '30 Ngày qua': [moment().subtract(29, 'days'), moment()],
                'Tháng này': [moment().startOf('month'), moment().endOf('month')],
                'Tháng trước': [moment().subtract(1, 'month').startOf('month'), moment().subtract(1, 'month').endOf('month')]
            },
            locale: {
                format: 'DD/MM/YYYY',
                applyLabel: 'Áp dụng',
                cancelLabel: 'Hủy',
                customRangeLabel: 'Tùy chỉnh'
            }
        }, function(start, end) {
            currentDateRange.fromDate = start.format('YYYY-MM-DD');
            currentDateRange.toDate = end.format('YYYY-MM-DD');
            loadAllTimeDependentData();
        });
    };

    const loadAllTimeDependentData = () => {
        loadKpis();
        loadTopProducts();
        loadCategoryChart();
        if ($('#selectSanPham').val()) {
            loadVariantStats($('#selectSanPham').val());
        }
    };

    const initEvents = () => {
        // Sort toggle for Top Products
        $('input[name="btnradioTop"]').change(function() {
            topSpSortBy = $(this).val();
            loadTopProducts();
        });

        // Load more for Top Products
        $('#btnLoadMoreTopSp').click(function(e) {
            e.preventDefault();
            topSpPage++;
            loadTopProducts(true);
        });

        // "Xem biến thể" button
        $(document).on('click', '.btn-xem-bien-the', function() {
            const sanPhamId = $(this).data('id');
            // Update Select2
            $('#selectSanPham').val(sanPhamId).trigger('change');
            // Scroll to variant section
            $('html, body').animate({
                scrollTop: $('#selectSanPham').offset().top - 100
            }, 500);
        });

        // Select2 change event
        $('#selectSanPham').on('change', function() {
            loadVariantStats($(this).val());
        });

        // Pagination for Warnings
        $(document).on('click', '.low-stock-page', function(e) {
            e.preventDefault();
            loadLowStockWarnings($(this).data('page'));
        });

        $(document).on('click', '.old-stock-page', function(e) {
            e.preventDefault();
            loadOldStockWarnings($(this).data('page'));
        });
        
        $('#old-stock-tab').on('shown.bs.tab', function (e) {
            if ($('#oldStockTableBody').children().length === 0) {
                loadOldStockWarnings();
            }
        })
    };

    return {
        init: function() {
            initDateRangePicker();
            loadDropdownSanPham();
            
            // Initial loads
            loadAllTimeDependentData();
            loadLowStockWarnings(); // oldStockWarnings loads on tab show or now
            loadOldStockWarnings(); // let's load both
            
            initEvents();
        }
    };

})();

$(document).ready(function() {
    ThongKeSanPham.init();
});
