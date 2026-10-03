package iuh.fit.se.eclinic.identity.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.identity.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 3 giờ sáng mỗi ngày xoá các phiên đăng nhập đã hết hạn. Phiên đã thu hồi nhưng chưa hết hạn được giữ lại
 * để còn phát hiện refresh token cũ bị dùng lại.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DonPhienHetHanJob {

    private final RefreshTokenService refreshTokenService;

    @Scheduled(cron = "0 0 3 * * *")
    public void donPhienHetHan() {
        // Bắt mọi lỗi: lần chạy lỗi (DB tạm mất kết nối...) chỉ ghi log, lần sau chạy lại bình thường
        try {
            int soPhien = refreshTokenService.xoaPhienHetHan();
            log.info("Đã xoá {} phiên đăng nhập hết hạn", soPhien);
        } catch (RuntimeException e) {
            log.error("Xoá phiên đăng nhập hết hạn thất bại", e);
        }
    }

}
