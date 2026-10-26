package iuh.fit.se.eclinic.booking.service.impl;

import java.util.Locale;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.booking.config.CapLaiLienKetProperties;
import iuh.fit.se.eclinic.booking.service.GioiHanCapLaiLienKetService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GioiHanCapLaiLienKetServiceImpl implements GioiHanCapLaiLienKetService {

    private final StringRedisTemplate redisTemplate;
    private final CapLaiLienKetProperties capLaiLienKetProperties;

    @Override
    public void ghiNhan(String maTraCuu, String diaChiIp) {
        // IP vượt thì dừng luôn, không đếm thêm cho mã: 1 máy dò nhiều mã không làm khoá mã của người khác thêm nữa
        kiemTra("cap-lai:ip:" + diaChiIp, capLaiLienKetProperties.soLanToiDaMoiIp());
        kiemTra("cap-lai:ma:" + maTraCuu.trim().toUpperCase(Locale.ROOT), capLaiLienKetProperties.soLanToiDaMoiMa());
    }

    private void kiemTra(String khoa, int toiDa) {
        long conLaiGiay;
        try {
            if (dem(khoa) <= toiDa) {
                return;
            }
            Long ttl = redisTemplate.getExpire(khoa);
            conLaiGiay = ttl == null || ttl <= 0 ? capLaiLienKetProperties.cuaSo().toSeconds() : ttl;
        } catch (RuntimeException ex) {
            log.warn("Không đếm được số lần xin cấp lại liên kết trên Redis, bỏ qua giới hạn: {}", ex.getMessage());
            return;
        }
        // Làm tròn lên: còn 61 giây thì báo 2 phút
        long conLaiPhut = Math.max(1, (conLaiGiay + 59) / 60);
        throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH, "loi.GUI_LAI_QUA_NHANH.capLaiLienKet",
                String.valueOf(conLaiPhut));
    }

    /** Cửa sổ cố định: lần gọi đầu mở cửa sổ đếm, hết cửa sổ thì khoá tự xoá. */
    private long dem(String khoa) {
        Long soLan = redisTemplate.opsForValue().increment(khoa);
        long ketQua = soLan == null ? 0 : soLan;
        // TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại, tránh khoá đếm mãi không hết hạn
        if (ketQua == 1 || conThieuTtl(khoa)) {
            redisTemplate.expire(khoa, capLaiLienKetProperties.cuaSo());
        }
        return ketQua;
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

}
