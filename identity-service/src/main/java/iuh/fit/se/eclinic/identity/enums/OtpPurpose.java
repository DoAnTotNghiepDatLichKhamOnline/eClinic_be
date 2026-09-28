package iuh.fit.se.eclinic.identity.enums;

/**
 * Mục đích OTP (một phần của Redis key otp:{userId}:{purpose}).
 */
public enum OtpPurpose {
    REGISTER,
    RESET_PASSWORD
}
