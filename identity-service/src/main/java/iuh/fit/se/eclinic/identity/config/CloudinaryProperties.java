package iuh.fit.se.eclinic.identity.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Kho ảnh Cloudinary (gói miễn phí), prefix {@code app.cloudinary} trong application.yml.
 *
 * @param cloudName   CLOUDINARY_CLOUD_NAME
 * @param apiKey      CLOUDINARY_API_KEY
 * @param apiSecret   CLOUDINARY_API_SECRET, dùng để ký request, không bao giờ ghi ra log
 * @param thuMuc      thư mục gốc của ảnh trong kho; 2 DB dùng chung 1 tài khoản Cloudinary thì đặt khác nhau
 *                    để không ghi đè ảnh của nhau (ảnh đặt tên theo id tài khoản)
 * @param urlApi      địa chỉ API của Cloudinary (chỉ đổi khi thử với server giả)
 * @param thoiGianCho thời gian chờ tối đa khi kết nối / chờ Cloudinary trả lời, phải nhỏ hơn read-timeout của gateway
 */
@ConfigurationProperties("app.cloudinary")
public record CloudinaryProperties(
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
