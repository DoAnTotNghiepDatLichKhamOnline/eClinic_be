package iuh.fit.se.eclinic.identity.dto.request;

import iuh.fit.se.eclinic.common.enums.TrangThaiTaiKhoan;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Quản trị viên vô hiệu hoá (VO_HIEU_HOA) hoặc kích hoạt lại (DA_KICH_HOAT) 1 tài khoản (UC-USER-01).
 *
 * @param lyDo bắt buộc khi vô hiệu hoá (được gửi cho chủ tài khoản qua email); bỏ qua khi kích hoạt lại
 */
public record CapNhatTrangThaiTaiKhoanRequest(

        @NotNull(message = "{kiemTra.trangThai.batBuoc}")
        TrangThaiTaiKhoan trangThai,

        @Size(max = 500, message = "{kiemTra.lyDo.toiDa}")
        String lyDo) {
}
