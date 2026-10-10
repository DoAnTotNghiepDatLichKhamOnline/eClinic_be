package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Tài khoản bác sĩ do quản trị viên tạo (phase 6).
 *
 * @param matKhauMacDinh mật khẩu ban đầu của mọi tài khoản bác sĩ mới; chỉ dùng được để đặt mật khẩu của chính bác sĩ
 *                       ở lần đăng nhập đầu (POST /api/auth/first-password), không mở được phiên đăng nhập
 */
@ConfigurationProperties("app.bac-si")
public record BacSiMoiProperties(@DefaultValue("Doctor@123") String matKhauMacDinh) {
}
