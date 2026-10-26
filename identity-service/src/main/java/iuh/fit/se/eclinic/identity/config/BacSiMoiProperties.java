package iuh.fit.se.eclinic.identity.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Tài khoản bác sĩ do quản trị viên tạo. */
@ConfigurationProperties("app.bac-si")
public record BacSiMoiProperties(@DefaultValue("Doctor@123") String matKhauMacDinh) {
}
