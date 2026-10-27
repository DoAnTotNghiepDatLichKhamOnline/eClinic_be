package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** catalog-service (cổng 8082): chuyên khoa, phòng khám, hồ sơ bác sĩ, tìm kiếm bác sĩ. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(CatalogServiceApplication.class, args);
    }

}
