package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Đăng nhập và phiên đăng nhập, prefix {@code app.dang-nhap} trong application.yml. */
@ConfigurationProperties("app.dang-nhap")
public record DangNhapProperties(
        @DefaultValue("5") int soLanSaiToiDa,
        @DefaultValue("15m") Duration thoiGianKhoa,
        @DefaultValue("7d") Duration thoiHanRefreshToken,
        @DefaultValue("10s") Duration anHanDungLai) {
}
