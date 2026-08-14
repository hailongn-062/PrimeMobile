const ThongKeDonHang = (function() {
    
    const today = new Date();
    const firstDay = new Date(today.getFullYear(), today.getMonth(), 1);

    const formatDate = (date) => {
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    // State
    let currentDateRange = {
        fromDate: formatDate(firstDay),
        toDate: formatDate(today)
    };
    let chartInstance = null;
    let failedOrdersPage = 0;

    // Formatters
    const formatCurrency = (value) => {
        return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(value);
    };

    const formatNumber = (value) => {
        return new Intl.NumberFormat('vi-VN').format(value);
    };

    const formatDateString = (dateString) => {
        if (!dateString) return '';
        const d = new Date(dateString);
        return d.toLocaleString('vi-VN');
    };

    // Loaders
    const loadKpis = () => {
        $('.kpi-skeleton').removeClass('d-none');
        $('.kpi-real').addClass('d-none');

        $.ajax({
            url: '/api/admin/thong-ke/don-hang/kpi',
            method: 'GET',
            data: currentDateRange,
            success: function(data) {
                $('#kpiTongDon').text(formatNumber(data.tongDon));
                $('#kpiDonHoanThanh').text(formatNumber(data.donHoanThanh));
                $('#kpiDonHuy').text(formatNumber(data.donHuy));
                $('#kpiGiaoThatBai').text(formatNumber(data.donGiaoThatBai));

                $('.kpi-skeleton').addClass('d-none');
                $('.kpi-real').removeClass('d-none');
            },
            error: function(err) {
                console.error("Lỗi khi tải KPI", err);
                $('.kpi-skeleton').addClass('d-none');
            }
        });
    };

    const loadStatusChart = () => {
        $('#statusChartSkeleton').removeClass('d-none');
        $('#statusChartContent').addClass('d-none');
        $('#statusLegend').addClass('d-none').empty();

        $.ajax({
            url: '/api/admin/thong-ke/don-hang/trang-thai',
            method: 'GET',
            data: currentDateRange,
            success: function(data) {
                $('#statusChartSkeleton').addClass('d-none');
                $('#statusChartContent').removeClass('d-none');
                $('#statusLegend').removeClass('d-none');

                const labels = data.map(d => d.trangThai);
                const values = data.map(d => d.soLuong);
                
                const backgroundColors = [
                    '#0d9488', '#ea580c', '#ef4444', '#3b82f6', '#f59e0b', 
                    '#8b5cf6', '#db2777', '#059669', '#65a30d', '#475569'
                ];

                if (chartInstance) {
                    chartInstance.destroy();
                }

                const ctx = document.getElementById('statusChart').getContext('2d');
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
                                            label += formatNumber(context.parsed) + ' đơn';
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
                                <span class="small font-weight-bold">${item.trangThai}</span>
                            </div>
                            <span class="small font-weight-bold text-gray-800">${item.tyTrong.toFixed(1)}%</span>
                        </div>
                    `;
                });
                $('#statusLegend').html(legendHtml);
            }
        });
    };

    const loadFailedOrders = (page = 0) => {
        failedOrdersPage = page;
        $('#failedOrdersSkeleton').removeClass('d-none');
        $('#failedOrdersContent').addClass('d-none');

        const statusFilter = $('#filterFailedStatus').val();

        $.ajax({
            url: '/api/admin/thong-ke/don-hang/that-bai',
            method: 'GET',
            data: {
                ...currentDateRange,
                status: statusFilter,
                page: failedOrdersPage,
                size: 5
            },
            success: function(response) {
                $('#failedOrdersSkeleton').addClass('d-none');
                $('#failedOrdersContent').removeClass('d-none');

                let html = '';
                response.content.forEach(item => {
                    let badge = '';
                    if (item.trangThai === 'da_huy') {
                        badge = '<span class="badge bg-danger">Đã hủy</span>';
                    } else if (item.trangThai === 'giao_that_bai') {
                        badge = '<span class="badge bg-danger">Giao thất bại</span>';
                    } else {
                        badge = `<span class="badge bg-secondary">${item.trangThai}</span>`;
                    }

                    html += `
                        <tr>
                            <td><a href="/admin/don-hang/${item.id}" target="_blank" class="fw-bold text-decoration-none">${item.maDonHang}</a></td>
                            <td>
                                <div class="fw-semibold">${item.tenKhachHang}</div>
                                <div class="text-muted small">${item.sdtKhachHang}</div>
                            </td>
                            <td>${badge}</td>
                            <td class="text-end fw-semibold text-danger">${formatCurrency(item.tongThanhToan)}</td>
                            <td><div class="text-truncate" style="max-width: 200px;" title="${item.lyDo || ''}">${item.lyDo || '<i class="text-muted">Không có ghi chú</i>'}</div></td>
                        </tr>
                    `;
                });

                if (response.content.length === 0) {
                    html = `<tr><td colspan="5" class="text-center text-muted py-4">Không có đơn hàng nào bị hủy hoặc giao thất bại trong khoảng thời gian này</td></tr>`;
                }

                $('#failedOrdersTableBody').html(html);
                $('#failedOrdersTotal').text(`Hiển thị ${response.numberOfElements} / ${response.totalElements} đơn hàng`);
                
                // Pagination basic
                let paginationHtml = '';
                if (response.totalPages > 1) {
                    for (let i = 0; i < response.totalPages; i++) {
                        paginationHtml += `<li class="page-item ${i === response.number ? 'active' : ''}"><a class="page-link failed-orders-page" href="#" data-page="${i}">${i + 1}</a></li>`;
                    }
                }
                $('#failedOrdersPagination').html(paginationHtml);
            },
            error: function(err) {
                console.error("Lỗi khi tải danh sách đơn thất bại", err);
                $('#failedOrdersSkeleton').addClass('d-none');
            }
        });
    };

    let completedOrdersPage = 0;

    const loadCompletedOrders = (page = 0) => {
        completedOrdersPage = page;
        $('#completedOrdersSkeleton').removeClass('d-none');
        $('#completedOrdersContent').addClass('d-none');

        $.ajax({
            url: '/api/admin/thong-ke/don-hang/hoan-thanh',
            method: 'GET',
            data: {
                ...currentDateRange,
                page: completedOrdersPage,
                size: 5
            },
            success: function(response) {
                $('#completedOrdersSkeleton').addClass('d-none');
                $('#completedOrdersContent').removeClass('d-none');

                let html = '';
                response.content.forEach(item => {
                    html += `
                        <tr>
                            <td><a href="/admin/don-hang/${item.id}" target="_blank" class="fw-bold text-decoration-none">${item.maDonHang}</a></td>
                            <td>
                                <div class="fw-semibold">${item.tenKhachHang}</div>
                                <div class="text-muted small">${item.sdtKhachHang}</div>
                            </td>
                            <td><span class="badge bg-success">Đã hoàn thành</span></td>
                            <td class="text-end fw-semibold text-success">${formatCurrency(item.tongThanhToan)}</td>
                        </tr>
                    `;
                });

                if (response.content.length === 0) {
                    html = `<tr><td colspan="4" class="text-center text-muted py-4">Không có đơn hàng hoàn thành nào trong khoảng thời gian này</td></tr>`;
                }

                $('#completedOrdersTableBody').html(html);
                $('#completedOrdersTotal').text(`Hiển thị ${response.numberOfElements} / ${response.totalElements} đơn hàng`);
                
                let paginationHtml = '';
                if (response.totalPages > 1) {
                    for (let i = 0; i < response.totalPages; i++) {
                        paginationHtml += `<li class="page-item ${i === response.number ? 'active' : ''}"><a class="page-link completed-orders-page" href="#" data-page="${i}">${i + 1}</a></li>`;
                    }
                }
                $('#completedOrdersPagination').html(paginationHtml);
            },
            error: function(err) {
                console.error("Lỗi khi tải danh sách đơn hoàn thành", err);
                $('#completedOrdersSkeleton').addClass('d-none');
            }
        });
    };

    const initDateRangePicker = () => {
        $('#startDate').val(currentDateRange.fromDate);
        $('#endDate').val(currentDateRange.toDate);

        $('#btnSearchDate').click(function() {
            const start = $('#startDate').val();
            const end = $('#endDate').val();
            if(!start || !end) {
                alert('Vui lòng chọn khoảng thời gian hợp lệ');
                return;
            }
            if (start > end) {
                alert('Từ ngày không được lớn hơn Đến ngày');
                return;
            }
            currentDateRange.fromDate = start;
            currentDateRange.toDate = end;
            loadAllTimeDependentData();
        });
    };

    const loadAllTimeDependentData = () => {
        loadKpis();
        loadStatusChart();
        loadFailedOrders(0);
        loadCompletedOrders(0);
    };

    const initEvents = () => {
        // Pagination for Failed Orders
        $(document).on('click', '.failed-orders-page', function(e) {
            e.preventDefault();
            loadFailedOrders($(this).data('page'));
        });

        // Pagination for Completed Orders
        $(document).on('click', '.completed-orders-page', function(e) {
            e.preventDefault();
            loadCompletedOrders($(this).data('page'));
        });

        // Filter dropdown change
        $('#filterFailedStatus').change(function() {
            loadFailedOrders(0);
        });
    };

    return {
        init: function() {
            initDateRangePicker();
            
            // Initial loads
            loadAllTimeDependentData();
            
            initEvents();
        }
    };

})();

$(document).ready(function() {
    ThongKeDonHang.init();
});
