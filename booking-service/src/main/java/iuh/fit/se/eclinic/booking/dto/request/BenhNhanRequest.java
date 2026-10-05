package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Thông tin người khám trên form đặt lịch (BOOK-01). Ngày sinh bắt buộc để tính tuổi theo ngày khám (BOOK-11).
 * Trường không bắt buộc: bỏ trống hoặc gửi chuỗi rỗng đều được.
 *
 * @param cccd         số CCCD 12 chữ số. Bắt buộc với người từ đủ 18 tuổi (tính theo ngày khám); người dưới 18 tuổi chưa
 *                     có CCCD thì bỏ trống, hồ sơ được nhận diện bằng họ tên + ngày sinh + CCCD người giám hộ (quy tắc #10)
 * @param email        email liên hệ của lượt khám (không bắt buộc), lưu theo lịch hẹn
 * @param soBaoHiemYTe số thẻ bảo hiểm y tế (không bắt buộc), ghi vào hồ sơ nếu hồ sơ chưa có
 * @param diaChi       địa chỉ (không bắt buộc), ghi vào hồ sơ nếu hồ sơ chưa có
 */
public record BenhNhanRequest(
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

        @Pattern(regexp = "^(\\d{12})?$", message = "Số CCCD gồm đúng 12 chữ số")
        String cccd,

        @Email(message = "Email không hợp lệ")
        @Size(max = 255, message = "Email tối đa 255 ký tự")
        String email,

        @Size(max = 50, message = "Số thẻ bảo hiểm y tế tối đa 50 ký tự")
        String soBaoHiemYTe,

        @Size(max = 500, message = "Địa chỉ tối đa 500 ký tự")
        String diaChi) {
}
