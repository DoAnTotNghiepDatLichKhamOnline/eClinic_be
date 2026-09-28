package iuh.fit.se.eclinic.common.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * JWT ký bằng HS256 với khoá chung JWT_SECRET: identity-service phát hành, mọi service tự kiểm tra.
 */
@Configuration
public class JwtConfig {

    public static final String NHA_PHAT_HANH = "eclinic";
    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_VAI_TRO = "vaiTro";

    private static final int DO_DAI_KHOA_TOI_THIEU = 32;

    @Bean
    SecretKey khoaKyJwt(BaoMatProperties baoMatProperties) {
        String secret = baoMatProperties.jwtSecret();
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < DO_DAI_KHOA_TOI_THIEU) {
            throw new IllegalStateException("app.bao-mat.jwt-secret (JWT_SECRET) phải dài tối thiểu "
                    + DO_DAI_KHOA_TOI_THIEU + " byte");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey khoaKyJwt) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(khoaKyJwt).macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(NHA_PHAT_HANH));
        return decoder;
    }

    /** Lưu ý: mặc định encoder ký RS256, nên khi encode phải truyền header HS256 (xem JwtService). */
    @Bean
    JwtEncoder jwtEncoder(SecretKey khoaKyJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(khoaKyJwt));
    }

    /** Claim "vaiTro" -> quyền ROLE_<vaiTro>, dùng với @PreAuthorize("hasRole('QUAN_TRI_VIEN')"). */
    @Bean
    JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter quyen = new JwtGrantedAuthoritiesConverter();
        quyen.setAuthoritiesClaimName(CLAIM_VAI_TRO);
        quyen.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(quyen);
        return converter;
    }

}
