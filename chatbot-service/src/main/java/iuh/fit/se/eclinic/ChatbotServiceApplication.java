package iuh.fit.se.eclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import iuh.fit.se.eclinic.common.config.AppTimeZone;

/**
 * chatbot-service (cổng 8087): chatbot (trợ lý AI) hướng dẫn dùng trang và trả lời câu hỏi sức khỏe thông thường —
 * UC-CHAT-01. Mô hình ngôn ngữ gọi qua API kiểu OpenAI, cấu hình ở app.chatbot.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code chatbot}.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }

}
