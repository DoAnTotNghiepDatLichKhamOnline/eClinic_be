package iuh.fit.se.eclinic.booking.event;

import iuh.fit.se.eclinic.booking.dto.request.BenhNhanRequest;
import iuh.fit.se.eclinic.booking.dto.request.NguoiGiamHoRequest;

/** Tài khoản vừa đặt lịch cho người thân (không phải "đặt cho bản thân") và không từ chối lưu. */
public record DaDatLichChoNguoiThanEvent(Long idTaiKhoan, Long idHoSoBenhNhan, BenhNhanRequest benhNhan,
        NguoiGiamHoRequest nguoiGiamHo) {
}
