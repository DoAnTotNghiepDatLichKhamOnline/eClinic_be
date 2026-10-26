package iuh.fit.se.eclinic.booking.dto.response;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/** Bệnh nhân trên phiếu khám công khai: chỉ năm sinh, CCCD và SĐT đã che. */
public record BenhNhanPhieuKhamResponse(String hoTen, Integer namSinh, GioiTinh gioiTinh, String cccd,
        String soDienThoai) {
}
