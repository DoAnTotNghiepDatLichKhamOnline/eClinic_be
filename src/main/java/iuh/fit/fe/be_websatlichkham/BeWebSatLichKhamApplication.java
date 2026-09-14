package iuh.fit.fe.be_websatlichkham;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class BeWebSatLichKhamApplication {

    public static final String DEFAULT_TIME_ZONE = "Asia/Ho_Chi_Minh";

    public static void main(String[] args) {
        // Toàn hệ thống dùng giờ Việt Nam (lịch khám là giờ địa phương).
        TimeZone.setDefault(TimeZone.getTimeZone(DEFAULT_TIME_ZONE));
        SpringApplication.run(BeWebSatLichKhamApplication.class, args);
    }

}
