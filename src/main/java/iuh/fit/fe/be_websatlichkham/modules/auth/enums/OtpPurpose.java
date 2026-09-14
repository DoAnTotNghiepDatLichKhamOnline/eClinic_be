package iuh.fit.fe.be_websatlichkham.modules.auth.enums;

/**
 * Mục đích OTP (một phần của Redis key otp:{userId}:{purpose}).
 */
public enum OtpPurpose {
    REGISTER,
    RESET_PASSWORD
}
