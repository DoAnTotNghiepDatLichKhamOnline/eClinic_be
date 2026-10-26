package iuh.fit.se.eclinic.identity.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.function.Consumer;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.config.GoogleProperties;

/**
 * Kiểm tra ID token Google mà không gọi Google: token ký bằng khoá RSA tự tạo, decoder dùng khoá công khai tương ứng
 * cùng bộ kiểm tra {@link XacMinhTokenGoogle#boKiemTra(String)} như lúc chạy thật.
 */
class XacMinhTokenGoogleTest {

    private static final String CLIENT_ID = "eclinic-test.apps.googleusercontent.com";

    private static KeyPair khoa;
    private static KeyPair khoaKhac;
    private static XacMinhTokenGoogle xacMinh;

    @BeforeAll
    static void taoKhoa() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        khoa = generator.generateKeyPair();
        khoaKhac = generator.generateKeyPair();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey((RSAPublicKey) khoa.getPublic()).build();
        decoder.setJwtValidator(XacMinhTokenGoogle.boKiemTra(CLIENT_ID));
        xacMinh = new XacMinhTokenGoogle(decoder, true);
    }

    @Test
    void chapNhanCaHaiGiaTriIssVaTraThongTin() {
        for (String iss : XacMinhTokenGoogle.NHA_PHAT_HANH) {
            ThongTinGoogle thongTin = xacMinh.xacMinh(token(c -> c.issuer(iss)
                    .claim("email", " Nguoi.Dung@Gmail.com ")
                    .claim("hd", null)
                    .claim("name", "Người Dùng")
                    .claim("picture", "https://lh3.googleusercontent.com/a/anh")));

            assertThat(thongTin.sub()).isEqualTo("1234567890");
            assertThat(thongTin.email()).isEqualTo("nguoi.dung@gmail.com");
            assertThat(thongTin.hd()).isNull();
            assertThat(thongTin.ten()).isEqualTo("Người Dùng");
            assertThat(thongTin.anh()).isEqualTo("https://lh3.googleusercontent.com/a/anh");
        }
    }

    @Test
    void emailVerifiedDangChuoiTrueVanHopLe() {
        assertThat(xacMinh.xacMinh(token(c -> c.claim("email_verified", "true"))).email())
                .isEqualTo("user@gmail.com");
    }

    @Test
    void tokenSaiDieuKienBao401() {
        assertMaLoi(token(c -> c.issuer("https://evil.example.com")), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.audience("client-khac.apps.googleusercontent.com")), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.issueTime(Date.from(Instant.now().minus(Duration.ofHours(2))))
                .expirationTime(Date.from(Instant.now().minus(Duration.ofMinutes(5))))),
                MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.claim("email_verified", false)), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.claim("email_verified", null)), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.claim("email", null)), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi(token(c -> c.subject(null)), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
    }

    @Test
    void chuKySaiHoacChuoiRacBao401() {
        assertMaLoi(token((RSAPrivateKey) khoaKhac.getPrivate(), c -> { }), MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi("khong-phai-jwt", MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
        assertMaLoi("a.b.c", MaLoi.GOOGLE_TOKEN_KHONG_HOP_LE);
    }

    @Test
    void chuaCauHinhClientIdBao503() {
        XacMinhTokenGoogle chuaCauHinh = new XacMinhTokenGoogle(
                new GoogleProperties(" ", "https://www.googleapis.com/oauth2/v3/certs", Duration.ofSeconds(5)));

        assertThatThrownBy(() -> chuaCauHinh.xacMinh(token(c -> { })))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.DANG_NHAP_GOOGLE_KHONG_KHA_DUNG);
    }

    @Test
    void khongLayDuocKhoaCongKhaiCuaGoogleBao503() {
        XacMinhTokenGoogle khongKetNoi = new XacMinhTokenGoogle(
                new GoogleProperties(CLIENT_ID, "http://127.0.0.1:1/certs", Duration.ofSeconds(2)));

        assertThatThrownBy(() -> khongKetNoi.xacMinh(token(c -> { })))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.DANG_NHAP_GOOGLE_KHONG_KHA_DUNG);
    }

    @Test
    void googleChiDamBaoGmailVaWorkspace() {
        assertThat(new ThongTinGoogle("1", "a@gmail.com", null, null, null).laGoogleXacNhan()).isTrue();
        assertThat(new ThongTinGoogle("1", "a@congty.vn", "congty.vn", null, null).laGoogleXacNhan()).isTrue();
        assertThat(new ThongTinGoogle("1", "a@yahoo.com", null, null, null).laGoogleXacNhan()).isFalse();
        assertThat(new ThongTinGoogle("1", "a@yahoo.com", " ", null, null).laGoogleXacNhan()).isFalse();
        assertThat(new ThongTinGoogle("1", "a@gmail.com.evil.vn", null, null, null).laGoogleXacNhan()).isFalse();
    }

    private static void assertMaLoi(String idToken, MaLoi maLoi) {
        assertThatThrownBy(() -> xacMinh.xacMinh(idToken))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(maLoi);
    }

    private static String token(Consumer<JWTClaimsSet.Builder> sua) {
        return token((RSAPrivateKey) khoa.getPrivate(), sua);
    }

    /** ID token hợp lệ (như Google cấp), sửa bằng {@code sua} để tạo từng trường hợp sai. */
    private static String token(RSAPrivateKey khoaKy, Consumer<JWTClaimsSet.Builder> sua) {
        Instant bayGio = Instant.now();
        JWTClaimsSet.Builder claims = new JWTClaimsSet.Builder()
                .issuer("https://accounts.google.com")
                .audience(CLIENT_ID)
                .subject("1234567890")
                .issueTime(Date.from(bayGio))
                .expirationTime(Date.from(bayGio.plus(Duration.ofHours(1))))
                .claim("email", "user@gmail.com")
                .claim("email_verified", true);
        sua.accept(claims);
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("khoa-test").type(JOSEObjectType.JWT).build(),
                    claims.build());
            jwt.sign(new RSASSASigner(khoaKy));
            return jwt.serialize();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

}
