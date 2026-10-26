package iuh.fit.se.eclinic.booking.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.booking.config.DatLichProperties;
import iuh.fit.se.eclinic.booking.service.GioiHanDatLichService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GioiHanDatLichServiceImpl implements GioiHanDatLichService {

    private final StringRedisTemplate redisTemplate;
    private final DatLichProperties datLichProperties;

    @Override
    public void ghiNhan(String diaChiIp) {
        long soLan;
        try {
            soLan = dem("dat-lich:ip:" + diaChiIp);
        } catch (RuntimeException ex) {
            log.warn("Không đếm được số lần đặt lịch trên Redis, bỏ qua giới hạn theo IP: {}", ex.getMessage());
            return;
        }
        if (soLan > datLichProperties.soLanDatToiDaMoiIp()) {
            throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH);
        }
    }

    /** Cửa sổ cố định: lần gọi đầu mở cửa sổ đếm, hết cửa sổ thì khoá tự xoá. */
    private long dem(String khoa) {
        Long soLan = redisTemplate.opsForValue().increment(khoa);
        long ketQua = soLan == null ? 0 : soLan;
        // TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại, tránh khoá đếm mãi không hết hạn
        if (ketQua == 1 || conThieuTtl(khoa)) {
            redisTemplate.expire(khoa, datLichProperties.cuaSoGioiHanIp());
        }
        return ketQua;
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

}
