package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

/** 1 ngày bệnh viện nghỉ cho trang đặt lịch (làm mờ ngày trên lịch, hiện lý do). */
public record NgayNghiResponse(LocalDate ngay, String lyDo) {
}
