package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thông tin người khám trên form đặt lịch (BOOK-01). Ngày sinh bắt buộc để tính tuổi theo ngày khám (BOOK-11).
 *
 * @param cccd số CCCD 12 chữ số; trẻ chưa có CCCD dùng số định danh cá nhân trên giấy khai sinh
 */
public record BenhNhanRequest(
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

        @NotBlank(message = "Số CCCD không được để trống")
        @Pattern(regexp = "^\\d{12}$", message = "Số CCCD gồm đúng 12 chữ số")
        String cccd) {
}
