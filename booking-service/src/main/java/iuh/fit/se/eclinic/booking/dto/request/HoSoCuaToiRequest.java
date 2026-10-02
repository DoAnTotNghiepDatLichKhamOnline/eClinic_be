package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Bệnh nhân tạo / sửa hồ sơ bệnh nhân của tài khoản mình (PAT-01). Các trường tuỳ chọn gửi trống thì bị xoá trắng.
 *
 * @param cccd bắt buộc khi tài khoản chưa có hồ sơ (tạo mới); đã có hồ sơ thì bỏ trống hoặc gửi đúng số đang lưu, vì số
 *             CCCD không đổi được
 */
public record HoSoCuaToiRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String hoTen,

        @NotNull(message = "Ngày sinh không được để trống")
        @PastOrPresent(message = "Ngày sinh không được ở tương lai")
        LocalDate ngaySinh,

        GioiTinh gioiTinh,

        @NotBlank(message = "Số điện thoại không được để trống")
        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại gồm 10 chữ số, bắt đầu bằng 0")
        String soDienThoai,

        @Pattern(regexp = "^\\d{12}$", message = "Số CCCD gồm đúng 12 chữ số")
        String cccd,

        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
        String diaChi,

        @Size(max = 50, message = "Số bảo hiểm y tế tối đa 50 ký tự")
        String soBaoHiemYTe,

        @Size(max = 5000, message = "Tiền sử bệnh lý tối đa 5000 ký tự")
        String tienSuBenhLy) {
}
