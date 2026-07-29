/**
 * Barcode Scanner Module using html5-qrcode
 * Phục vụ quét IMEI qua webcam máy tính
 */

const BarcodeScanner = (function () {
    let html5QrCode = null;
    let isScanning = false;
    const SCANNER_DOM_ID = "barcode-scanner-ui";

    /**
     * Tạo UI cho scanner nếu chưa có
     */
    function createScannerUI() {
        let scannerWrapper = document.getElementById("barcode-scanner-wrapper");
        if (!scannerWrapper) {
            scannerWrapper = document.createElement("div");
            scannerWrapper.id = "barcode-scanner-wrapper";
            scannerWrapper.style.cssText = `
                position: fixed;
                top: 0; left: 0; width: 100%; height: 100%;
                background: rgba(0, 0, 0, 0.8);
                display: none;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                z-index: 9999;
            `;

            const container = document.createElement("div");
            container.style.cssText = `
                background: #fff;
                padding: 1rem;
                border-radius: 12px;
                width: 90%;
                max-width: 500px;
                text-align: center;
                box-shadow: 0 4px 20px rgba(0,0,0,0.5);
            `;

            const header = document.createElement("h5");
            header.innerHTML = '<i class="fa fa-barcode me-2"></i>Quét mã IMEI';
            header.style.marginBottom = "1rem";
            header.style.color = "#333";

            const scannerDiv = document.createElement("div");
            scannerDiv.id = SCANNER_DOM_ID;
            scannerDiv.style.width = "100%";
            scannerDiv.style.minHeight = "300px";

            const btnClose = document.createElement("button");
            btnClose.innerHTML = '<i class="fa fa-times me-2"></i>Đóng Camera';
            btnClose.className = "btn btn-danger mt-3";
            btnClose.onclick = stopScanner;

            container.appendChild(header);
            container.appendChild(scannerDiv);
            container.appendChild(btnClose);
            scannerWrapper.appendChild(container);

            document.body.appendChild(scannerWrapper);
        }
        return scannerWrapper;
    }

    /**
     * Mở camera và bắt đầu quét
     * @param {Function} onSuccess Callback khi quét thành công: onSuccess(decodedText)
     */
    function openScanner(onSuccess) {
        if (isScanning) return;

        const wrapper = createScannerUI();
        wrapper.style.display = "flex";

        html5QrCode = new Html5Qrcode(SCANNER_DOM_ID);
        const config = { fps: 10, qrbox: { width: 250, height: 150 } };

        html5QrCode.start(
            { facingMode: "environment" },
            config,
            (decodedText, decodedResult) => {
                // Quét thành công
                if (decodedText && decodedText.trim().length > 0) {
                    onSuccess(decodedText.trim());
                }
            },
            (errorMessage) => {
                // Lỗi quét (bỏ qua vì quét liên tục)
            }
        ).then(() => {
            isScanning = true;
        }).catch((err) => {
            console.error("Camera start failed", err);
            wrapper.style.display = "none";
            if (typeof Swal !== 'undefined') {
                 Swal.fire({
                     icon: 'error',
                     title: 'Lỗi Camera',
                     text: 'Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập.'
                 });
            } else {
                 alert("Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập.");
            }
        });
    }

    /**
     * Tắt camera và đóng UI (cả overlay và inline)
     */
    function stopScanner() {
        if (html5QrCode && isScanning) {
            html5QrCode.stop().then(() => {
                isScanning = false;
                html5QrCode.clear();
                const wrapper = document.getElementById("barcode-scanner-wrapper");
                if (wrapper) wrapper.style.display = "none";
            }).catch(err => {
                console.error("Failed to stop scanner", err);
            });
        } else {
            const wrapper = document.getElementById("barcode-scanner-wrapper");
            if (wrapper) wrapper.style.display = "none";
        }
    }

    /**
     * Mở camera trong một DOM element cụ thể (inline)
     * @param {string} containerId ID của element chứa scanner
     * @param {Function} onSuccess Callback khi quét thành công
     */
    function openInline(containerId, onSuccess) {
        if (isScanning) return;

        const container = document.getElementById(containerId);
        if (!container) {
            console.error(`Không tìm thấy element có ID: ${containerId}`);
            return;
        }

        container.style.display = "block";
        html5QrCode = new Html5Qrcode(containerId);
        const config = { fps: 10, qrbox: { width: 250, height: 150 } };

        html5QrCode.start(
            { facingMode: "environment" },
            config,
            (decodedText, decodedResult) => {
                if (decodedText && decodedText.trim().length > 0) {
                    onSuccess(decodedText.trim());
                }
            },
            (errorMessage) => {
                // Ignore errors
            }
        ).then(() => {
            isScanning = true;
        }).catch((err) => {
            console.error("Camera start failed", err);
            container.style.display = "none";
            if (typeof Swal !== 'undefined') {
                 Swal.fire({
                     icon: 'error',
                     title: 'Lỗi Camera',
                     text: 'Không thể truy cập camera. Vui lòng kiểm tra quyền truy cập.'
                 });
            } else {
                 alert("Không thể truy cập camera.");
            }
        });
    }

    /**
     * Gọi API tra cứu IMEI
     * @param {string} imei1 
     * @returns {Promise<Object>} API Response
     */
    async function lookupImei(imei1) {
        try {
            const response = await fetch(`/api/admin/imei/tra-cuu?imei1=${encodeURIComponent(imei1)}`);
            return await response.json();
        } catch (error) {
            console.error("Lỗi tra cứu IMEI:", error);
            return { success: false, message: "Lỗi kết nối tới máy chủ." };
        }
    }

    return {
        open: openScanner,
        openInline: openInline,
        stop: stopScanner,
        lookup: lookupImei
    };
})();
