package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import iuh.fit.se.eclinic.common.enums.LoaiYeuCauDoiLich;
import iuh.fit.se.eclinic.common.enums.TrangThaiYeuCau;

/**
 * 1 yêu cầu đổi ca / xin nghỉ / nhường ca, cho bác sĩ đã gửi, bác sĩ nhận ca và quản trị viên.
 *
 * @param bacSi                bác sĩ đã gửi (sau khi duyệt nhường ca, {@code ca.bacSi} là bác sĩ nhận)
 * @param ca                   ca làm việc của yêu cầu, theo trạng thái hiện tại (kèm số lượt đã có người đặt: duyệt
 *                             yêu cầu thì các lịch hẹn đó phải đổi lịch, hoặc chuyển sang bác sĩ nhận)
 * @param ngayMongMuon         chỉ có với DOI_CA (cùng 2 trường giờ)
 * @param phongKhamMongMuon    chỉ có với DOI_CA; null = giữ phòng khám hiện tại
 * @param bacSiNhan            chỉ có với NHUONG_CA
 * @param ghiChuBacSiNhan      NHUONG_CA: ghi chú của bác sĩ nhận khi từ chối; null nếu không có
 * @param ngayBacSiNhanPhanHoi NHUONG_CA: lúc bác sĩ nhận đồng ý / từ chối; null khi còn CHO_DONG_Y
 * @param maNhom               các yêu cầu xin nghỉ gửi cùng 1 lần mang chung mã; null với yêu cầu gửi lẻ
 * @param ghiChuXuLy           ghi chú của quản trị viên khi duyệt / từ chối (nguyên văn), hoặc câu của hệ thống khi ca bị
 *                             hủy làm đóng yêu cầu (theo ngôn ngữ của request); null khi còn chờ hoặc đã rút
 * @param ngayXuLy             thời điểm duyệt / từ chối / rút
 */
public record YeuCauDoiLichResponse(Long id, LoaiYeuCauDoiLich loaiYeuCau, TrangThaiYeuCau trangThai, String lyDo,
        BacSiTomTatResponse bacSi, CaLamViecResponse ca, LocalDate ngayMongMuon, LocalTime gioBatDauMongMuon,
        LocalTime gioKetThucMongMuon, PhongKhamTomTatResponse phongKhamMongMuon, BacSiTomTatResponse bacSiNhan,
        String ghiChuBacSiNhan, LocalDateTime ngayBacSiNhanPhanHoi, String maNhom, String ghiChuXuLy,
        LocalDateTime ngayGui, LocalDateTime ngayXuLy) {
}
