package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.identity.config.DangNhapProperties;
import iuh.fit.se.eclinic.identity.service.GioiHanDangNhapService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GioiHanDangNhapServiceImpl implements GioiHanDangNhapService {

    private final StringRedisTemplate redisTemplate;
    private final DangNhapProperties dangNhapProperties;

    @Override
    public boolean dangBiKhoa(String dinhDanh) {
        String soLan = redisTemplate.opsForValue().get(khoa(dinhDanh));
        return soLan != null && Long.parseLong(soLan) >= dangNhapProperties.soLanSaiToiDa();
    }

    @Override
    public long ghiNhanThatBai(String dinhDanh) {
        String khoa = khoa(dinhDanh);
        Long soLan = redisTemplate.opsForValue().increment(khoa);
        long ketQua = soLan == null ? 0 : soLan;
        // Lần sai đầu: mở cửa sổ đếm. Từ lần sai thứ N: khoá tính lại từ lúc này (dùng >= vì 2 request đồng thời
        // có thể nhảy qua đúng N). TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại.
        if (ketQua == 1 || ketQua >= dangNhapProperties.soLanSaiToiDa() || conThieuTtl(khoa)) {
            redisTemplate.expire(khoa, dangNhapProperties.thoiGianKhoa());
        }
        return ketQua;
    }

    @Override
    public void xoa(String dinhDanh) {
        redisTemplate.delete(khoa(dinhDanh));
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

    private static String khoa(String dinhDanh) {
        return "dang-nhap:sai:" + dinhDanh;
    }

}
