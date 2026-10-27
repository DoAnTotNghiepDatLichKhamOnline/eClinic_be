package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import iuh.fit.se.eclinic.common.enums.BuoiKham;
import iuh.fit.se.eclinic.common.enums.TrangThaiHangDoi;

/** Hàng đợi khám 1 ngày của bác sĩ, theo từng ca đang hoạt động (giờ bắt đầu sớm nhất trước). */
public record HangDoiResponse(
        LocalDate ngay,
        Long idCaHienTai,
        LocalDateTime tinhLuc,
        List<CaHangDoi> cacCa) {

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
