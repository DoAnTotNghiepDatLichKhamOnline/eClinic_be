package iuh.fit.se.eclinic.booking.dto.request;

import jakarta.validation.constraints.Size;

/** Hủy 1 lịch hẹn. */
public record HuyLichRequest(
        @Size(max = 500, message = "{kiemTra.lyDoHuy.toiDa}")
        String lyDo,

        String soDienThoai) {
}
