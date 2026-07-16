package org.example.primemobile.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.entity.MayDienThoai;
import org.example.primemobile.repository.MayDienThoaiRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ImeiCleanupScheduler {

    private final MayDienThoaiRepository mayDienThoaiRepository;

    /**
     * Chạy mỗi 5 phút một lần.
     * Quét các IMEI đang ở trạng thái "dang_giu" mà đã bị giữ quá 15 phút thì tự động nhả về "trong_kho".
     * Việc này giúp dọn dẹp các IMEI bị "mồ côi" do nhân viên tắt trình duyệt đột ngột hoặc bỏ giỏ hàng mà không thanh toán.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupAbandonedImeis() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(15);
        List<MayDienThoai> abandonedImeis = mayDienThoaiRepository.findByTinhTrangAndThoiGianGiuBeforeAndDonHangIsNull("dang_giu", cutoffTime);

        if (!abandonedImeis.isEmpty()) {
            log.info("[ImeiCleanupScheduler] Đã phát hiện {} IMEI bị kẹt ở trạng thái 'dang_giu' quá 15 phút. Tiến hành giải phóng...", abandonedImeis.size());
            for (MayDienThoai may : abandonedImeis) {
                may.setTinhTrang("trong_kho");
                may.setNguoiGiu(null);
                may.setThoiGianGiu(null);
                mayDienThoaiRepository.save(may);
                log.debug("[ImeiCleanupScheduler] Đã giải phóng IMEI: {}", may.getImei1());
            }
        }
    }
}
