package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** 0 giờ 15 mỗi ngày bổ sung ca cho các bác sĩ mẫu lớn: {@link BoSungLichMauJob} chỉ xếp ca cho 12 bác sĩ đầu. */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty({ "app.du-lieu-mau.bat", "app.du-lieu-mau.lon" })
public class BoSungLichMauLonJob {

    private final DuLieuMauLonService duLieuMauLonService;

    @Scheduled(cron = "0 15 0 * * *")
    public void boSungLichMau() {
        // Bắt mọi lỗi: lần chạy lỗi chỉ ghi log, lần sau (hoặc lần khởi động sau) bổ sung lại
        try {
            log.info("Đã bổ sung {} ca làm việc mẫu lớn", duLieuMauLonService.boSungLichLamViec(LocalDate.now()));
        } catch (RuntimeException e) {
            log.error("Bổ sung ca làm việc mẫu lớn thất bại", e);
        }
    }

}
