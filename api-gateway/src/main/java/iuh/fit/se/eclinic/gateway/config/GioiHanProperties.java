package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/** Giới hạn của gateway (app.gioi-han trong application.yml). */
@ConfigurationProperties("app.gioi-han")
public record GioiHanProperties(
        @DefaultValue("5MB") DataSize kichThuocRequestToiDa,
        @DefaultValue("300") int soYeuCauMoiPhut,
        @DefaultValue("20") int soYeuCauXacThucMoiPhut,
        @DefaultValue("30") int soYeuCauChatbotMoiPhut,
        @DefaultValue({ "/api/auth/login", "/api/auth/google", "/api/auth/register", "/api/auth/forgot-password",
                "/api/auth/resend-verification" }) List<String> duongDanXacThuc) {
}
