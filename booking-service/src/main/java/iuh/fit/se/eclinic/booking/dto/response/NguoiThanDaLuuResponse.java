package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;

/**
 * 1 người thân đã lưu của tài khoản: đúng những gì tài khoản nhập ở lần đặt lịch gần nhất cho người này, không che
 * (chỉ chủ tài khoản xem). Tên trường trùng với {@code benhNhan} / {@code nguoiGiamHo} của request đặt lịch để điền
 * thẳng vào form.
 *
 * @param id          dùng để bỏ người này khỏi danh sách đã lưu
 * @param cccd        null nếu người dưới 18 tuổi được đặt khi chưa có CCCD
 * @param nguoiGiamHo null nếu lần đặt gần nhất không có người giám hộ
 * @param lanDungCuoi lần đặt lịch gần nhất cho người này
 */
public record NguoiThanDaLuuResponse(
        Long id,
        String hoTen,
        LocalDate ngaySinh,
        GioiTinh gioiTinh,
        String cccd,
        String soDienThoai,
        String email,
        String diaChi,
        String soBaoHiemYTe,
        NguoiGiamHo nguoiGiamHo,
        LocalDateTime lanDungCuoi) {

    public record NguoiGiamHo(String hoTen, QuanHeGiamHo quanHe, String soDienThoai, String cccd,
            LocalDate ngaySinh) {
    }
}
