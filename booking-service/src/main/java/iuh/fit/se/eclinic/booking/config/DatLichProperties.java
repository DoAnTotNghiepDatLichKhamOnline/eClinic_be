package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Giới hạn khi đặt lịch, prefix {@code app.dat-lich} trong application.yml.
 *
 * @param datTruocToiThieu               lượt khám chỉ đặt được khi còn cách giờ bắt đầu ít nhất khoảng này
 * @param soNgayDatTruocToiDa            chỉ đặt được lịch trong số ngày tới này, tính từ hôm nay
 * @param soLichHieuLucToiDaMoiHoSo      số lịch hẹn sắp tới còn hiệu lực tối đa của 1 hồ sơ bệnh nhân
 * @param soLichHieuLucToiDaMoiSoDienThoai số lịch hẹn sắp tới còn hiệu lực tối đa đặt bằng cùng 1 SĐT liên hệ
 * @param soLanDatToiDaMoiIp             số lần gọi đặt lịch tối đa từ 1 địa chỉ IP trong {@code cuaSoGioiHanIp}
 * @param cuaSoGioiHanIp                 khoảng thời gian đếm số lần gọi đặt lịch của 1 địa chỉ IP
 * @param huyDoiTruocToiThieu            bệnh nhân chỉ hủy / đổi được lịch khi còn cách giờ khám ít nhất khoảng này
 * @param soLanDoiLichToiDa              số lần đổi lịch tối đa của 1 lần đặt (đếm theo chuỗi lịch cũ)
 */
@ConfigurationProperties("app.dat-lich")
public record DatLichProperties(Duration datTruocToiThieu, int soNgayDatTruocToiDa, int soLichHieuLucToiDaMoiHoSo,
        int soLichHieuLucToiDaMoiSoDienThoai, int soLanDatToiDaMoiIp, Duration cuaSoGioiHanIp,
        Duration huyDoiTruocToiThieu, int soLanDoiLichToiDa) {
}
