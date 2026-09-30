package iuh.fit.se.eclinic.identity.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.enums.MucDichLienKet;
import iuh.fit.se.eclinic.identity.service.TokenLienKetService;
import iuh.fit.se.eclinic.identity.util.TokenNgauNhien;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TokenLienKetServiceImpl implements TokenLienKetService {

    /** Token hợp lệ dài 43 ký tự; chặn chuỗi quá dài trước khi băm. */
    private static final int DO_DAI_TOI_DA = 100;

    private final StringRedisTemplate redisTemplate;

    @Override
    public String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan) {
        huy(idTaiKhoan, mucDich);
        String token = TokenNgauNhien.tao();
        String bam = TokenNgauNhien.bam(token);
        redisTemplate.opsForValue().set(khoaToken(mucDich, bam), idTaiKhoan.toString(), thoiHan);
        redisTemplate.opsForValue().set(khoaTaiKhoan(mucDich, idTaiKhoan), bam, thoiHan);
        return token;
    }

    @Override
    public Long suDung(String token, MucDichLienKet mucDich) {
        if (token == null || token.isBlank() || token.length() > DO_DAI_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
        }
        // GETDEL nguyên tử. Không dọn khoá con trỏ: con trỏ cũ vô hại (lần tao sau xoá 1 khoá không còn),
        // còn GET-so sánh-DEL sẽ tranh chấp với 1 lần tao đồng thời.
        String idTaiKhoan = redisTemplate.opsForValue().getAndDelete(khoaToken(mucDich, TokenNgauNhien.bam(token)));
        if (idTaiKhoan == null) {
            throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
        }
        return Long.valueOf(idTaiKhoan);
    }

    @Override
    public boolean giuCho(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiGianCho) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(khoaCho(mucDich, idTaiKhoan), "1", thoiGianCho));
    }

    @Override
    public void huy(Long idTaiKhoan, MucDichLienKet mucDich) {
        String bamCu = redisTemplate.opsForValue().getAndDelete(khoaTaiKhoan(mucDich, idTaiKhoan));
        if (bamCu != null) {
            redisTemplate.delete(khoaToken(mucDich, bamCu));
        }
    }

    private static String khoaToken(MucDichLienKet mucDich, String bam) {
        return "lien-ket:" + mucDich.name() + ":" + bam;
    }

    private static String khoaTaiKhoan(MucDichLienKet mucDich, Long idTaiKhoan) {
        return "lien-ket:tai-khoan:" + mucDich.name() + ":" + idTaiKhoan;
    }

    private static String khoaCho(MucDichLienKet mucDich, Long idTaiKhoan) {
        return "lien-ket:cho:" + mucDich.name() + ":" + idTaiKhoan;
    }

}
