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

        @Pattern(regexp = "^(\\d{12})?$", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @Email(message = "{kiemTra.email.khongHopLe}")
        @Size(max = 255, message = "{kiemTra.email.toiDa}")
        String email,

        @Size(max = 50, message = "{kiemTra.soTheBaoHiemYTe.toiDa}")
        String soBaoHiemYTe,

        @Size(max = 500, message = "{kiemTra.diaChi.toiDa}")
        String diaChi) {
}
