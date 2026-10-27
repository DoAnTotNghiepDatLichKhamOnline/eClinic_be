package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Thông tin người giám hộ, bắt buộc khi người khám dưới 18 tuổi. */
public record NguoiGiamHoRequest(
        @NotBlank(message = "{kiemTra.hoTenGiamHo.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTenGiamHo.toiDa}")
        String hoTen,

        @NotNull(message = "{kiemTra.quanHe.batBuoc}")
        QuanHeGiamHo quanHe,

        @NotBlank(message = "{kiemTra.soDienThoaiGiamHo.batBuoc}")
        @Pattern(regexp = "^0\\d{9}$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        @NotBlank(message = "{kiemTra.cccdGiamHo.batBuoc}")
        @Pattern(regexp = "^\\d{12}$", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @Past(message = "{kiemTra.ngaySinhGiamHo.quaKhu}")
        LocalDate ngaySinh) {
}
