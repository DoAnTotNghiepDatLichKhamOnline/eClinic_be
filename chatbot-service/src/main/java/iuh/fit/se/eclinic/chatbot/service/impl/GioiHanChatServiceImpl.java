package iuh.fit.se.eclinic.chatbot.service.impl;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import iuh.fit.se.eclinic.chatbot.config.ChatbotProperties;
import iuh.fit.se.eclinic.chatbot.service.GioiHanChatService;
import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GioiHanChatServiceImpl implements GioiHanChatService {

    private static final Duration MOT_GIO = Duration.ofHours(1);
    private static final Duration MOT_NGAY = Duration.ofDays(1);

    private final StringRedisTemplate redisTemplate;
    private final ChatbotProperties properties;

    @Override
    public void ghiNhanMoPhien(String chuThe) {
        kiemTra("chat:phien:" + chuThe, properties.soPhienMoiGio(), MOT_GIO, "loi.CHAT_QUA_NHIEU.moPhien");
    }

    @Override
    public void ghiNhanTinNhan(String chuThe) {
        kiemTra("chat:tin-nhan:" + chuThe, properties.soTinNhanMoiCuaSo(), properties.cuaSo(), "loi.CHAT_QUA_NHIEU");
        kiemTra("chat:tin-nhan-ngay:" + chuThe, properties.soTinNhanMoiNgay(), MOT_NGAY, "loi.CHAT_QUA_NHIEU.moiNgay");
    }

    @Override
    public void ghiNhanGoiMoHinh() {
        kiemTra("chat:toan-he-thong", properties.soTinNhanToanHeThongMoiNgay(), MOT_NGAY,
                "loi.CHAT_QUA_NHIEU.toanHeThong");
    }

    private void kiemTra(String khoa, int toiDa, Duration cuaSo, String khoaThongDiep) {
        long soLan;
        try {
            soLan = dem(khoa, cuaSo);
        } catch (RuntimeException ex) {
            log.warn("Không đếm được trên Redis, bỏ qua giới hạn chatbot: {}", ex.getMessage());
            return;
        }
        if (soLan > toiDa) {
            throw new LoiNghiepVu(MaLoi.CHAT_QUA_NHIEU, khoaThongDiep);
        }
    }

    /** Cửa sổ cố định: lần gọi đầu mở cửa sổ đếm, hết cửa sổ thì khoá tự xoá. */
    private long dem(String khoa, Duration cuaSo) {
        Long soLan = redisTemplate.opsForValue().increment(khoa);
        long ketQua = soLan == null ? 0 : soLan;
        // TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại, tránh khoá đếm mãi không hết hạn
        if (ketQua == 1 || conThieuTtl(khoa)) {
            redisTemplate.expire(khoa, cuaSo);
        }
        return ketQua;
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

}
