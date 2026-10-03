package iuh.fit.se.eclinic.gateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Giới hạn của gateway (app.gioi-han trong application.yml).
 *
 * @param kichThuocRequestToiDa dung lượng body tối đa của 1 request (theo header Content-Length); vượt thì gateway
 *                              trả 413 ngay, không chuyển tiếp
 */
@ConfigurationProperties("app.gioi-han")
public record GioiHanProperties(@DefaultValue("5MB") DataSize kichThuocRequestToiDa) {
}
