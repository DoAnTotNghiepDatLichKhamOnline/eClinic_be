package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/** Kết quả đặt lịch. */
public record DatLichResponse(String maPhieuKham, String maTraCuu, String linkPhieuKham, Integer soThuTu, LocalDate ngay,
        LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung, LocalDateTime gioKetThucKhung,
        TrangThaiLichHen trangThai, BacSiTomTatResponse bacSi, PhongKhamTomTatResponse phongKham,
        String hoTenBenhNhan, String hoTenNguoiGiamHo, boolean luuVaoTaiKhoan, boolean bacSiDoPhongKhamXep) {
}
