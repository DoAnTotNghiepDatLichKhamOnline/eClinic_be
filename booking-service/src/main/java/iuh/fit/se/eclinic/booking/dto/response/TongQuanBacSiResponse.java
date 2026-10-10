package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * Số liệu màn hình Dashboard của bác sĩ đang đăng nhập.
 *
 * @param lichHenHomNay    lịch hẹn của tôi có giờ khám hôm nay, không tính lịch đã hủy, đã hủy do đổi lịch, bị từ chối
 * @param dangCho          trong số đó: chờ xác nhận hoặc đã xác nhận, và không bị đánh dấu cần đổi lịch (chưa khám)
 * @param daKham           trong số đó: đã hoàn thành
 * @param diemDanhGia      điểm đánh giá trung bình của tôi (mọi thời gian), 1 chữ số thập phân; null khi chưa có
 * @param soDanhGia        số lượt đánh giá
 * @param yeuCauChoXacNhan số lịch hẹn đang chờ tôi xác nhận từ bây giờ trở đi (bằng tổng của danh sách Appointment
 *                         Requests)
 * @param tinhLuc          thời điểm tính các số trên
 */
public record TongQuanBacSiResponse(
        long lichHenHomNay,
        long dangCho,
        long daKham,
        Double diemDanhGia,
        long soDanhGia,
        long yeuCauChoXacNhan,
        LocalDateTime tinhLuc) {
}
