package iuh.fit.se.eclinic.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Sổ khám bệnh (EXAM-03, EXAM-04), prefix {@code app.so-kham} trong application-common.yml: booking-service mở trang công
 * khai và trả link cho bệnh nhân, medical-service trả link cho bác sĩ vừa ghi kết quả. Service dùng phải có
 * {@code @ConfigurationPropertiesScan}.
 *
 * @param frontendUrl   địa chỉ frontend, link sổ khám trỏ về đây
 * @param duongDan      trang của frontend hiển thị sổ khám, mã sổ khám nối ngay sau
 * @param kichThuocQr   cạnh ảnh QR (pixel) khi request không gửi kích thước
 * @param soLanSaiToiDa số lần nhập sai 4 số cuối SĐT tối đa của 1 sổ khám trong {@code khoaSau}
 * @param khoaSau       cửa sổ đếm số lần sai; đủ {@code soLanSaiToiDa} lần thì khoá xem tới hết cửa sổ
 */
@ConfigurationProperties("app.so-kham")
public record SoKhamProperties(
        @DefaultValue("http://localhost:5173") String frontendUrl,
        @DefaultValue("/so-kham/") String duongDan,
        @DefaultValue("300") int kichThuocQr,
        @DefaultValue("5") int soLanSaiToiDa,
        @DefaultValue("15m") Duration khoaSau) {

    /** Link sổ khám; cũng là nội dung duy nhất của mã QR (không chứa thông tin bệnh nhân). */
    public String lienKet(String maSoKham) {
        return frontendUrl + duongDan + maSoKham;
    }

}
