package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/** chatbot-service (cổng 8087): trợ lý AI hướng dẫn dùng trang và trả lời câu hỏi sức khỏe thông thường. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }

}
