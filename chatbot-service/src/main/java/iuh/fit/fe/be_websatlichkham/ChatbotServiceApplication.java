package iuh.fit.fe.be_websatlichkham;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import iuh.fit.fe.be_websatlichkham.common.config.AppTimeZone;

/**
 * chatbot-service (cổng 8087): chatbot gợi ý chuyên khoa, hướng dẫn đặt lịch — CHAT-01.
 * <p>
 * Đặt ở package gốc để scan được cả entity/config trong module common lẫn code trong package {@code chatbot}.
 */
@SpringBootApplication
public class ChatbotServiceApplication {

    public static void main(String[] args) {
        AppTimeZone.apply();
        SpringApplication.run(ChatbotServiceApplication.class, args);
    }

}
