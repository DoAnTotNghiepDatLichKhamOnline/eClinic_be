package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** notification-service (cổng 8086): thông báo trong ứng dụng, gửi email. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class NotificationServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}
