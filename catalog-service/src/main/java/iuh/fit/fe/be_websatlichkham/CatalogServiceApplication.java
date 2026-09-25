package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * catalog-service (cổng 8082): chuyên khoa, phòng khám, hồ sơ bác sĩ, tìm kiếm bác sĩ — ADM-01, ADM-02, AUTH-04, DOC-01..03.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code catalog}.
 */
@SpringBootApplication
public class CatalogServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

}
