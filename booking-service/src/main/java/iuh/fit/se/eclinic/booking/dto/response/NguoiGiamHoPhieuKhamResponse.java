package iuh.fit.se.eclinic.booking.dto.response;

import iuh.fit.se.eclinic.common.enums.QuanHeGiamHo;

/**
 * Người giám hộ trên phiếu khám công khai (BOOK-05): họ tên, quan hệ, SĐT đã che; không có CCCD.
 */
public record NguoiGiamHoPhieuKhamResponse(String hoTen, QuanHeGiamHo quanHe, String soDienThoai) {
}
