package iuh.fit.se.eclinic.booking.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.GuiThongBaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi thông báo đang chờ sang notification-service sau mỗi {@code app.thong-bao.chu-ky-gui} (tính từ lúc lần trước chạy
 * xong), và 3 giờ 30 sáng mỗi ngày xoá các sự kiện đã gửi quá 7 ngày.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuiThongBaoJob {

    private final GuiThongBaoService guiThongBaoService;

    @Scheduled(fixedDelayString = "${app.thong-bao.chu-ky-gui}", initialDelayString = "${app.thong-bao.chu-ky-gui}")
    public void gui() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            guiThongBaoService.guiDangCho();
        } catch (RuntimeException e) {
            log.error("Gửi thông báo đang chờ thất bại", e);
        }
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void don() {
        try {
            int soDong = guiThongBaoService.donDaGui();
            log.info("Đã xoá {} sự kiện thông báo đã gửi quá 7 ngày", soDong);
        } catch (RuntimeException e) {
            log.error("Xoá sự kiện thông báo đã gửi thất bại", e);
        }
    }

}
