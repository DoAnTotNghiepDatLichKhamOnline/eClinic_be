package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Size;

/** Quản trị viên từ chối gắn hồ sơ bệnh nhân vào tài khoản; lý do không bắt buộc. */
public record TuChoiLienKetRequest(

        @Size(max = 500, message = "{kiemTra.lyDo.toiDa}")
        String lyDo) {
}
