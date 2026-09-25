package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * notification-service (cổng 8086): thông báo trong ứng dụng, gửi email — NOTI-01.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code notification}.
 */
@SpringBootApplication
public class NotificationServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}
