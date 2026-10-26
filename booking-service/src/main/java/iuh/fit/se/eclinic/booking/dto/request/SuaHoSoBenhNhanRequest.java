package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên sửa hồ sơ bệnh nhân sau khi đối chiếu giấy tờ. Các trường tuỳ chọn gửi trống thì bị xoá trắng.
 *
 * @param cccd chỉ điền được cho hồ sơ chưa có số CCCD (trẻ đặt lịch khi chưa có CCCD); hồ sơ đã có thì bỏ trống hoặc
 *             gửi đúng số đang lưu, vì số CCCD không đổi được
 */
public record SuaHoSoBenhNhanRequest(
        @NotBlank(message = "{kiemTra.hoTen.batBuoc}")
        @Size(max = 150, message = "{kiemTra.hoTen.toiDa}")
        String hoTen,

        @NotNull(message = "{kiemTra.ngaySinh.batBuoc}")
        @PastOrPresent(message = "{kiemTra.ngaySinh.tuongLai}")
        LocalDate ngaySinh,

        GioiTinh gioiTinh,

        @NotBlank(message = "{kiemTra.soDienThoai.batBuoc}")
        @Pattern(regexp = "^0\\d{9}$", message = "{kiemTra.soDienThoai.dinhDang}")
        String soDienThoai,

        @Pattern(regexp = "^(\\d{12})?$", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @Size(max = 500, message = "{kiemTra.diaChi.toiDa}")
        String diaChi,

        @Size(max = 50, message = "{kiemTra.soBaoHiemYTe.toiDa}")
        String soBaoHiemYTe) {
}
