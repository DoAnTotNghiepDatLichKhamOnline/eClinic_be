package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.PhieuKhamResponse;

/** Phiếu khám xem bằng mã ngẫu nhiên, không cần đăng nhập. */
public interface PhieuKhamService {

    PhieuKhamResponse xem(String maPhieuKham);

    /** Ảnh PNG mã QR chứa link phiếu khám. */
    byte[] taoQr(String maPhieuKham, Integer kichThuoc);

    /** Tệp lịch .ics của lịch hẹn theo trạng thái hiện tại, chữ theo ngôn ngữ của request. */
    TepIcs taoTepLich(String maPhieuKham);

    /** @param tenTep {@code lich-kham-<mã tra cứu>.ics} */
    record TepIcs(String noiDung, String tenTep) {
    }

}
