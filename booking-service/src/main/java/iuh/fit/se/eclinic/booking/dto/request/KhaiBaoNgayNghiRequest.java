package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Khai báo bệnh viện nghỉ các ngày [tuNgay, denNgay] (từ ngày mai, tối đa 42 ngày). Lý do hiện cho bệnh nhân trên trang
 * đặt lịch và nằm trong thông báo gửi bác sĩ / bệnh nhân có ca, lịch hẹn bị hủy.
 *
 * @param lyDoEn bản tiếng Anh; để trống thì người đọc tiếng Anh thấy câu tiếng Việt
 */
public record KhaiBaoNgayNghiRequest(
        @NotNull(message = "{kiemTra.ngayNghi.tuNgay.batBuoc}") LocalDate tuNgay,
        @NotNull(message = "{kiemTra.ngayNghi.denNgay.batBuoc}") LocalDate denNgay,
        @NotBlank(message = "{kiemTra.ngayNghi.lyDo.batBuoc}")
        @Size(max = 200, message = "{kiemTra.ngayNghi.lyDo.toiDa}") String lyDo,
        @Size(max = 200, message = "{kiemTra.ngayNghi.lyDoEn.toiDa}") String lyDoEn) {
}
