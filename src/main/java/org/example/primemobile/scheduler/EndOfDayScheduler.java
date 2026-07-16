package org.example.primemobile.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.DonHang;
import org.example.primemobile.entity.MayDienThoai;
import org.example.primemobile.repository.DonHangRepository;
import org.example.primemobile.repository.MayDienThoaiRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class EndOfDayScheduler {

    private final DonHangRepository donHangRepository;
    private final MayDienThoaiRepository mayDienThoaiRepository;

    /**
     * Chạy tự động lúc 23:59:50 mỗi tối.
     * Quét các đơn hàng chờ chưa thanh toán trong ngày và tự động huỷ để giải phóng tồn kho.
     */
    @Scheduled(cron = "50 59 23 * * *")
    @Transactional
    public void autoCancelPendingOrders() {
        log.info("[EndOfDayScheduler] Bắt đầu quét chốt ca cuối ngày...");
        List<DonHang> pendingOrders = donHangRepository.findByKenhBanAndTrangThaiOrderByNgayDatDesc("tai_quay", "don_hang_cho");

        if (pendingOrders.isEmpty()) {
            log.info("[EndOfDayScheduler] Không có đơn hàng chờ nào cần huỷ.");
            return;
        }

        log.info("[EndOfDayScheduler] Đã phát hiện {} đơn hàng chờ tồn đọng. Tiến hành huỷ và giải phóng IMEI...", pendingOrders.size());
        LocalDateTime now = LocalDateTime.now();

        for (DonHang donHang : pendingOrders) {
            donHang.setTrangThai("da_huy");
            donHang.setGhiChu("Hệ thống tự động huỷ lúc chốt ca cuối ngày");
            donHang.setUpdatedAt(now);
            donHangRepository.save(donHang);

            // Hoàn trả IMEI về trong_kho
            List<MayDienThoai> imeiList = mayDienThoaiRepository.findByDonHangId(donHang.getId());
            for (MayDienThoai may : imeiList) {
                may.setTinhTrang("trong_kho");
                may.setNguoiGiu(null);
                may.setThoiGianGiu(null);
                may.setDonHang(null);
                mayDienThoaiRepository.save(may);
            }
            log.info("[EndOfDayScheduler] Đã huỷ tự động đơn hàng chờ: {}", donHang.getMaDonHang());
        }
        
        log.info("[EndOfDayScheduler] Hoàn tất quét chốt ca cuối ngày.");
    }
}
