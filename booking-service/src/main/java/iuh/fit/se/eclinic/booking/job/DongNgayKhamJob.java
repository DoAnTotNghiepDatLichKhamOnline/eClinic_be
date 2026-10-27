package iuh.fit.se.eclinic.booking.job;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.DongNgayKhamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 0 giờ 1 phút mỗi ngày đóng các ngày khám đã qua: lịch hẹn bệnh nhân chưa được khám thành KHONG_DEN. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DongNgayKhamJob {

    private final DongNgayKhamService dongNgayKhamService;

    @EventListener(ApplicationReadyEvent.class)
    public void khiKhoiDong() {
        dong();
    }

    @Scheduled(cron = "0 1 0 * * *")
    public void dong() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            int soLich = dongNgayKhamService.dongNgayDaQua();
            if (soLich > 0) {
                log.info("Đã đóng {} lịch hẹn bệnh nhân không đến", soLich);
            }
        } catch (RuntimeException e) {
            log.error("Đóng ngày khám thất bại", e);
        }
    }

}
