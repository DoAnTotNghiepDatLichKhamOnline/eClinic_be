package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Chạy sau {@link KhoiTaoDuLieuMauRunner}: tạo dữ liệu mẫu lớn nếu chưa có, rồi bổ sung ca cho những ngày tới. */
@Slf4j
@Component
@Order(3)
@RequiredArgsConstructor
@ConditionalOnBooleanProperty({ "app.du-lieu-mau.bat", "app.du-lieu-mau.lon" })
public class KhoiTaoDuLieuMauLonRunner implements ApplicationRunner {

    private final DuLieuMauLonService duLieuMauLonService;

    @Override
    public void run(ApplicationArguments args) {
        // Bắt mọi lỗi: dữ liệu mẫu hỏng không được làm service ngừng khởi động
        try {
            long batDau = System.currentTimeMillis();
            if (duLieuMauLonService.taoDuLieuLon()) {
                log.info("Đã tạo dữ liệu mẫu lớn trong {} ms", System.currentTimeMillis() - batDau);
            }
            log.info("Đã bổ sung {} ca làm việc mẫu lớn", duLieuMauLonService.boSungLichLamViec(LocalDate.now()));
        } catch (RuntimeException e) {
            log.error("Tạo dữ liệu mẫu lớn thất bại, service vẫn chạy bình thường", e);
        }
        // Sau khi đã xếp ca: đặt sẵn 1 nửa mỗi ca sắp tới, nâng hạn đặt lịch tới ngày cuối có ca
        try {
            duLieuMauLonService.nangHanDatLich(LocalDate.now());
            log.info("Đã đặt sẵn {} lịch hẹn cho ca sắp tới", duLieuMauLonService.lapNuaCaSapToi(LocalDate.now()));
        } catch (RuntimeException e) {
            log.error("Đặt sẵn lịch cho ca sắp tới thất bại, service vẫn chạy bình thường", e);
        }
        // Sau dữ liệu mẫu lớn: dữ liệu mẫu lớn chia lịch hẹn đã qua cho benhnhan01..04, bổ sung trước sẽ thành quá 10
        try {
            log.info("Đã bổ sung {} lần khám mẫu", duLieuMauLonService.boSungLichSuKham(LocalDate.now()));
        } catch (RuntimeException e) {
            log.error("Bổ sung lịch sử khám mẫu thất bại, service vẫn chạy bình thường", e);
        }
    }

}
