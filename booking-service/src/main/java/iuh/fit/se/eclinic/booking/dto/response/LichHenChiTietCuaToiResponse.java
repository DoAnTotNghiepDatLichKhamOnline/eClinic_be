package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * Chi tiết 1 lịch hẹn trong "lịch hẹn của tôi", mở bằng mã phiếu khám.
 *
 * @param lichHen           các trường của dòng trong danh sách
 * @param ngaySinhBenhNhan  {@code lichHen.laBanThan}: ngày sinh trong hồ sơ của chủ tài khoản; còn lại: ngày sinh người
 *                          đặt đã nhập
 * @param soDienThoaiLienHe SĐT liên hệ của lượt khám; null nếu lịch không do tài khoản đang xem đặt
 * @param emailLienHe       email liên hệ của lượt khám; null nếu không nhập hoặc lịch không do tài khoản đang xem đặt
 * @param ketQua            kết quả khám; null nếu chưa khám xong, hoặc tài khoản không được xem kết quả (chỉ được xem
 *                          khi {@code lichHen.laBanThan} hoặc {@code lichHen.nguoiDat} là TOI)
 */
public record LichHenChiTietCuaToiResponse(
        LichHenCuaToiResponse lichHen,
        LocalDate ngaySinhBenhNhan,
        GioiTinh gioiTinhBenhNhan,
        String soDienThoaiLienHe,
        String emailLienHe,
        KetQuaKhamResponse ketQua) {
}
