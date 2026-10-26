package iuh.fit.se.eclinic.medical.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Sinh hiệu đo lúc khám; mọi trường đều không bắt buộc. Huyết áp gửi cả 2 số hoặc không số nào, tâm trương thấp hơn tâm
 * thu (kiểm tra trong service).
 *
 * @param mach             lần/phút
 * @param nhietDo          °C
 * @param huyetApTamThu    mmHg
 * @param huyetApTamTruong mmHg
 * @param nhipTho          lần/phút
 * @param canNang          kg
 * @param chieuCao         cm
 */
public record SinhHieuRequest(
        @Min(value = 20, message = "{kiemTra.mach.khoang}")
        @Max(value = 250, message = "{kiemTra.mach.khoang}")
        Integer mach,

        @DecimalMin(value = "30.0", message = "{kiemTra.nhietDo.khoang}")
        @DecimalMax(value = "45.0", message = "{kiemTra.nhietDo.khoang}")
        @Digits(integer = 2, fraction = 1, message = "{kiemTra.nhietDo.khoang}")
        BigDecimal nhietDo,

        @Min(value = 50, message = "{kiemTra.huyetApTamThu.khoang}")
        @Max(value = 300, message = "{kiemTra.huyetApTamThu.khoang}")
        Integer huyetApTamThu,

        @Min(value = 20, message = "{kiemTra.huyetApTamTruong.khoang}")
        @Max(value = 200, message = "{kiemTra.huyetApTamTruong.khoang}")
        Integer huyetApTamTruong,

        @Min(value = 5, message = "{kiemTra.nhipTho.khoang}")
        @Max(value = 80, message = "{kiemTra.nhipTho.khoang}")
        Integer nhipTho,

        @DecimalMin(value = "0.3", message = "{kiemTra.canNang.khoang}")
        @DecimalMax(value = "400", message = "{kiemTra.canNang.khoang}")
        @Digits(integer = 3, fraction = 2, message = "{kiemTra.canNang.khoang}")
        BigDecimal canNang,

        @DecimalMin(value = "20", message = "{kiemTra.chieuCao.khoang}")
        @DecimalMax(value = "250", message = "{kiemTra.chieuCao.khoang}")
        @Digits(integer = 3, fraction = 1, message = "{kiemTra.chieuCao.khoang}")
        BigDecimal chieuCao) {

    /** Bỏ số 0 thừa sau dấu phẩy trước khi kiểm tra: 38.20 là 1 chữ số thập phân như 38.2. */
    public SinhHieuRequest {
        nhietDo = boSoKhongThua(nhietDo);
        canNang = boSoKhongThua(canNang);
        chieuCao = boSoKhongThua(chieuCao);
    }

    private static BigDecimal boSoKhongThua(BigDecimal so) {
        return so == null ? null : so.stripTrailingZeros();
    }
}
