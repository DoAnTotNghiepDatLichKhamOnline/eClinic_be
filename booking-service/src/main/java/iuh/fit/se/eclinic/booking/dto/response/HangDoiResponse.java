package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.BuoiKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;

/**
 * Hàng đợi khám 1 ngày của bác sĩ, theo từng ca đang hoạt động (giờ bắt đầu sớm nhất trước).
 *
 * @param idCaHienTai ca nên mở sẵn: hôm nay là ca đang diễn ra, không có thì ca kế tiếp trong ngày, không có nữa thì
 *                    ca cuối; ngày khác là ca đầu tiên. null khi ngày đó không có ca
 * @param tinhLuc     thời điểm máy chủ tính hàng đợi
 */
public record HangDoiResponse(
        LocalDate ngay,
        Long idCaHienTai,
        LocalDateTime tinhLuc,
        List<CaHangDoi> cacCa) {

    /**
     * @param idDangKham id lịch hẹn đang khám của ca (null nếu không có)
     * @param idTiepTheo id lịch hẹn sẽ được gọi tiếp: lượt CHO_KHAM có số thứ tự nhỏ nhất; chỉ có khi là hôm nay
     * @param cacLuot    theo số thứ tự
     */
    public record CaHangDoi(
            Long idLichLamViec,
            BuoiKham buoi,
            LocalTime gioBatDau,
            LocalTime gioKetThuc,
            PhongKhamTomTatResponse phongKham,
            SoLuong soLuong,
            Long idDangKham,
            Long idTiepTheo,
            List<LuotHangDoi> cacLuot) {
    }

    public record SoLuong(int tong, int choKham, int dangKham, int vangMat, int daKham) {
    }

    /** @param goiLuc lần gần nhất bác sĩ gọi vào khám (null nếu chưa gọi) */
    public record LuotHangDoi(
            TrangThaiHangDoi trangThaiHangDoi,
            LocalDateTime goiLuc,
            LichHenTrongCaResponse lichHen) {
    }

}
