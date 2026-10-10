package iuh.fit.se.eclinic.booking.event;

import iuh.fit.se.eclinic.booking.dto.request.BenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.NguoiGiamHoRequest;

/**
 * Tài khoản vừa đặt lịch cho người thân (không phải "đặt cho bản thân") và không từ chối lưu. Phát trong transaction
 * đặt lịch, xử lý SAU khi transaction commit: lưu những gì tài khoản đã nhập để điền sẵn form lần sau.
 *
 * @param nguoiGiamHo người giám hộ của lượt khám; null nếu người khám từ đủ 18 tuổi
 */
public record DaDatLichChoNguoiThanEvent(Long idTaiKhoan, Long idHoSoBenhNhan, BenhNhanRequest benhNhan,
        NguoiGiamHoRequest nguoiGiamHo) {
}
