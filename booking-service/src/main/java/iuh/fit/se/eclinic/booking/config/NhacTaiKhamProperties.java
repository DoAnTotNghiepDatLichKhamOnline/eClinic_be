package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Nhắc tái khám, prefix {@code app.nhac-tai-kham} trong application.yml. Chu kỳ job, khoảng giờ được gửi và kích thước
 * lô dùng chung của nhắc lịch khám ({@link NhacLichProperties}).
 *
 * @param truocNgay      nhắc trước ngày tái khám bao nhiêu ngày; 0 = chỉ báo lúc bác sĩ hẹn, không nhắc trước
 * @param duongDanDatLich trang đặt lịch của frontend, nối sau {@code app.phieu-kham.frontend-url} thành link trong email
 */
@ConfigurationProperties("app.nhac-tai-kham")
public record NhacTaiKhamProperties(
        @DefaultValue("7") int truocNgay,
        @DefaultValue("/dat-lich") String duongDanDatLich) {

    /** Sai cấu hình thì không cho khởi động. */
    public NhacTaiKhamProperties {
        if (truocNgay < 0) {
            throw new IllegalArgumentException("app.nhac-tai-kham.truoc-ngay: không được âm");
        }
    }

}
