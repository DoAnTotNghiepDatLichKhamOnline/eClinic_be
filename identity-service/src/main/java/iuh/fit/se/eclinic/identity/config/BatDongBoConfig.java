package iuh.fit.se.eclinic.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật @Async (gửi email chạy nền, không bắt request chờ SMTP) với executor mặc định applicationTaskExecutor của Boot,
 * và @Scheduled (job dọn phiên hết hạn) với scheduler riêng của Boot (1 thread).
 */
@Configuration
@EnableAsync
@EnableScheduling
public class BatDongBoConfig {
}
