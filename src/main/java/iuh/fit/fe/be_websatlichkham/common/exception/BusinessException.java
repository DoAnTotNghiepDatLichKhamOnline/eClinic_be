package iuh.fit.fe.be_websatlichkham.common.exception;

import lombok.Getter;

/**
 * Lỗi nghiệp vụ có mã lỗi. Sau này GlobalExceptionHandler sẽ map sang HTTP response.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        this(errorCode, errorCode.getDefaultMessage());
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

}
