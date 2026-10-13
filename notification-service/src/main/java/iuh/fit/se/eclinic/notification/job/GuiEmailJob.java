package iuh.fit.se.eclinic.notification.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.notification.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi email đến hạn sau mỗi {@code app.email.chu-ky-gui} (tính từ lúc lần trước chạy xong), và 3 giờ 40 sáng mỗi ngày
 * xoá các email đã gửi / đã bỏ quá hạn giữ.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuiEmailJob {

    private final EmailService emailService;

    @Scheduled(fixedDelayString = "${app.email.chu-ky-gui}", initialDelayString = "${app.email.chu-ky-gui}")
    public void gui() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            emailService.guiDangCho();
        } catch (RuntimeException e) {
            log.error("Gửi email đang chờ thất bại", e);
        }
    }

    @Scheduled(cron = "0 40 3 * * *")
    public void don() {
        try {
            int soDong = emailService.donDaXong();
            log.info("Đã xoá {} email đã gửi / đã bỏ quá hạn giữ", soDong);
        } catch (RuntimeException e) {
            log.error("Xoá email đã xong thất bại", e);
        }
    }

}
