package iuh.fit.se.eclinic.common.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.extern.slf4j.Slf4j;

/** Khoá của các API nội bộ {@code /noi-bo/**} (service gọi service, không qua gateway). */
@Slf4j
@Component
@EnableConfigurationProperties(NoiBoProperties.class)
public class KhoaNoiBo {

    public static final String TEN_HEADER = "X-Khoa-Noi-Bo";

    private static final int DO_DAI_TOI_THIEU = 32;

    /** PHẢI trùng giá trị mặc định của app.noi-bo.khoa trong application-common.yml. */
    static final String KHOA_DEV_MAC_DINH = "eclinic-dev-khoa-noi-bo-chi-dung-o-may-local-khong-dung-that";

    private final byte[] khoa;
    private final String giaTri;

    public KhoaNoiBo(NoiBoProperties properties) {
        String khoa = properties.khoa();
        if (khoa == null || khoa.getBytes(StandardCharsets.UTF_8).length < DO_DAI_TOI_THIEU) {
            throw new IllegalStateException("app.noi-bo.khoa (INTERNAL_API_KEY) phải dài tối thiểu "
                    + DO_DAI_TOI_THIEU + " byte");
        }
        if (KHOA_DEV_MAC_DINH.equals(khoa)) {
            log.warn("INTERNAL_API_KEY đang là khoá mặc định cho máy dev. Đặt INTERNAL_API_KEY khi triển khai thật.");
        }
        this.giaTri = khoa;
        this.khoa = khoa.getBytes(StandardCharsets.UTF_8);
    }

    /** Giá trị để bên gọi đặt vào header. */
    public String giaTri() {
        return giaTri;
    }

    /** Thiếu hoặc sai khoá: 401. So sánh không phụ thuộc vị trí ký tự sai. */
    public void kiemTra(String khoaDaGui) {
        byte[] daGui = khoaDaGui == null ? new byte[0] : khoaDaGui.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(khoa, daGui)) {
            throw new LoiNghiepVu(MaLoi.CHUA_DANG_NHAP, "loi.CHUA_DANG_NHAP.khoaNoiBo");
        }
    }

}
