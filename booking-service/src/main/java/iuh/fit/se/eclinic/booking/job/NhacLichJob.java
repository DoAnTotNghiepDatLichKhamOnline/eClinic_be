package iuh.fit.se.eclinic.booking.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.NhacLichService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Tìm và nhắc các lịch hẹn đến hạn nhắc sau mỗi {@code app.nhac-lich.chu-ky} (tính từ lúc lần trước chạy xong). */
@Slf4j
@Component
@RequiredArgsConstructor
public class NhacLichJob {

    private final NhacLichService nhacLichService;

    @Scheduled(fixedDelayString = "${app.nhac-lich.chu-ky}", initialDelayString = "${app.nhac-lich.chu-ky}")
    public void nhac() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            int soLich = nhacLichService.nhacDenHan();
            if (soLich > 0) {
                log.info("Đã nhắc {} lịch hẹn sắp tới", soLich);
            }
        } catch (RuntimeException e) {
            log.error("Nhắc lịch khám thất bại", e);
        }
    }

}
