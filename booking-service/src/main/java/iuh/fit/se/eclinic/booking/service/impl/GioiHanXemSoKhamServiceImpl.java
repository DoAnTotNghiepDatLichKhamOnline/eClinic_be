package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.booking.service.GioiHanXemSoKhamService;
import iuh.fit.se.eclinic.common.config.SoKhamProperties;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GioiHanXemSoKhamServiceImpl implements GioiHanXemSoKhamService {

    private final StringRedisTemplate redisTemplate;
    private final SoKhamProperties soKhamProperties;

    @Override
    public void kiemTraKhoa(Long idHoSoBenhAn) {
        String khoa = khoa(idHoSoBenhAn);
        long soLanSai;
        long conLaiGiay;
        try {
            String giaTri = redisTemplate.opsForValue().get(khoa);
            soLanSai = giaTri == null ? 0 : Long.parseLong(giaTri);
            if (soLanSai < soKhamProperties.soLanSaiToiDa()) {
                return;
            }
            Long ttl = redisTemplate.getExpire(khoa);
            conLaiGiay = ttl == null || ttl <= 0 ? soKhamProperties.khoaSau().toSeconds() : ttl;
        } catch (RuntimeException ex) {
            log.warn("Không đọc được số lần nhập sai sổ khám trên Redis, bỏ qua giới hạn: {}", ex.getMessage());
            return;
        }
        // Làm tròn lên: còn 61 giây thì báo 2 phút
        long conLaiPhut = Math.max(1, (conLaiGiay + 59) / 60);
        throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH, "loi.GUI_LAI_QUA_NHANH.soKham", String.valueOf(conLaiPhut));
    }

    @Override
    public void ghiNhanSai(Long idHoSoBenhAn) {
        String khoa = khoa(idHoSoBenhAn);
        try {
            Long soLan = redisTemplate.opsForValue().increment(khoa);
            // TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại, tránh khoá mãi không hết hạn
            if (Long.valueOf(1).equals(soLan) || conThieuTtl(khoa)) {
                redisTemplate.expire(khoa, soKhamProperties.khoaSau());
            }
        } catch (RuntimeException ex) {
            log.warn("Không đếm được lần nhập sai sổ khám trên Redis, bỏ qua giới hạn: {}", ex.getMessage());
        }
    }

    @Override
    public void xoa(Long idHoSoBenhAn) {
        try {
            redisTemplate.delete(khoa(idHoSoBenhAn));
        } catch (RuntimeException ex) {
            log.warn("Không xoá được số lần nhập sai sổ khám trên Redis: {}", ex.getMessage());
        }
    }

    private static String khoa(Long idHoSoBenhAn) {
        return "so-kham:sai:" + idHoSoBenhAn;
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

}
