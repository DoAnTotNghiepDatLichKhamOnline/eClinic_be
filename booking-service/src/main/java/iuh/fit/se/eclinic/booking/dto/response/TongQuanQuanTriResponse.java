package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * Số liệu màn hình Dashboard của quản trị viên.
 *
 * @param bacSiDangCongTac  bác sĩ đang công tác có tài khoản đã kích hoạt (đúng tập bác sĩ của danh sách công khai)
 * @param benhNhanDaDangKy  tài khoản bệnh nhân đã kích hoạt; khách chỉ đặt lịch không có tài khoản thì không tính
 * @param luotDatTrongThang lịch hẹn được tạo từ 00:00 ngày 1 của tháng này tới lúc tính, mọi trạng thái trừ lịch đã hủy
 *                          do đổi lịch (1 lần đổi lịch không bị đếm 2 lần)
 * @param tinhLuc           thời điểm tính các số trên
 */
public record TongQuanQuanTriResponse(
        long bacSiDangCongTac,
        long benhNhanDaDangKy,
        long luotDatTrongThang,
        LocalDateTime tinhLuc) {
}
