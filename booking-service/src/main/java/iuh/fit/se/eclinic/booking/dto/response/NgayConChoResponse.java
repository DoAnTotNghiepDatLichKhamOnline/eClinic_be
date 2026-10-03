package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

/**
 * 1 ngày có ca làm việc đặt lịch được. {@code soChoConLai == 0}: ngày đã hết chỗ.
 */
public record NgayConChoResponse(LocalDate ngay, long soChoConLai) {
}
