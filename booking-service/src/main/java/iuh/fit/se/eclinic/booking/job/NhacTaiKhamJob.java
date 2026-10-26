package iuh.fit.se.eclinic.booking.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.booking.service.NhacTaiKhamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Báo và nhắc ngày tái khám sau mỗi {@code app.nhac-lich.chu-ky} (cùng chu kỳ với {@link NhacLichJob}): bác sĩ lưu ngày
 * tái khám thì bệnh nhân được báo sau nhiều nhất 1 chu kỳ. Thông báo và email được ghi vào bảng chờ gửi,
 * {@link GuiThongBaoJob} gửi đi.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NhacTaiKhamJob {

    private final NhacTaiKhamService nhacTaiKhamService;

    @Scheduled(fixedDelayString = "${app.nhac-lich.chu-ky}", initialDelayString = "${app.nhac-lich.chu-ky}")
    public void nhac() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            int soHoSo = nhacTaiKhamService.xuLyDenHan();
            if (soHoSo > 0) {
                log.info("Đã báo / nhắc tái khám cho {} hồ sơ bệnh án", soHoSo);
            }
        } catch (RuntimeException e) {
            log.error("Nhắc tái khám thất bại", e);
        }
    }

}
