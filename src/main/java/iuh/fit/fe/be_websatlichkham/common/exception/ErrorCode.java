package iuh.fit.fe.be_websatlichkham.common.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Mã lỗi nghiệp vụ dùng chung. Thêm mã mới ở đây khi viết nghiệp vụ / API.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Chung
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Không tìm thấy dữ liệu"),
    INVALID_ARGUMENT(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ"),

    // Tài khoản / xác thực
    OTP_INVALID(HttpStatus.BAD_REQUEST, "Mã OTP không đúng"),
    OTP_EXPIRED(HttpStatus.BAD_REQUEST, "Mã OTP đã hết hạn hoặc không tồn tại"),
    OTP_TOO_MANY_ATTEMPTS(HttpStatus.TOO_MANY_REQUESTS, "Nhập sai OTP quá nhiều lần"),

    // Lịch làm việc / đặt lịch
    SCHEDULE_OVERLAP(HttpStatus.CONFLICT, "Lịch làm việc bị trùng giờ"),
    SLOT_NOT_AVAILABLE(HttpStatus.CONFLICT, "Khung giờ không còn trống");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

}
