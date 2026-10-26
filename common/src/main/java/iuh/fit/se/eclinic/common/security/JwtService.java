package iuh.fit.se.eclinic.common.security;

import java.time.Instant;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.common.enums.VaiTro;
import lombok.RequiredArgsConstructor;

/**
 * Phát hành access token. Chỉ identity-service gọi khi đăng nhập (và test).
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final BaoMatProperties baoMatProperties;

    /** Token không gắn với phiên đăng nhập nào (không có claim "phien"). */
    public String taoAccessToken(Long idTaiKhoan, String email, VaiTro vaiTro) {
        return taoAccessToken(idTaiKhoan, email, vaiTro, null);
    }

    /** @param maPhien mã phiên đăng nhập, đưa vào claim "phien"; null thì token không có claim này */
    public String taoAccessToken(Long idTaiKhoan, String email, VaiTro vaiTro, String maPhien) {
        Instant bayGio = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer(JwtConfig.NHA_PHAT_HANH)
                .subject(String.valueOf(idTaiKhoan))
                .issuedAt(bayGio)
                .expiresAt(bayGio.plus(baoMatProperties.thoiHanAccessToken()))
                .claim(JwtConfig.CLAIM_EMAIL, email)
                .claim(JwtConfig.CLAIM_VAI_TRO, vaiTro.name());
        // claim() không nhận giá trị null
        if (maPhien != null) {
            claims.claim(JwtConfig.CLAIM_PHIEN, maPhien);
        }
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims.build())).getTokenValue();
    }

}
