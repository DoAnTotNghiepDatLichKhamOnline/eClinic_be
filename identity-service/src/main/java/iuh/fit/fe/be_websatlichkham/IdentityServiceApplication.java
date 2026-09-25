package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * identity-service (cổng 8081): tài khoản, xác thực, phiên đăng nhập — AUTH-01..05, ADM-03.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code identity}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(IdentityServiceApplication.class, args);
    }

}
