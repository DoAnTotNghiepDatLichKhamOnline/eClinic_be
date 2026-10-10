package iuh.fit.se.eclinic.booking.dto.request;

import java.time.LocalDate;
import java.time.LocalTime;

import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Bác sĩ xin đổi ca hoặc xin nghỉ 1 ca của mình (SCHED-02).
 *
 * @param ngayMongMuon        bắt buộc với DOI_CA (cùng {@code gioBatDauMongMuon}, {@code gioKetThucMongMuon}); bỏ qua
 *                            với XIN_NGHI
 * @param idPhongKhamMongMuon chỉ với DOI_CA; null = giữ phòng khám hiện tại
 */
public record GuiYeuCauDoiLichRequest(
        @NotNull Long idLichLamViec,
        @NotNull LoaiYeuCauDoiLich loaiYeuCau,
        @NotBlank @Size(max = 1000) String lyDo,
        LocalDate ngayMongMuon,
        LocalTime gioBatDauMongMuon,
        LocalTime gioKetThucMongMuon,
        Long idPhongKhamMongMuon) {
}
