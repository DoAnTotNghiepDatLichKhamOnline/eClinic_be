package iuh.fit.se.eclinic.identity.service;

import iuh.fit.se.eclinic.identity.enums.OtpPurpose;

/**
 * OTP lưu ở Redis (không có bảng SQL). Key: {@code otp:{userId}:{purpose}}, TTL = app.otp.ttl.
 * <p>
 * TODO: giữ tạm từ bản cũ. Theo yêu cầu (AUTH-01, AUTH-03) hệ thống dùng link kích hoạt / đặt lại mật khẩu
 * qua email, không dùng OTP -> thay bằng service token link (Redis, TTL, dùng 1 lần) khi viết nghiệp vụ.
 */
public interface OtpService {

    /**
     * Sinh mã OTP mới (ghi đè mã cũ nếu có) và trả về để gửi email/SMS.
     */
    String tao(Long userId, OtpPurpose purpose);

    /**
     * Kiểm tra mã. Đúng -> xoá mã (dùng 1 lần). Sai -> ném LoiNghiepVu
     * (OTP_KHONG_DUNG / OTP_HET_HAN / OTP_NHAP_SAI_QUA_NHIEU).
     */
    void xacThuc(Long userId, OtpPurpose purpose, String code);

    void huy(Long userId, OtpPurpose purpose);

}
