package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Xếp ca làm việc và yêu cầu đổi ca / xin nghỉ, prefix {@code app.lich-lam-viec} trong application.yml. */
@ConfigurationProperties("app.lich-lam-viec")
public record LichLamViecProperties(Duration guiYeuCauTruocToiThieu, int soNgayXepTruocToiDa, int soGioToiDaMoiTuan,
        @DefaultValue("/doctor/work-schedule") String duongDanLichBacSi) {

    /** {@link #soGioToiDaMoiTuan} tính bằng phút. */
    public long soPhutToiDaMoiTuan() {
        return soGioToiDaMoiTuan * 60L;
    }
}
