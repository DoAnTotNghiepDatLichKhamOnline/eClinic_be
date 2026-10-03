package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Tài khoản quản trị tạo sẵn khi DB chưa có admin, prefix {@code app.admin-mac-dinh} trong application.yml
 * (lấy từ biến môi trường ADMIN_EMAIL / ADMIN_PASSWORD).
 *
 * @param email   email đăng nhập của admin
 * @param matKhau mật khẩu ban đầu (nên đổi sau lần đăng nhập đầu tiên)
 */
@ConfigurationProperties("app.admin-mac-dinh")
public record AdminMacDinhProperties(String email, String matKhau) {
}
