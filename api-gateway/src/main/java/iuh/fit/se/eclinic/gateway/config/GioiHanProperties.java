package iuh.fit.se.eclinic.gateway.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.util.unit.DataSize;

/**
 * Giới hạn của gateway (app.gioi-han trong application.yml).
 *
 * @param kichThuocRequestToiDa       dung lượng body tối đa của 1 request (theo header Content-Length); vượt thì gateway
 *                                    trả 413 ngay, không chuyển tiếp
 * @param soYeuCauMoiPhut             số request tối đa của 1 địa chỉ IP trong 1 phút (nhóm chung)
 * @param soYeuCauXacThucMoiPhut      như trên, cho POST tới {@code duongDanXacThuc}
 * @param soYeuCauChatbotMoiPhut      như trên, cho POST /api/chatbot/**
 * @param duongDanXacThuc             các đường dẫn đăng nhập / đăng ký / gửi email tính vào nhóm xác thực
 */
@ConfigurationProperties("app.gioi-han")
public record GioiHanProperties(
        @DefaultValue("5MB") DataSize kichThuocRequestToiDa,
        @DefaultValue("300") int soYeuCauMoiPhut,
        @DefaultValue("20") int soYeuCauXacThucMoiPhut,
        @DefaultValue("30") int soYeuCauChatbotMoiPhut,
        @DefaultValue({ "/api/auth/login", "/api/auth/google", "/api/auth/register", "/api/auth/forgot-password",
                "/api/auth/resend-verification" }) List<String> duongDanXacThuc) {
}
