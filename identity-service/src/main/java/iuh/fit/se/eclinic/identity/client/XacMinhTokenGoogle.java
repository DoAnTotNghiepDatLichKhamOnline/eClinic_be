package iuh.fit.se.eclinic.identity.client;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.GoogleProperties;
import iuh.fit.se.eclinic.identity.util.ChuanHoa;
import lombok.extern.slf4j.Slf4j;

/**
 * Kiểm tra ID token do Google Identity Services cấp cho frontend: chữ ký (khoá công khai của Google), iss, aud
 * (= Client ID của mình), hạn dùng, email_verified.
 * <p>
 * Decoder là field riêng, KHÔNG phải bean {@link JwtDecoder}: bean đó (JwtConfig) kiểm tra access token của hệ thống.
 */
@Slf4j
@Component
public class XacMinhTokenGoogle {

    /** Google phát hành ID token với 1 trong 2 giá trị iss này. */
    static final Set<String> NHA_PHAT_HANH = Set.of("accounts.google.com", "https://accounts.google.com");

    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_EMAIL_VERIFIED = "email_verified";

    private final JwtDecoder decoder;
    private final boolean daCauHinh;

    @Autowired
    public XacMinhTokenGoogle(GoogleProperties googleProperties) {
        this(taoDecoder(googleProperties), googleProperties.daCauHinh());
        if (!daCauHinh) {
            log.warn("Chưa đặt GOOGLE_CLIENT_ID: đăng nhập Google (POST /api/auth/google) sẽ trả 503.");
        }
    }

    /** Cho test: decoder dùng khoá tự tạo thay vì khoá của Google. */
    XacMinhTokenGoogle(JwtDecoder decoder, boolean daCauHinh) {
        this.decoder = decoder;
        this.daCauHinh = daCauHinh;
    }

    /**
     * @throws LoiNghiepVu GOOGLE_TOKEN_KHONG_HOP_LE (401) khi token sai / hết hạn / không dành cho ứng dụng này;
     *                     DANG_NHAP_GOOGLE_KHONG_KHA_DUNG (503) khi chưa cấu hình hoặc không lấy được khoá của Google
     */
    public ThongTinGoogle xacMinh(String idToken) {
        if (!daCauHinh) {
            throw new LoiNghiepVu(MaLoi.DANG_NHAP_GOOGLE_KHONG_KHA_DUNG);
        }
        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (BadJwtException ex) {
            // Chữ ký sai, hết hạn, sai iss/aud, email chưa xác minh (thông điệp không chứa token)
            log.info("Từ chối ID token Google: {}", ex.getMessage());
            throw new LoiNghiepVu(MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        } catch (JwtException ex) {
            // Không phải lỗi của token: thường là không lấy được khoá công khai của Google
            log.warn("Không kiểm tra được ID token Google: {}", ex.getMessage());
            throw new LoiNghiepVu(MaLoi.DANG_NHAP_GOOGLE_KHONG_KHA_DUNG);
        }
        return new ThongTinGoogle(jwt.getSubject(), ChuanHoa.email(jwt.getClaimAsString(CLAIM_EMAIL)),
                jwt.getClaimAsString("hd"), jwt.getClaimAsString("name"), jwt.getClaimAsString("picture"));
    }

    /**
     * Các điều kiện ID token phải thỏa (ngoài chữ ký). Tự ghép thay vì JwtValidators.createDefaultWithIssuer
     * vì Google dùng 2 giá trị iss.
     */
    static OAuth2TokenValidator<Jwt> boKiemTra(String clientId) {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtClaimValidator<Object>(JwtClaimNames.ISS,
                        iss -> iss != null && NHA_PHAT_HANH.contains(iss.toString())),
                new JwtClaimValidator<List<String>>(JwtClaimNames.AUD, aud -> aud != null && aud.contains(clientId)),
                // Google gửi boolean; token cũ có thể là chuỗi "true"
                new JwtClaimValidator<Object>(CLAIM_EMAIL_VERIFIED, v -> v != null && "true".equals(v.toString())),
                new JwtClaimValidator<Object>(JwtClaimNames.SUB, XacMinhTokenGoogle::khongRong),
                new JwtClaimValidator<Object>(CLAIM_EMAIL, XacMinhTokenGoogle::khongRong));
    }

    private static boolean khongRong(Object giaTri) {
        return giaTri != null && !giaTri.toString().isBlank();
    }

    private static JwtDecoder taoDecoder(GoogleProperties googleProperties) {
        if (!googleProperties.daCauHinh()) {
            return null;
        }
        // RestTemplate mặc định chờ tới 30 giây: quá lâu cho 1 request đăng nhập
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(googleProperties.thoiGianCho());
        factory.setReadTimeout(googleProperties.thoiGianCho());
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(googleProperties.jwkSetUri())
                .restOperations(new RestTemplate(factory))
                .build();
        decoder.setJwtValidator(boKiemTra(googleProperties.clientId()));
        return decoder;
    }

}
