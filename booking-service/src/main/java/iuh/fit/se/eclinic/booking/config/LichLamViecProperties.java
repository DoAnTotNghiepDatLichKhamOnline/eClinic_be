package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Xếp ca làm việc và yêu cầu đổi ca / xin nghỉ, prefix {@code app.lich-lam-viec} trong application.yml.
 *
 * @param guiYeuCauTruocToiThieu bác sĩ chỉ gửi được yêu cầu khi ca còn cách giờ bắt đầu ít nhất khoảng này; giờ mong
 *                               muốn của yêu cầu đổi ca cũng phải cách hiện tại ít nhất khoảng này
 * @param soNgayXepTruocToiDa    quản trị viên chỉ xếp được ca trong số ngày tới này, tính từ hôm nay
 */
@ConfigurationProperties("app.lich-lam-viec")
public record LichLamViecProperties(Duration guiYeuCauTruocToiThieu, int soNgayXepTruocToiDa) {
}
