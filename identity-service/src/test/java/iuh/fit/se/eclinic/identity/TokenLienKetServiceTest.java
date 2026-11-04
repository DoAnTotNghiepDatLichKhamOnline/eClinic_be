package iuh.fit.se.eclinic.identity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;

/** Token liên kết trên Redis thật. */
class TokenLienKetServiceTest extends KiemThuTichHop {

    private static final MucDichLienKet MUC_DICH = MucDichLienKet.XAC_THUC_EMAIL;
    private static final Duration THOI_HAN = Duration.ofMinutes(5);

    @Autowired TokenLienKetService tokenLienKetService;
    @Autowired StringRedisTemplate redisTemplate;

    @Test
    void tokenChiDungDuocMotLan() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        assertThat(tokenLienKetService.suDung(token, MUC_DICH)).isEqualTo(idTaiKhoan);
        assertLienKetKhongHopLe(token);
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
    void redisChiLuuBanBamKhongLuuTokenGoc() {
        long idTaiKhoan = idNgauNhien();
        String token = tokenLienKetService.tao(idTaiKhoan, MUC_DICH, THOI_HAN);

        Set<String> khoa = redisTemplate.keys("lien-ket:*");
        assertThat(khoa).isNotEmpty().noneMatch(k -> k.contains(token));
        assertThat(khoa).allSatisfy(k -> assertThat(redisTemplate.opsForValue().get(k)).isNotEqualTo(token));
        assertThat(redisTemplate.getExpire("lien-ket:tai-khoan:" + MUC_DICH.name() + ":" + idTaiKhoan))
                .isPositive();
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
