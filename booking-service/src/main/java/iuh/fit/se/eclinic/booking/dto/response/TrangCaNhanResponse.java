package iuh.fit.se.eclinic.booking.dto.response;

import java.util.List;

/** Mọi thứ trang cá nhân của bệnh nhân hiển thị, trong 1 lần gọi. */
public record TrangCaNhanResponse(
        HoSoCuaToiResponse hoSo,
        String emailTaiKhoan,
        List<NguoiThanDaLuuResponse> nguoiThan,
        LichSapToi lichSapToi,
        List<LanKhamCuaToiResponse> lanKhamGanDay,
        SoLich soLich) {

    public record LichSapToi(List<LichHenCuaToiResponse> cuaToi, List<LichHenCuaToiResponse> cuaNguoiKhac) {
    }

    public record SoLich(long sapToiCuaToi, long sapToiCuaNguoiKhac, long lichSu, long daKham) {
    }
}
