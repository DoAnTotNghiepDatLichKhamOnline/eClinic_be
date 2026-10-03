package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import iuh.fit.se.eclinic.common.enums.TrangThaiLichHen;

/**
 * 1 dòng trong "lịch hẹn của tôi" (BOOK-07). Không có id lịch hẹn: xem chi tiết / QR bằng phiếu khám theo
 * {@code maPhieuKham} (quy tắc #8).
 *
 * @param laBanThan        true nếu người khám là chủ tài khoản (lịch dùng hồ sơ bệnh nhân của tài khoản); false là lịch
 *                         đặt cho người thân
 * @param hoTenNguoiGiamHo null nếu lượt khám không có người giám hộ
 * @param ngayDat          thời điểm đặt lịch
 */
public record LichHenCuaToiResponse(String maPhieuKham, String linkPhieuKham, TrangThaiLichHen trangThai,
        Integer soThuTu, LocalDate ngay, LocalDateTime gioKhamDuKien, LocalDateTime gioBatDauKhung,
        LocalDateTime gioKetThucKhung, BacSiTomTatResponse bacSi, String tenChuyenKhoa,
        PhongKhamTomTatResponse phongKham, String hoTenBenhNhan, boolean laBanThan, String hoTenNguoiGiamHo,
        String lyDoKham, LocalDateTime ngayDat) {
}
