package iuh.fit.se.eclinic.booking.dto.response;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * Bệnh nhân trên phiếu khám công khai: chỉ năm sinh, CCCD và SĐT đã che.
 *
 * @param cccd        đã che, chỉ còn 3 số cuối
 * @param soDienThoai SĐT liên hệ của lượt khám, đã che; null khi lượt khám có người giám hộ (SĐT liên hệ là của người
 *                    giám hộ) hoặc lịch hẹn không lưu SĐT liên hệ
 */
public record BenhNhanPhieuKhamResponse(String hoTen, Integer namSinh, GioiTinh gioiTinh, String cccd,
        String soDienThoai) {
}
