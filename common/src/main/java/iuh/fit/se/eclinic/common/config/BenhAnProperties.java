package iuh.fit.se.eclinic.common.config;

import java.time.LocalDateTime;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Hồ sơ bệnh án, prefix {@code app.benh-an} trong application-common.yml (medical-service chặn sửa, booking-service trả
 * hạn sửa cho bác sĩ). Service dùng phải có {@code @ConfigurationPropertiesScan}.
 *
 * @param soNgayDuocSua bác sĩ sửa được hồ sơ bệnh án trong số ngày này kể từ lúc ghi nhận
 */
@ConfigurationProperties("app.benh-an")
public record BenhAnProperties(int soNgayDuocSua) {

    /** Hạn sửa của hồ sơ ghi nhận lúc {@code ngayGhiNhan}; sau thời điểm này hồ sơ chỉ xem được. */
    public LocalDateTime hanSua(LocalDateTime ngayGhiNhan) {
        return ngayGhiNhan.plusDays(soNgayDuocSua);
    }

}
