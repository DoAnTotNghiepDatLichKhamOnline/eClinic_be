package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/**
 * booking-service (cổng 8084): ca làm việc, khung giờ khám, yêu cầu đổi lịch của bác sĩ; đặt/đổi/hủy lịch hẹn, phiếu khám,
 * hồ sơ bệnh nhân theo CCCD — BOOK-01..12, SCHED-01..05, PAT-01..03, ADM-03, ADM-04.
 * <p>
 * Gồm cả phần xếp lịch (trước là scheduling-service): giữ chỗ khung giờ và duyệt đổi ca phải cập nhật
 * lịch làm việc lẫn lịch hẹn trong cùng 1 transaction.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code booking}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class BookingServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
