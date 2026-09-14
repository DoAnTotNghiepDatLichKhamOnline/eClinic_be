package iuh.fit.fe.be_websatlichkham.modules.auth.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.fe.be_websatlichkham.common.config.OtpProperties;
import iuh.fit.fe.be_websatlichkham.common.exception.BusinessException;
import iuh.fit.fe.be_websatlichkham.common.exception.ErrorCode;
import iuh.fit.fe.be_websatlichkham.modules.auth.enums.OtpPurpose;
import iuh.fit.fe.be_websatlichkham.modules.auth.service.OtpService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final StringRedisTemplate redisTemplate;
    private final OtpProperties otpProperties;

    @Override
    public String generate(Long userId, OtpPurpose purpose) {
        String code = randomDigits(otpProperties.length());
        redisTemplate.opsForValue().set(otpKey(userId, purpose), code, otpProperties.ttl());
        redisTemplate.delete(attemptsKey(userId, purpose));
        return code;
    }

    @Override
    public void verify(Long userId, OtpPurpose purpose, String code) {
        String otpKey = otpKey(userId, purpose);
        String attemptsKey = attemptsKey(userId, purpose);

        String storedCode = redisTemplate.opsForValue().get(otpKey);
        if (storedCode == null) {
            throw new BusinessException(ErrorCode.OTP_EXPIRED);
        }

        Long attempts = redisTemplate.opsForValue().increment(attemptsKey);
        if (attempts != null && attempts == 1) {
            redisTemplate.expire(attemptsKey, otpProperties.ttl());
        }
        if (attempts != null && attempts > otpProperties.maxAttempts()) {
            invalidate(userId, purpose);
            throw new BusinessException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        boolean matches = code != null && MessageDigest.isEqual(
                storedCode.getBytes(StandardCharsets.UTF_8), code.getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            throw new BusinessException(ErrorCode.OTP_INVALID);
        }

        invalidate(userId, purpose);
    }

    @Override
    public void invalidate(Long userId, OtpPurpose purpose) {
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
