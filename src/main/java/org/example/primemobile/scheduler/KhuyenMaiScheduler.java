package org.example.primemobile.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.primemobile.repository.ChuongTrinhKhuyenMaiRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduler tự động quét và cập nhật trạng thái các chương trình khuyến mãi.
 *
 * <p>Chạy mỗi 60 giây, thực thi lệnh UPDATE trực tiếp trên database thông qua
 * JPQL {@code @Modifying} — không kéo entity về Java, tránh OOM với lượng dữ liệu lớn.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KhuyenMaiScheduler {

    private final ChuongTrinhKhuyenMaiRepository khuyenMaiRepository;

    /**
     * Quét và đồng bộ trạng thái tất cả chương trình khuyến mãi theo thời gian thực.
     *
     * <ul>
     *   <li>Kích hoạt các chương trình {@code chua_bat_dau} đã đến ngày bắt đầu.</li>
     *   <li>Kết thúc các chương trình {@code dang_dien_ra} đã quá ngày kết thúc.</li>
     * </ul>
     *
     * <p>Chạy định kỳ mỗi 60 000 ms (1 phút) kể từ lần chạy trước kết thúc.</p>
     */
    @Scheduled(fixedRate = 60_000)
    public void quetTrangThaiKhuyenMai() {
        try {
            int soKichHoat = khuyenMaiRepository.kichHoatKhuyenMai();
            int soKetThuc  = khuyenMaiRepository.ketThucKhuyenMai();

            if (soKichHoat > 0 || soKetThuc > 0) {
                log.info("[KhuyenMaiScheduler] Đã kích hoạt {} đợt sale, kết thúc {} đợt sale.",
                        soKichHoat, soKetThuc);
            } else {
                log.debug("[KhuyenMaiScheduler] Không có thay đổi trạng thái khuyến mãi.");
            }
        } catch (Exception e) {
            log.error("[KhuyenMaiScheduler] Lỗi khi cập nhật trạng thái khuyến mãi: {}", e.getMessage(), e);
        }
    }
}
