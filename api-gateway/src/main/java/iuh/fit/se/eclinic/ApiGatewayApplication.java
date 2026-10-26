package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** api-gateway (cổng 8080): cổng vào duy nhất cho frontend (Spring Cloud Gateway Server WebMVC). */
@SpringBootApplication
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }

}
