package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * api-gateway (cổng 8080): cổng vào duy nhất cho frontend (Spring Cloud Gateway Server WebMVC).
 * <ul>
 *   <li>Route khai báo trong application.yml: /api/&lt;service&gt;/** -> service đó, giữ nguyên đường dẫn.</li>
 *   <li>CORS chỉ cấu hình ở đây ({@code gateway.config.CorsConfig}).</li>
 *   <li>Gateway KHÔNG kiểm tra JWT: header Authorization được chuyển nguyên, mỗi service tự kiểm tra.</li>
 *   <li>Service chưa chạy / phản hồi quá lâu -> 503 / 504 ({@code gateway.exception.XuLyLoiGatewayHandler}).</li>
 * </ul>
 * Không dùng DB nên không phụ thuộc module common.
 */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
