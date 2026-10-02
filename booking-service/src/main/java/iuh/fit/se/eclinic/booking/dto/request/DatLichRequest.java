package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Đặt lịch khám (BOOK-01). Khung giờ chọn bằng {@code idLichLamViec} + {@code gioBatDauKhung}, đúng 2 giá trị mà
 * GET /api/booking/khung-gio trả về; ca làm việc đã xác định bác sĩ và phòng khám.
 *
 * @param gioBatDauKhung giờ bắt đầu khung 1 giờ, vd 2026-10-05T08:00:00
 * @param nguoiGiamHo    bắt buộc khi người khám dưới 18 tuổi tính theo ngày khám; từ đủ 18 tuổi thì bị bỏ qua
 * @param datChoBanThan  chỉ có nghĩa khi bệnh nhân đã đăng nhập: true = người khám là chủ tài khoản, lịch dùng hồ sơ bệnh
 *                       nhân của tài khoản (chưa có thì tạo và gắn vào tài khoản); bỏ trống / false = đặt cho người thân,
 *                       hồ sơ theo CCCD như khách đặt
 */
public record DatLichRequest(
        @NotNull(message = "Chưa chọn ca khám")
        Long idLichLamViec,

        @NotNull(message = "Chưa chọn khung giờ khám")
        LocalDateTime gioBatDauKhung,

        @NotNull(message = "Thiếu thông tin bệnh nhân")
        @Valid
        BenhNhanRequest benhNhan,

        @Valid
        NguoiGiamHoRequest nguoiGiamHo,

        @Size(max = 500, message = "Lý do khám tối đa 500 ký tự")
        String lyDoKham,

        Boolean datChoBanThan) {
}
