package iuh.fit.se.eclinic.common.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Hồ sơ bệnh án, prefix {@code app.benh-an}: hạn bác sĩ được sửa hồ sơ. */
@ConfigurationProperties("app.benh-an")
public record BenhAnProperties(int soNgayDuocSua) {

    /** Hạn sửa của hồ sơ ghi nhận lúc {@code ngayGhiNhan}; sau thời điểm này hồ sơ chỉ xem được. */
    public LocalDateTime hanSua(LocalDateTime ngayGhiNhan) {
        return ngayGhiNhan.plusDays(soNgayDuocSua);
    }

}
