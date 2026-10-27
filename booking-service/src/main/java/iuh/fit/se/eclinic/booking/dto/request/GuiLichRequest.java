package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Gửi lịch làm việc cho các bác sĩ có ca còn hoạt động trong [tuNgay, denNgay] (tính từ hôm nay; tối đa 42 ngày). */
public record GuiLichRequest(
        @NotNull LocalDate tuNgay,
        @NotNull LocalDate denNgay,
        Long idChuyenKhoa,
        @Size(max = 200) List<@NotNull Long> cacIdBacSi) {
}
