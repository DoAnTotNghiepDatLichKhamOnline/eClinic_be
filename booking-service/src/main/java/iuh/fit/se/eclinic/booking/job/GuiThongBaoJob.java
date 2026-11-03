package iuh.fit.se.eclinic.booking.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.GuiThongBaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Gửi thông báo đang chờ sang notification-service theo chu kỳ, và xoá sự kiện đã gửi quá 7 ngày. */
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

    @Scheduled(cron = "${app.thong-bao.cron-don}")
    public void don() {
        try {
            int soDong = guiThongBaoService.donDaGui();
            log.info("Đã xoá {} sự kiện thông báo đã gửi quá 7 ngày", soDong);
        } catch (RuntimeException e) {
            log.error("Xoá sự kiện thông báo đã gửi thất bại", e);
        }
    }

}
