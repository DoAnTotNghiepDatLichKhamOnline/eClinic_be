package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thông tin người giám hộ, bắt buộc khi người khám dưới 18 tuổi (BOOK-11). Ngày sinh bắt buộc để kiểm tra người giám
 * hộ từ đủ 18 tuổi.
 */
public record NguoiGiamHoRequest(
        @NotBlank(message = "Họ tên người giám hộ không được để trống")
        @Size(max = 150, message = "Họ tên người giám hộ tối đa 150 ký tự")
        String hoTen,

        @NotNull(message = "Chưa chọn quan hệ với bệnh nhân")
        QuanHeGiamHo quanHe,

        @NotBlank(message = "Số điện thoại người giám hộ không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        @NotBlank(message = "Số CCCD người giám hộ không được để trống")
        @Pattern(regexp = "^\\d{12}$", message = "Số CCCD gồm đúng 12 chữ số")
        String cccd,

        @NotNull(message = "Ngày sinh người giám hộ không được để trống")
        @Past(message = "Ngày sinh người giám hộ phải ở quá khứ")
        LocalDate ngaySinh) {
}
