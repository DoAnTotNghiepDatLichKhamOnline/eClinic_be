package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** medical-service (cổng 8085): hồ sơ bệnh án, đơn thuốc, danh mục thuốc, sổ khám bệnh. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MedicalServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(MedicalServiceApplication.class, args);
    }

}
