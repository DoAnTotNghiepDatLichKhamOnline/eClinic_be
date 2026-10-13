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

        @Pattern(regexp = "^\\d{12}$", message = "{kiemTra.cccd.dinhDang}")
        String cccd,

        @Size(max = 500, message = "{kiemTra.diaChi.toiDa}")
        String diaChi,

        @Size(max = 50, message = "{kiemTra.soBaoHiemYTe.toiDa}")
        String soBaoHiemYTe,

        @Size(max = 5000, message = "{kiemTra.tienSuBenhLy.toiDa}")
        String tienSuBenhLy) {
}
