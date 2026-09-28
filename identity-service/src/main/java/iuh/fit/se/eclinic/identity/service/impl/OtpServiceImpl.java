package iuh.fit.se.eclinic.identity.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.OtpProperties;
import iuh.fit.se.eclinic.identity.enums.OtpPurpose;
import iuh.fit.se.eclinic.identity.service.OtpService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;

    @Override
    public String tao(Long userId, OtpPurpose purpose) {
        String code = randomDigits(otpProperties.length());
        redisTemplate.opsForValue().set(otpKey(userId, purpose), code, otpProperties.ttl());
        redisTemplate.delete(attemptsKey(userId, purpose));
        return code;
    }

    @Override
    public void xacThuc(Long userId, OtpPurpose purpose, String code) {
        String otpKey = otpKey(userId, purpose);
        String attemptsKey = attemptsKey(userId, purpose);

        String storedCode = redisTemplate.opsForValue().get(otpKey);
        if (storedCode == null) {
            throw new LoiNghiepVu(MaLoi.OTP_HET_HAN);
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, otpProperties.ttl());
        }
        if (attempts != null && attempts > otpProperties.maxAttempts()) {
            huy(userId, purpose);
            throw new LoiNghiepVu(MaLoi.OTP_NHAP_SAI_QUA_NHIEU);
        }

        boolean matches = code != null && MessageDigest.isEqual(
                storedCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new LoiNghiepVu(MaLoi.OTP_KHONG_DUNG);
        }

        huy(userId, purpose);
    }

    @Override
    public void huy(Long userId, OtpPurpose purpose) {
        redisTemplate.delete(otpKey(userId, purpose));
        redisTemplate.delete(attemptsKey(userId, purpose));
    }

    private static String otpKey(Long userId, OtpPurpose purpose) {
        return "otp:" + userId + ":" + purpose.name();
    }

    private static String attemptsKey(Long userId, OtpPurpose purpose) {
        return "otp:attempts:" + userId + ":" + purpose.name();
    }

    private static String randomDigits(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

}
