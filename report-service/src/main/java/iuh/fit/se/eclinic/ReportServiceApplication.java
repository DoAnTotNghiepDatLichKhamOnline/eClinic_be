package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** report-service (cổng 8088): thống kê, xuất báo cáo. */
@SpringBootApplication
public class ReportServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(ReportServiceApplication.class, args);
    }

}
