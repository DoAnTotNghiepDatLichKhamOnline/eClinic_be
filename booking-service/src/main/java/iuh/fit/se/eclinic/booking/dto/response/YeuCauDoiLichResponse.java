package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/**
 * 1 yêu cầu đổi ca / xin nghỉ, cho bác sĩ đã gửi và cho quản trị viên.
 *
 * @param ca                  ca làm việc của yêu cầu, theo trạng thái hiện tại (kèm số lượt đã có người đặt: duyệt yêu
 *                            cầu thì các lịch hẹn đó phải đổi lịch)
 * @param ngayMongMuon        chỉ có với DOI_CA (cùng 2 trường giờ)
 * @param phongKhamMongMuon   chỉ có với DOI_CA; null = giữ phòng khám hiện tại
 * @param ghiChuXuLy          ghi chú của quản trị viên khi duyệt / từ chối; null khi còn chờ duyệt hoặc đã rút
 * @param ngayXuLy            thời điểm duyệt / từ chối / rút
 */
public record YeuCauDoiLichResponse(Long id, LoaiYeuCauDoiLich loaiYeuCau, TrangThaiYeuCau trangThai, String lyDo,
        CaLamViecResponse ca, LocalDate ngayMongMuon, LocalTime gioBatDauMongMuon, LocalTime gioKetThucMongMuon,
        PhongKhamTomTatResponse phongKhamMongMuon, String ghiChuXuLy, LocalDateTime ngayGui, LocalDateTime ngayXuLy) {
}
