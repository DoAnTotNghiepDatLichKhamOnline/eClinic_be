package iuh.fit.se.eclinic.booking.config;

import java.time.Duration;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Nhắc lịch khám, prefix {@code app.nhac-lich} trong application.yml.
 *
 * @param cacMoc      các mốc nhắc: trước giờ khám bao lâu thì nhắc (vd 3 ngày, 1 ngày); danh sách rỗng = không nhắc
 * @param chuKy       khoảng nghỉ giữa 2 lần job tìm lịch hẹn đến hạn nhắc
 * @param guiTu       chỉ nhắc từ giờ này trong ngày (giờ của phòng khám)...
 * @param guiDen      ...tới trước giờ này; ngoài khoảng đó job không làm gì, lịch đến hạn ban đêm được nhắc vào sáng
 * @param conToiThieu lượt khám còn cách ít hơn khoảng này thì không nhắc nữa
 * @param kichThuocLo số lịch hẹn tối đa xử lý cho mỗi mốc trong 1 lần chạy
 */
@ConfigurationProperties("app.nhac-lich")
public record NhacLichProperties(
        @DefaultValue({"3d", "1d"}) List<Duration> cacMoc,
        @DefaultValue("5m") Duration chuKy,
        @DefaultValue("07:00") LocalTime guiTu,
        @DefaultValue("21:00") LocalTime guiDen,
        @DefaultValue("2h") Duration conToiThieu,
        @DefaultValue("200") int kichThuocLo) {

    /** Sai cấu hình thì không cho khởi động, tránh chạy "bình thường" mà không nhắc ai. */
    public NhacLichProperties {
        if (cacMoc.stream().anyMatch(moc -> moc.toMinutes() <= 0)) {
            throw new IllegalArgumentException("app.nhac-lich.cac-moc: mỗi mốc phải từ 1 phút trở lên");
        }
        if (!guiTu.isBefore(guiDen)) {
            throw new IllegalArgumentException("app.nhac-lich: gui-tu phải trước gui-den");
        }
        // Mốc gần giờ khám nhất trước: 2 mốc cùng đến hạn thì chỉ nhắc mốc đó
        cacMoc = cacMoc.stream().distinct().sorted(Comparator.naturalOrder()).toList();
    }

}
