package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.booking.dto.response.SoKhamResponse;
import iuh.fit.se.eclinic.booking.dto.response.SoKhamTomTatResponse;

/** Sổ khám bệnh công khai bằng mã ngẫu nhiên của hồ sơ bệnh án, tách khỏi mã phiếu khám. */
public interface SoKhamService {

    /** Phần công khai: họ tên đã che, năm sinh, ngày giờ khám, bác sĩ, chuyên khoa, phòng; không có kết quả khám. */
    SoKhamTomTatResponse tomTat(String maSoKham);

    /** Sổ khám đầy đủ khi {@code bonSoCuoi} khớp 4 số cuối SĐT liên hệ của lượt khám. */
    SoKhamResponse xem(String maSoKham, String bonSoCuoi);

    /** Ảnh PNG mã QR chứa link sổ khám; {@code kichThuoc} null thì dùng app.so-kham.kich-thuoc-qr. */
    byte[] taoQr(String maSoKham, Integer kichThuoc);

}
