package iuh.fit.se.eclinic.identity.service.impl;

import java.time.Duration;
import java.util.Optional;

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
    /** Ngăn id tài khoản với dữ liệu kèm trong giá trị của khoá token. */
    private static final char NGAN_CACH = ':';

    private final StringRedisTemplate redisTemplate;

    @Override
    public String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan) {
        return tao(idTaiKhoan, mucDich, thoiHan, null);
    }

    @Override
    public String tao(Long idTaiKhoan, MucDichLienKet mucDich, Duration thoiHan, String duLieu) {
        huy(idTaiKhoan, mucDich);
        String token = TokenNgauNhien.tao();
        String bam = TokenNgauNhien.bam(token);
        String giaTri = duLieu == null ? idTaiKhoan.toString() : idTaiKhoan.toString() + NGAN_CACH + duLieu;
        redisTemplate.opsForValue().set(khoaToken(mucDich, bam), giaTri, thoiHan);
        redisTemplate.opsForValue().set(khoaTaiKhoan(mucDich, idTaiKhoan), bam, thoiHan);
        return token;
    }

    @Override
    public Long suDung(String token, MucDichLienKet mucDich) {
        return suDungKemDuLieu(token, mucDich).idTaiKhoan();
    }

    @Override
    public LienKetDaDung suDungKemDuLieu(String token, MucDichLienKet mucDich) {
        if (token == null || token.isBlank() || token.length() > DO_DAI_TOI_DA) {
            throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
        }
        // GETDEL nguyên tử. Không dọn khoá con trỏ: con trỏ cũ vô hại (lần tao sau xoá 1 khoá không còn),
        // còn GET-so sánh-DEL sẽ tranh chấp với 1 lần tao đồng thời.
        String giaTri = redisTemplate.opsForValue().getAndDelete(khoaToken(mucDich, TokenNgauNhien.bam(token)));
        if (giaTri == null) {
            throw new LoiNghiepVu(MaLoi.LIEN_KET_KHONG_HOP_LE);
        }
        return tach(giaTri);
    }

    @Override
    public Optional<String> xemDuLieu(Long idTaiKhoan, MucDichLienKet mucDich) {
        String bam = redisTemplate.opsForValue().get(khoaTaiKhoan(mucDich, idTaiKhoan));
        if (bam == null) {
            return Optional.empty();
        }
        // Con trỏ có thể còn sau khi token đã dùng (xem suDungKemDuLieu): khi đó khoá token không còn
        String giaTri = redisTemplate.opsForValue().get(khoaToken(mucDich, bam));
        return giaTri == null ? Optional.empty() : Optional.ofNullable(tach(giaTri).duLieu());
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

    /** Giá trị của khoá token: {@code <id>} hoặc {@code <id>:<dữ liệu>}; chỉ cắt ở dấu ngăn cách đầu tiên. */
    private static LienKetDaDung tach(String giaTri) {
        int viTri = giaTri.indexOf(NGAN_CACH);
        if (viTri < 0) {
            return new LienKetDaDung(Long.valueOf(giaTri), null);
        }
        return new LienKetDaDung(Long.valueOf(giaTri.substring(0, viTri)), giaTri.substring(viTri + 1));
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
