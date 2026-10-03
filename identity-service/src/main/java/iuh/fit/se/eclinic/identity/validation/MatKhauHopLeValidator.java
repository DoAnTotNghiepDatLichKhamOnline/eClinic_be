package iuh.fit.se.eclinic.identity.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MatKhauHopLeValidator implements ConstraintValidator<MatKhauHopLe, String> {

    static final int DO_DAI_TOI_THIEU = 6;
    /** BCrypt chỉ nhận tối đa 72 byte; tiếng Việt có dấu mỗi ký tự 2-3 byte nên phải đếm byte, không đếm ký tự. */
    static final int SO_BYTE_TOI_DA = 72;

    @Override
    public boolean isValid(String matKhau, ConstraintValidatorContext context) {
        String thongDiep = null;
        if (matKhau == null || matKhau.isEmpty()) {
            thongDiep = "Mật khẩu không được để trống";
        } else if (matKhau.length() < DO_DAI_TOI_THIEU) {
            thongDiep = "Mật khẩu tối thiểu " + DO_DAI_TOI_THIEU + " ký tự";
        } else if (matKhau.getBytes(StandardCharsets.UTF_8).length > SO_BYTE_TOI_DA) {
            thongDiep = "Mật khẩu quá dài (tối đa " + SO_BYTE_TOI_DA + " byte)";
        }
        if (thongDiep == null) {
            return true;
        }
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(thongDiep).addConstraintViolation();
        return false;
    }

}
