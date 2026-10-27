package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.booking.dto.response.CauHinhQuanTriResponse.QuanTriThucHien;

/** 1 ngày bệnh viện nghỉ, cho quản trị viên: cả 2 bản lý do, ai khai báo, lúc nào. */
public record NgayNghiQuanTriResponse(
        Long id,
        LocalDate ngay,
        String lyDo,
        String lyDoEn,
        QuanTriThucHien quanTriTao,
        LocalDateTime ngayTao) {
}
