package iuh.fit.se.eclinic.identity.validation;

import java.nio.charset.StandardCharsets;

import org.hibernate.validator.constraintvalidation.HibernateConstraintValidatorContext;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MatKhauHopLeValidator implements ConstraintValidator<MatKhauHopLe, String> {

    static final int DO_DAI_TOI_THIEU = 6;
    /** BCrypt nhận tối đa 72 byte; tiếng Việt có dấu 2-3 byte mỗi ký tự nên phải đếm byte. */
    static final int SO_BYTE_TOI_DA = 72;

    @Override
    public boolean isValid(String matKhau, ConstraintValidatorContext context) {
        // Khoá trong thong-diep/identity*.properties; {toiThieu}, {toiDaByte} lấy từ hằng số bên trên
        String khoa = null;
        if (matKhau == null || matKhau.isEmpty()) {
            khoa = "kiemTra.matKhau.batBuoc";
        } else if (matKhau.length() < DO_DAI_TOI_THIEU) {
            khoa = "kiemTra.matKhau.toiThieu";
        } else if (matKhau.getBytes(StandardCharsets.UTF_8).length > SO_BYTE_TOI_DA) {
            khoa = "kiemTra.matKhau.toiDaByte";
        }
        if (khoa == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.unwrap(HibernateConstraintValidatorContext.class)
                .addMessageParameter("toiThieu", DO_DAI_TOI_THIEU)
                .addMessageParameter("toiDaByte", SO_BYTE_TOI_DA)
                .buildConstraintViolationWithTemplate("{" + khoa + "}")
                .addConstraintViolation();
        return false;
    }

}
