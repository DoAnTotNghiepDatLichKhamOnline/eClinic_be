package iuh.fit.fe.be_websatlichkham.modules.auth.service;

import iuh.fit.fe.be_websatlichkham.modules.auth.enums.OtpPurpose;

/**
 * OTP lưu ở Redis (không có bảng SQL). Key: {@code otp:{userId}:{purpose}}, TTL = app.otp.ttl.
 */
public interface OtpService {

    /**
     * Sinh mã OTP mới (ghi đè mã cũ nếu có) và trả về để gửi email/SMS.
     */
    String generate(Long userId, OtpPurpose purpose);

    /**
     * Kiểm tra mã. Đúng -> xoá mã (dùng 1 lần). Sai -> ném BusinessException
     * (OTP_INVALID / OTP_EXPIRED / OTP_TOO_MANY_ATTEMPTS).
     */
    void verify(Long userId, OtpPurpose purpose, String code);

    void invalidate(Long userId, OtpPurpose purpose);

}
