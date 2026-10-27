package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Nhắc tái khám, prefix {@code app.nhac-tai-kham} trong application.yml. */
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
