package iuh.fit.se.eclinic.identity.dulieumau;

import java.time.LocalDate;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Khi khởi động với app.du-lieu-mau.bat = true: tạo dữ liệu mẫu (nếu chưa có bác sĩ nào) rồi bổ sung ca làm việc
 * cho những ngày sắp tới. Chạy sau KhoiTaoAdminRunner vì ca làm việc phải có quản trị viên tạo.
 */
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
        // Bắt mọi lỗi: dữ liệu mẫu hỏng (vd trùng số điện thoại với tài khoản đã có) không được làm service ngừng khởi động
        try {
            duLieuMauService.taoDuLieuNen();
            int soCa = duLieuMauService.boSungLichLamViec(LocalDate.now());
            log.info("Đã bổ sung {} ca làm việc mẫu", soCa);
        } catch (RuntimeException e) {
            log.error("Tạo dữ liệu mẫu thất bại, service vẫn chạy bình thường", e);
        }
    }

}
