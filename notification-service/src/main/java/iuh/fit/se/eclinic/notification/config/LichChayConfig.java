package iuh.fit.se.eclinic.notification.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật @Scheduled (job gửi email đang chờ) với scheduler riêng của Boot (1 thread).
 */
@Configuration
@EnableScheduling
public class LichChayConfig {
}
