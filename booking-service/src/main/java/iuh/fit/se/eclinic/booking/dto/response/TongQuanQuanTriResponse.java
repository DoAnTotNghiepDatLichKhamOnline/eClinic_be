package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/** Số liệu màn hình Dashboard của quản trị viên. */
public record TongQuanQuanTriResponse(
        long bacSiDangCongTac,
        long benhNhanDaDangKy,
        long luotDatTrongThang,
        LocalDateTime tinhLuc) {
}
