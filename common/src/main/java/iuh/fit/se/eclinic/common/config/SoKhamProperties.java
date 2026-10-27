package iuh.fit.se.eclinic.common.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Sổ khám bệnh công khai, prefix {@code app.so-kham}. */
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
