package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * medical-service (cổng 8085): hồ sơ bệnh án, đơn thuốc, danh mục thuốc, sổ khám bệnh — EXAM-01..05, DOC-02.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code medical}.
 */
@SpringBootApplication
public class MedicalServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(MedicalServiceApplication.class, args);
    }

}
