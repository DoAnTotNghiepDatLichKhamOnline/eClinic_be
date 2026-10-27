package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** 1 dòng trong "lịch hẹn của tôi". */
public record LichHenCuaToiResponse(String maPhieuKham, String maTraCuu, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String hoTenBenhNhan, boolean laBanThan, String hoTenNguoiGiamHo,
        String lyDoKham, LocalDateTime ngayDat, NguoiDatLich nguoiDat, boolean thongTinKhacHoSo,
        String lyDoHuy, boolean duocHuyDoi, LocalDateTime hanHuyDoi, boolean canDoiLich,
        boolean daDoiBacSi) {
}
