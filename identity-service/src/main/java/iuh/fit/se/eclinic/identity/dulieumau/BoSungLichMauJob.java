package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 0 giờ 10 mỗi ngày bổ sung ca làm việc mẫu cho ngày mới nhất, để service chạy nhiều ngày liền vẫn luôn còn
 * khung giờ trống để đặt lịch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("app.du-lieu-mau.bat")
public class BoSungLichMauJob {

    private final DuLieuMauService duLieuMauService;

    @Scheduled(cron = "0 10 0 * * *")
    public void boSungLichMau() {
        // Bắt mọi lỗi: lần chạy lỗi chỉ ghi log, lần sau (hoặc lần khởi động sau) bổ sung lại
        try {
            int soCa = duLieuMauService.boSungLichLamViec(LocalDate.now());
            log.info("Đã bổ sung {} ca làm việc mẫu", soCa);
        } catch (RuntimeException e) {
            log.error("Bổ sung ca làm việc mẫu thất bại", e);
        }
    }

}
