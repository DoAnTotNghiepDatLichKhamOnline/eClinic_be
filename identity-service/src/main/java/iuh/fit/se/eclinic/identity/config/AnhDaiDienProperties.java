package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Giới hạn đổi ảnh đại diện, prefix {@code app.anh-dai-dien} trong application.yml. Dung lượng tệp tối đa nằm ở
 * {@code spring.servlet.multipart}.
 *
 * @param soLanTaiLenToiDa số lần tải ảnh lên tối đa của 1 tài khoản trong {@code khoangDem} (giữ hạn mức miễn phí của kho ảnh)
 * @param khoangDem        cửa sổ đếm, tính từ lần tải lên đầu tiên
 */
@ConfigurationProperties("app.anh-dai-dien")
public record AnhDaiDienProperties(
        @DefaultValue("10") int soLanTaiLenToiDa,
        @DefaultValue("1h") Duration khoangDem) {
}
