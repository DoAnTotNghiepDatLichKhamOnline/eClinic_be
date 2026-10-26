package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/** Quy tắc đặt lịch / xếp ca đang dùng, cho form của quản trị viên. */
public record CauHinhQuanTriResponse(
        int datTruocToiThieuPhut,
        int soNgayDatTruocToiDa,
        int soLichHieuLucToiDaMoiHoSo,
        int soLichHieuLucToiDaMoiSoDienThoai,
        int huyDoiTruocToiThieuPhut,
        int soLanDoiLichToiDa,
        int guiYeuCauTruocToiThieuGio,
        int soNgayXepTruocToiDa,
        int soGioToiDaMoiTuan,
        int soNgayDuocSuaDanhGia,
        int phienBan,
        LocalDateTime ngaySua,
        QuanTriThucHien quanTriSua) {

    public record QuanTriThucHien(Long id, String hoTen) {
    }
}
