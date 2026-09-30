package iuh.fit.se.eclinic.identity.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Mật khẩu hợp lệ: tối thiểu 6 ký tự (quy tắc tạm khi đang thử nghiệm, phải siết lại trước khi triển khai thật)
 * và tối đa 72 byte UTF-8 (giới hạn của BCrypt: vượt quá thì BCryptPasswordEncoder ném lỗi thay vì cắt bớt).
 */
@Documented
@Constraint(validatedBy = MatKhauHopLeValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface MatKhauHopLe {

    String message() default "Mật khẩu không hợp lệ";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
