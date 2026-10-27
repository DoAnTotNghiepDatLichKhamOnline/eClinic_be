package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

/** 1 ngày có ca làm việc đặt lịch được. */
public record NgayConChoResponse(LocalDate ngay, long soChoConLai) {
}
