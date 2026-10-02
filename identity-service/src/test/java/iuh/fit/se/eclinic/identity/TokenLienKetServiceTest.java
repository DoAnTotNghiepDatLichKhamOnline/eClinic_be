package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.mail.health.MailHealthIndicator;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.common.testsupport.MySqlTestcontainersConfiguration;
import iuh.fit.se.eclinic.common.testsupport.RedisTestcontainersConfiguration;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService.LienKetDaDung;

/**
 * Token liên kết trên Redis thật. Context load thành công cũng có nghĩa là Flyway chạy được V1__init_schema.sql
 * và Hibernate validate khớp mọi entity.
 */
@SpringBootTest
@Import({ MySqlTestcontainersConfiguration.class, RedisTestcontainersConfiguration.class })
class TokenLienKetServiceTest {

    private static final MucDichLienKet MUC_DICH = MucDichLienKet.XAC_THUC_EMAIL;
    private static final Duration THOI_HAN = Duration.ofMinutes(5);

    @Autowired TokenLienKetService tokenLienKetService;
    @Autowired StringRedisTemplate redisTemplate;
    @Autowired ObjectProvider<MailHealthIndicator> mailHealthIndicator;

    @Test
    void tokenChiDungDuocMotLan() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        assertThat(tokenLienKetService.suDung(token, MUC_DICH)).isEqualTo(idTaiKhoan);
        assertLienKetKhongHopLe(token);
    }

    @Test
    void phatTokenMoiThiTokenCuHetHieuLuc() {
        long idTaiKhoan = idNgauNhien();
        String tokenCu = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);
        String tokenMoi = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        assertLienKetKhongHopLe(tokenCu);
        assertThat(tokenLienKetService.suDung(tokenMoi, MUC_DICH)).isEqualTo(idTaiKhoan);
    }

    @Test
    void tokenMucDichNayKhongDungDuocChoMucDichKhac() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        assertThatThrownBy(() -> tokenLienKetService.suDung(token, MucDichLienKet.DAT_LAI_MAT_KHAU))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.LIEN_KET_KHONG_HOP_LE);
        // Thử sai mục đích không làm mất token của mục đích đúng
        assertThat(tokenLienKetService.suDung(token, MUC_DICH)).isEqualTo(idTaiKhoan);
    }

    @Test
    void tokenRacRongHoacQuaDaiBiTuChoi() {
        assertLienKetKhongHopLe("khong-phai-token-that");
        assertLienKetKhongHopLe(" ");
        assertLienKetKhongHopLe(null);
        assertLienKetKhongHopLe("a".repeat(101));
    }

    @Test
    void redisChiLuuBanBamKhongLuuTokenGoc() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        Set<String> khoa = redisTemplate.keys("lien-ket:*");
        assertThat(khoa).isNotEmpty().noneMatch(k -> k.contains(token));
        assertThat(khoa).allSatisfy(k -> assertThat(redisTemplate.opsForValue().get(k)).isNotEqualTo(token));
        assertThat(redisTemplate.getExpire("lien-ket:tai-khoan:" + MUC_DICH.name() + ":" + idTaiKhoan))
                .isPositive();
    }

    @Test
    void tokenKemDuLieuTraLaiDungDuLieuKeCaKhiCoDauHaiCham() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MucDichLienKet.DOI_EMAIL, THOI_HAN, "a:b@example.com");

        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).contains("a:b@example.com");
        LienKetDaDung daDung = tokenLienKetService.suDungKemDuLieu(token, MucDichLienKet.DOI_EMAIL);
        assertThat(daDung).isEqualTo(new LienKetDaDung(idTaiKhoan, "a:b@example.com"));
        // Đã dùng: không xem được, không dùng lại được
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).isEmpty();
        assertThatThrownBy(() -> tokenLienKetService.suDungKemDuLieu(token, MucDichLienKet.DOI_EMAIL))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.LIEN_KET_KHONG_HOP_LE);
    }

    @Test
    void suDungTrenTokenKemDuLieuVanTraVeIdTaiKhoan() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN, "moi@example.com");

        assertThat(tokenLienKetService.suDung(token, MUC_DICH)).isEqualTo(idTaiKhoan);
    }

    @Test
    void tokenKhongKemDuLieuThiDuLieuNull() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MUC_DICH)).isEmpty();
        assertThat(tokenLienKetService.suDungKemDuLieu(token, MUC_DICH)).isEqualTo(new LienKetDaDung(idTaiKhoan, null));
    }

    @Test
    void xemDuLieuRongKhiChuaPhatDaHuyHoacBiThayBangTokenMoi() {
        long idTaiKhoan = idNgauNhien();
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).isEmpty();

        String tokenCu = tokenLienKetService.tao(idTaiKhoan, MucDichLienKet.DOI_EMAIL, THOI_HAN, "cu@example.com");
        tokenLienKetService.tao(idTaiKhoan, MucDichLienKet.DOI_EMAIL, THOI_HAN, "moi@example.com");
        // Xem không tiêu token: xem 2 lần vẫn còn; mục đích khác không thấy
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).contains("moi@example.com");
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).contains("moi@example.com");
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MUC_DICH)).isEmpty();
        assertThatThrownBy(() -> tokenLienKetService.suDungKemDuLieu(tokenCu, MucDichLienKet.DOI_EMAIL))
                .isInstanceOf(LoiNghiepVu.class);

        tokenLienKetService.huy(idTaiKhoan, MucDichLienKet.DOI_EMAIL);
        assertThat(tokenLienKetService.xemDuLieu(idTaiKhoan, MucDichLienKet.DOI_EMAIL)).isEmpty();
    }

    /** Health check không được đăng nhập Gmail (docker healthcheck gọi /actuator/health liên tục). */
    @Test
    void khongCoMailHealthIndicator() {
        assertThat(mailHealthIndicator.getIfAvailable()).isNull();
    }

    @Test
    void giuChoChanLanThuHai() {
        long idTaiKhoan = idNgauNhien();

        assertThat(tokenLienKetService.giuCho(idTaiKhoan, MUC_DICH, Duration.ofMinutes(1))).isTrue();
        assertThat(tokenLienKetService.giuCho(idTaiKhoan, MUC_DICH, Duration.ofMinutes(1))).isFalse();
    }

    private void assertLienKetKhongHopLe(String token) {
        assertThatThrownBy(() -> tokenLienKetService.suDung(token, MUC_DICH))
                .isInstanceOf(LoiNghiepVu.class)
                .extracting(e -> ((LoiNghiepVu) e).getMaLoi())
                .isEqualTo(MaLoi.LIEN_KET_KHONG_HOP_LE);
    }

    private static long idNgauNhien() {
        return System.nanoTime();
    }

}
