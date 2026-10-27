package iuh.fit.se.eclinic.booking.dto.response;

/** Thông tin bác sĩ đủ để hiển thị cạnh khung giờ khám. */
public record BacSiTomTatResponse(Long id, String hoTen, String hocVi, String anhDaiDien) {
}
