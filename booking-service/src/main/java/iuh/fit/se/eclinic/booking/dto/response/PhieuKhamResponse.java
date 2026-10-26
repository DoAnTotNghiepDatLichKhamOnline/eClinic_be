package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Phiếu khám xem bằng mã ngẫu nhiên, không cần đăng nhập. */
public record PhieuKhamResponse(String maPhieuKham, String maTraCuu, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String lyDoKham, LocalDateTime ngayDat,
        BenhNhanPhieuKhamResponse benhNhan, NguoiGiamHoPhieuKhamResponse nguoiGiamHo,
        boolean canNguoiGiamHoDiCung, String lyDoHuy, boolean duocHuyDoi, LocalDateTime hanHuyDoi,
        boolean canDoiLich, boolean daDoiBacSi) {
}
