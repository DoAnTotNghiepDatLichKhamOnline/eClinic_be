package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * booking-service (cổng 8084): đặt/đổi/hủy lịch hẹn, phiếu khám, hồ sơ bệnh nhân theo CCCD — BOOK-01..10, PAT-01..03, ADM-04.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code booking}.
 */
@SpringBootApplication
public class BookingServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(BookingServiceApplication.class, args);
    }

}
