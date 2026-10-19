package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Xếp ca làm việc và yêu cầu đổi ca / xin nghỉ, prefix {@code app.lich-lam-viec} trong application.yml.
 *
 * @param guiYeuCauTruocToiThieu bác sĩ chỉ gửi được yêu cầu khi ca còn cách giờ bắt đầu ít nhất khoảng này; giờ mong
 *                               muốn của yêu cầu đổi ca cũng phải cách hiện tại ít nhất khoảng này
 * @param soNgayXepTruocToiDa    quản trị viên chỉ xếp được ca trong số ngày tới này, tính từ hôm nay
 * @param soGioToiDaMoiTuan      tổng giờ các ca còn hoạt động của 1 bác sĩ trong 1 tuần (thứ Hai - Chủ nhật) không vượt
 *                               số giờ này (SCHED-05)
 * @param duongDanLichBacSi      trang Lịch làm việc của bác sĩ trên frontend (nối sau phieu-kham.frontend-url): nút
 *                               trong email gửi lịch mở trang này
 */
@ConfigurationProperties("app.lich-lam-viec")
public record LichLamViecProperties(Duration guiYeuCauTruocToiThieu, int soNgayXepTruocToiDa, int soGioToiDaMoiTuan,
        @DefaultValue("/doctor/work-schedule") String duongDanLichBacSi) {

    /** {@link #soGioToiDaMoiTuan} tính bằng phút. */
    public long soPhutToiDaMoiTuan() {
        return soGioToiDaMoiTuan * 60L;
    }
}
