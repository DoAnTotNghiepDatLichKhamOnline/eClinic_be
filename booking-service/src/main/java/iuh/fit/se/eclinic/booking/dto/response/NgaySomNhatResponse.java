package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

/**
 * Ngày còn chỗ sớm nhất của 1 bác sĩ (hiện trên thẻ bác sĩ) và số chỗ còn lại của ngày đó.
 */
public record NgaySomNhatResponse(Long idBacSi, LocalDate ngay, long soChoConLai) {
}
