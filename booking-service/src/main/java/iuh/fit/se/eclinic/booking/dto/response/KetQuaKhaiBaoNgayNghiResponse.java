package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Kết quả (hoặc xem trước) khai báo ngày bệnh viện nghỉ.
 *
 * @param daLuu           false = xem trước: không có gì được lưu, không ai được báo
 * @param ngayMoi         các ngày vừa thành ngày nghỉ, theo ngày (khi xem trước, id là null)
 * @param ngayDaNghi      các ngày trong khoảng đã là ngày nghỉ từ trước: bỏ qua, không đổi gì
 * @param caBiHuy         các ca còn hoạt động của ngày mới nghỉ, bị hủy theo quy trình "cần đổi lịch"; theo ngày, giờ
 * @param soLichHenCanDoi tổng số lịch hẹn còn hiệu lực của các ca đó: giữ nguyên, được đánh dấu cần đổi lịch, bệnh nhân
 *                        được báo và nhận email
 */
public record KetQuaKhaiBaoNgayNghiResponse(
        boolean daLuu,
        List<NgayNghiQuanTriResponse> ngayMoi,
        List<LocalDate> ngayDaNghi,
        List<CaBiHuy> caBiHuy,
        int soLichHenCanDoi) {

    /** @param soLichHenCanDoi số lịch hẹn còn hiệu lực của ca này */
    public record CaBiHuy(
            Long idLichLamViec,
            LocalDate ngay,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            BacSiTomTatResponse bacSi,
            PhongKhamTomTatResponse phongKham,
            int soLichHenCanDoi) {
    }
}
