package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.GioiTinh;
import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;

/** 1 người thân đã lưu, đúng những gì tài khoản nhập ở lần đặt gần nhất, không che. */
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
