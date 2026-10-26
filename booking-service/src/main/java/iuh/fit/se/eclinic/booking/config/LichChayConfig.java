package iuh.fit.se.eclinic.booking.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bật @Scheduled (job gửi thông báo sang notification-service) với scheduler riêng của Boot (1 thread).
 */
@Configuration
@EnableScheduling
public class LichChayConfig {
}
