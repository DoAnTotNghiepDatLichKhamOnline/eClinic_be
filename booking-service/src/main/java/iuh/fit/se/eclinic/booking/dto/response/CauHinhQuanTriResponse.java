package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDateTime;

/**
 * Quy tắc đặt lịch / xếp ca đang dùng, cho form của quản trị viên. Ý nghĩa từng trường: xem {@code CauHinhHienTai}.
 *
 * @param phienBan   gửi lại khi lưu; tăng 1 mỗi lần lưu có thay đổi
 * @param ngaySua    lần lưu gần nhất; null = chưa ai sửa (giá trị lấy từ biến môi trường lúc khởi tạo)
 * @param quanTriSua quản trị viên lưu gần nhất; null nếu chưa ai sửa
 */
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
