package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * scheduling-service (cổng 8083): ca làm việc, khung giờ khám, yêu cầu đổi lịch của bác sĩ — SCHED-01..06.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code scheduling}.
 */
@SpringBootApplication
public class SchedulingServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(SchedulingServiceApplication.class, args);
    }

}
