package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** booking-service (cổng 8084): ca làm việc, khung giờ, đặt, đổi, hủy lịch hẹn, phiếu khám, hồ sơ bệnh nhân. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookingServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
