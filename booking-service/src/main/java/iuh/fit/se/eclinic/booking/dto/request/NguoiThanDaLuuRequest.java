package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Sửa thông tin đã lưu của 1 người thân, trừ số CCCD (thứ nối bản lưu với hồ sơ bệnh nhân). */
public record NguoiThanDaLuuRequest(
        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @NotNull(message = "{kiemTra.ngaySinh.batBuoc}")
        @PastOrPresent(message = "{kiemTra.ngaySinh.tuongLai}")
        LocalDate ngaySinh,

        @NotNull(message = "{kiemTra.gioiTinh.batBuoc}")
        GioiTinh gioiTinh,

        @NotBlank(message = "{kiemTra.soDienThoai.batBuoc}")
        @Pattern(regexp = "^0\\d{9}$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        @Email(message = "{kiemTra.email.khongHopLe}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @Size(max = 50, message = "{kiemTra.soTheBaoHiemYTe.toiDa}")
        String soBaoHiemYTe,

        @Size(max = 500, message = "{kiemTra.diaChi.toiDa}")
        String diaChi,

        @Valid
        NguoiGiamHoRequest nguoiGiamHo) {
}
