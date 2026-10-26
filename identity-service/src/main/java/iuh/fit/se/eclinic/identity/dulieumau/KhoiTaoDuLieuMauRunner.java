package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Khi khởi động với app.du-lieu-mau.bat = true: tạo dữ liệu mẫu nếu chưa có, rồi bổ sung ca cho những ngày tới. */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
@ConditionalOnBooleanProperty("app.du-lieu-mau.bat")
public class KhoiTaoDuLieuMauRunner implements ApplicationRunner {

    private final DuLieuMauService duLieuMauService;

    @Override
    public void run(ApplicationArguments args) {
        log.warn("Dữ liệu mẫu đang BẬT (SEED_DATA=true): có tài khoản bác sĩ / bệnh nhân mẫu dùng chung mật khẩu"
                + " đã công khai. Chỉ dùng để thử / demo, KHÔNG bật khi triển khai thật.");
        // Bắt mọi lỗi: dữ liệu mẫu hỏng không được làm service ngừng khởi động
        try {
            duLieuMauService.taoDuLieuNen();
            duLieuMauService.boSungHoSoBacSi();
            duLieuMauService.boSungTiengAnh();
            duLieuMauService.boSungGiayPhepVaNgonNgu();
            duLieuMauService.boSungTrieuChung();
            log.info("Đã bổ sung {} thuốc mẫu vào danh mục", duLieuMauService.boSungThuoc());
            int soCa = duLieuMauService.boSungLichLamViec(LocalDate.now());
            log.info("Đã bổ sung {} ca làm việc mẫu", soCa);
        } catch (RuntimeException e) {
            log.error("Tạo dữ liệu mẫu thất bại, service vẫn chạy bình thường", e);
        }
    }

}
