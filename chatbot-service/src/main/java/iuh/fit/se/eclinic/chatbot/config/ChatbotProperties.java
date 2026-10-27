package iuh.fit.se.eclinic.chatbot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Cấu hình chatbot, prefix {@code app.chatbot} trong application.yml. */
@ConfigurationProperties("app.chatbot")
public record ChatbotProperties(String urlGoc, String khoaApi, String moHinh, String mucSuyLuan, Duration thoiGianCho,
        Duration tongThoiGianCho, int soLanGoiMoHinhToiDa, String catalogUrl, String bookingUrl,
        boolean traCuuCaNhan, int soTokenTraLoiToiDa, int soTinNhanLichSu, int soTinNhanMoiCuaSo, Duration cuaSo, int soTinNhanMoiNgay,
        int soPhienMoiGio, int soTinNhanToanHeThongMoiNgay) {

    public boolean daCauHinh() {
        return khoaApi != null && !khoaApi.isBlank();
    }

}
