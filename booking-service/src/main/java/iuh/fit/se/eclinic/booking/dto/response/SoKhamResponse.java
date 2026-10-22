package iuh.fit.se.eclinic.booking.dto.response;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.enums.GioiTinh;

/**
 * Sổ khám bệnh đầy đủ, sau khi nhập đúng 4 số cuối SĐT liên hệ (EXAM-03). Không có id lịch hẹn, id hồ sơ; CCCD vẫn che.
 *
 * @param hoTenNguoiGiamHo null nếu lượt khám không có người giám hộ
 * @param ketQua           kết quả khám như bệnh nhân xem trong lịch sử khám
 */
public record SoKhamResponse(SoKhamTomTatResponse tomTat, BenhNhan benhNhan, String hoTenNguoiGiamHo,
        KetQuaKhamResponse ketQua) {

    /** Họ tên, ngày sinh, giới tính như người đặt đã nhập; CCCD của hồ sơ, chỉ giữ 3 số cuối. */
    public record BenhNhan(String hoTen, LocalDate ngaySinh, GioiTinh gioiTinh, String cccdChe) {
    }
}
