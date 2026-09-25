package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * report-service (cổng 8088): thống kê, xuất báo cáo — DASH-01..06, ADM-05. Không sở hữu bảng nào, chỉ đọc DB chung.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code report}.
 */
@SpringBootApplication
public class ReportServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(ReportServiceApplication.class, args);
    }

}
