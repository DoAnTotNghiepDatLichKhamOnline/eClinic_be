package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** identity-service (cổng 8081): tài khoản, xác thực, phiên đăng nhập. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(IdentityServiceApplication.class, args);
    }

}
