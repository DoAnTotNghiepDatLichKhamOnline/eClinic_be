package iuh.fit.se.eclinic.common.security;

import java.time.Duration;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cấu hình bảo mật, prefix {@code app.bao-mat} (giá trị chung ở application-common.yml).
 *
 * @param jwtSecret          khoá ký JWT (HS256), tối thiểu 32 byte, phải giống nhau ở mọi service
 * @param thoiHanAccessToken thời gian sống của access token
 * @param duongDanCongKhai   đường dẫn không cần đăng nhập của service, dạng "pattern" hoặc "METHOD pattern",
 *                           vd {@code GET /api/catalog/chuyen-khoa/**}
 */
@ConfigurationProperties("app.bao-mat")
public record BaoMatProperties(
        String jwtSecret,
        @DefaultValue("30m") Duration thoiHanAccessToken,
        @DefaultValue List<String> duongDanCongKhai) {
}
