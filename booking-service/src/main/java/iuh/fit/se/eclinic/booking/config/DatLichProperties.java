package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Giới hạn khi đặt lịch, prefix {@code app.dat-lich} trong application.yml. */
@ConfigurationProperties("app.dat-lich")
public record DatLichProperties(Duration datTruocToiThieu, int soNgayDatTruocToiDa, int soLichHieuLucToiDaMoiHoSo,
        int soLichHieuLucToiDaMoiSoDienThoai, int soLanDatToiDaMoiIp, Duration cuaSoGioiHanIp,
        Duration huyDoiTruocToiThieu, int soLanDoiLichToiDa) {
}
