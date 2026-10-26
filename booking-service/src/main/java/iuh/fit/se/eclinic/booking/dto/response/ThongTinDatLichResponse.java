package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/** Mọi thứ để điền sẵn form đặt lịch cho bệnh nhân đã đăng nhập, trong 1 lần gọi. */
public record ThongTinDatLichResponse(
        HoSoCuaToiResponse banThan,
        String emailTaiKhoan,
        List<NguoiThanDaLuuResponse> nguoiThan,
        LanDatGanNhat lanDatGanNhat) {

    public record LanDatGanNhat(Long idChuyenKhoa, String tenChuyenKhoa, Long idBacSi, String hoTenBacSi) {
    }
}
