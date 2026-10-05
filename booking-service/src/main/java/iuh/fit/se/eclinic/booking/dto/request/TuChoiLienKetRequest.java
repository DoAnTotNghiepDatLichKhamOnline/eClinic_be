package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Size;

/** Quản trị viên từ chối gắn hồ sơ bệnh nhân vào tài khoản; lý do không bắt buộc. */
public record TuChoiLienKetRequest(

        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String lyDo) {
}
