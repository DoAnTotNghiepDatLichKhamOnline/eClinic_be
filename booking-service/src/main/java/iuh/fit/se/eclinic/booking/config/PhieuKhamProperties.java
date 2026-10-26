package iuh.fit.se.eclinic.booking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Link và mã QR của phiếu khám, prefix {@code app.phieu-kham} trong application.yml. */
@ConfigurationProperties("app.phieu-kham")
public record PhieuKhamProperties(
        @DefaultValue("http://localhost:5173") String frontendUrl,
        @DefaultValue("/phieu-kham/") String duongDan,
        @DefaultValue("300") int kichThuocQr) {

    /** Link phiếu khám gửi cho người đặt; cũng là nội dung duy nhất của mã QR (không chứa thông tin bệnh nhân). */
    public String lienKet(String maPhieuKham) {
        return frontendUrl + duongDan + maPhieuKham;
    }

}
