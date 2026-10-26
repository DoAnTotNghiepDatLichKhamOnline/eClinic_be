package iuh.fit.se.eclinic.common.luutru;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Kho ảnh Cloudinary (gói miễn phí), prefix {@code app.cloudinary} trong application-common.yml. */
@ConfigurationProperties("app.cloudinary")
public record CloudinaryProperties(
        @DefaultValue("false") boolean bat,
        @DefaultValue("") String cloudName,
        @DefaultValue("") String apiKey,
        @DefaultValue("") String apiSecret,
        @DefaultValue("eclinic") String thuMuc,
        @DefaultValue("https://api.cloudinary.com") String urlApi,
        @DefaultValue("20s") Duration thoiGianCho) {

    /** Thiếu 1 trong 3 giá trị thì coi như chưa cấu hình: tải ảnh lên trả 503. */
    public boolean daCauHinh() {
        return coGiaTri(cloudName) && coGiaTri(apiKey) && coGiaTri(apiSecret);
    }

    private static boolean coGiaTri(String giaTri) {
        return giaTri != null && !giaTri.isBlank();
    }

    @Override
    public String toString() {
        return "CloudinaryProperties[cloudName=" + cloudName + ", thuMuc=" + thuMuc + ", urlApi=" + urlApi
                + ", apiSecret=***]";
    }

}
