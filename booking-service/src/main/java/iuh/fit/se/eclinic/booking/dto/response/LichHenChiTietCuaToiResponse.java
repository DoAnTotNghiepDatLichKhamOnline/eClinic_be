package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/** Chi tiết 1 lịch hẹn trong "lịch hẹn của tôi", mở bằng mã phiếu khám. */
public record LichHenChiTietCuaToiResponse(
        LichHenCuaToiResponse lichHen,
        LocalDate ngaySinhBenhNhan,
        GioiTinh gioiTinhBenhNhan,
        String soDienThoaiLienHe,
        String emailLienHe,
        KetQuaKhamResponse ketQua,
        boolean duocDanhGia,
        DanhGiaCuaToiResponse danhGia) {
}
