package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Tài khoản quản trị tạo sẵn khi DB chưa có admin, prefix {@code app.admin-mac-dinh} (ADMIN_EMAIL, ADMIN_PASSWORD). */
@ConfigurationProperties("app.admin-mac-dinh")
public record AdminMacDinhProperties(String email, String matKhau) {
}
