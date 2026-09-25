package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * api-gateway (cổng 8080): cổng vào duy nhất cho frontend — định tuyến tới các service, kiểm tra JWT.
 * Không dùng DB nên không phụ thuộc module common.
 * <p>
 * TODO: thêm Spring Cloud Gateway (xem ghi chú trong pom.xml) và khai báo route:
 * /api/auth/** -> identity-service, /api/chuyen-khoa/** -> catalog-service, ...
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
