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

/**
 * Sửa thông tin đã lưu của 1 người thân: các trường của {@link BenhNhanRequest} trừ số CCCD (số CCCD là thứ nối bản lưu
 * với hồ sơ bệnh nhân nên không đổi được; nhập sai số thì bỏ người thân này và đặt lịch lại). Trường không bắt buộc gửi
 * trống thì bị xoá trắng.
 *
 * @param nguoiGiamHo bỏ trống thì bản lưu không còn người giám hộ
 */
public record NguoiThanDaLuuRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @NotNull(message = "Ngày sinh không được để trống")
        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate ngaySinh,

        @NotNull(message = "Chưa chọn giới tính")
        GioiTinh gioiTinh,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @Size(max = 50, message = "Số thẻ bảo hiểm y tế tối đa 50 ký tự")
        String soBaoHiemYTe,

        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
        String diaChi,

        @Valid
        NguoiGiamHoRequest nguoiGiamHo) {
}
