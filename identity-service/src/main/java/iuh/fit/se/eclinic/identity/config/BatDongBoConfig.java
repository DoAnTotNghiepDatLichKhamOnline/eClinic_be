package iuh.fit.se.eclinic.identity.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Bật @Async (gửi email chạy nền) và @Scheduled (job dọn phiên hết hạn) với executor / scheduler mặc định của Boot. */
@Configuration
@EnableAsync
@EnableScheduling
public class BatDongBoConfig {
}
