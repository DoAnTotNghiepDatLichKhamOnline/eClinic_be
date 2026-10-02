package iuh.fit.se.eclinic.identity.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import iuh.fit.se.eclinic.common.exception.LoiNghiepVu;
import iuh.fit.se.eclinic.common.exception.MaLoi;
import iuh.fit.se.eclinic.identity.client.LuuTruAnh;
import iuh.fit.se.eclinic.identity.config.AnhDaiDienProperties;
import iuh.fit.se.eclinic.identity.dto.response.HoSoCaNhanResponse;
import iuh.fit.se.eclinic.identity.event.TaiKhoanDaXoaEvent;
import iuh.fit.se.eclinic.identity.service.AnhDaiDienService;
import iuh.fit.se.eclinic.identity.service.HoSoCaNhanService;
import iuh.fit.se.eclinic.identity.service.TaiKhoanService;
import iuh.fit.se.eclinic.identity.util.DinhDangAnh;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * CỐ Ý không có @Transactional ở class (khác các service khác): gọi kho ảnh có thể mất tới 20 giây, không được giữ
 * transaction / kết nối DB trong lúc đó. Mỗi lời gọi TaiKhoanService, HoSoCaNhanService tự mở transaction ngắn của nó.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnhDaiDienServiceImpl implements AnhDaiDienService {

    private static final String KHOA_DEM_TAI_LEN = "anh-dai-dien:tai-len:";

    private final TaiKhoanService taiKhoanService;
    private final HoSoCaNhanService hoSoCaNhanService;
    private final LuuTruAnh luuTruAnh;
    private final StringRedisTemplate redisTemplate;
    private final AnhDaiDienProperties anhDaiDienProperties;

    @Override
    public HoSoCaNhanResponse taiLen(Long idTaiKhoan, byte[] noiDung) {
        taiKhoanService.layDangHoatDong(idTaiKhoan);
        if (DinhDangAnh.nhanDien(noiDung).isEmpty()) {
            throw new LoiNghiepVu(MaLoi.ANH_KHONG_HOP_LE);
        }
        if (!luuTruAnh.daCauHinh()) {
            throw new LoiNghiepVu(MaLoi.LUU_TRU_ANH_KHONG_KHA_DUNG);
        }
        kiemTraSoLanTaiLen(idTaiKhoan);

        // Cùng mã với ảnh cũ: kho ghi đè nên không phải xoá ảnh cũ. Ảnh Google (nếu đang dùng) không nằm trong kho.
        String url = luuTruAnh.taiLen(maAnh(idTaiKhoan), noiDung);
        taiKhoanService.capNhatAnhDaiDien(idTaiKhoan, url);
        log.info("Đổi ảnh đại diện tài khoản id={} ({} byte)", idTaiKhoan, noiDung.length);
        return hoSoCaNhanService.layHoSo(idTaiKhoan);
    }

    @Override
    public HoSoCaNhanResponse xoa(Long idTaiKhoan) {
        String anhCu = taiKhoanService.capNhatAnhDaiDien(idTaiKhoan, null);
        if (anhCu != null) {
            if (!xoaTrongKho(idTaiKhoan, anhCu)) {
                log.warn("Không xoá được ảnh đại diện cũ của tài khoản id={} trong kho ảnh (sẽ bị ghi đè ở lần tải lên sau)",
                        idTaiKhoan);
            }
            log.info("Bỏ ảnh đại diện tài khoản id={}", idTaiKhoan);
        }
        return hoSoCaNhanService.layHoSo(idTaiKhoan);
    }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void donAnhCuaTaiKhoanDaXoa(TaiKhoanDaXoaEvent event) {
        if (event.anhDaiDien() != null && !xoaTrongKho(event.idTaiKhoan(), event.anhDaiDien())) {
            // id không bao giờ được dùng lại nên ảnh này không còn bị ghi đè: phải xoá tay trong kho
            log.warn("Không xoá được ảnh đại diện của tài khoản đã xoá id={} trong kho ảnh (ảnh còn sót lại: {})",
                    event.idTaiKhoan(), maAnh(event.idTaiKhoan()));
        }
    }

    /**
     * Chỉ xoá ảnh của kho mình (không phải URL Google). Lỗi kho không ném ra, để không làm hỏng việc của bên gọi.
     *
     * @return false nếu kho báo lỗi (ảnh còn trong kho)
     */
    private boolean xoaTrongKho(Long idTaiKhoan, String anhCu) {
        if (!luuTruAnh.laAnhCuaKho(anhCu)) {
            return true;
        }
        try {
            luuTruAnh.xoa(maAnh(idTaiKhoan));
            return true;
        } catch (LoiNghiepVu ex) {
            return false;
        }
    }

    /** Chỉ đếm các lần thật sự gọi kho ảnh (ảnh sai định dạng không tốn hạn mức nên không đếm). */
    private void kiemTraSoLanTaiLen(Long idTaiKhoan) {
        String khoa = KHOA_DEM_TAI_LEN + idTaiKhoan;
        Long soLan = redisTemplate.opsForValue().increment(khoa);
        long ketQua = soLan == null ? 0 : soLan;
        // TTL < 0: INCR xong mà chưa kịp EXPIRE (service chết giữa chừng) thì đặt lại
        if (ketQua == 1 || conThieuTtl(khoa)) {
            redisTemplate.expire(khoa, anhDaiDienProperties.khoangDem());
        }
        if (ketQua > anhDaiDienProperties.soLanTaiLenToiDa()) {
            log.info("Tài khoản id={} tải ảnh đại diện quá {} lần", idTaiKhoan, anhDaiDienProperties.soLanTaiLenToiDa());
            throw new LoiNghiepVu(MaLoi.GUI_LAI_QUA_NHANH, "Bạn đổi ảnh đại diện quá nhiều lần, vui lòng thử lại sau");
        }
    }

    private boolean conThieuTtl(String khoa) {
        Long ttl = redisTemplate.getExpire(khoa);
        return ttl != null && ttl < 0;
    }

    private static String maAnh(Long idTaiKhoan) {
        return "avatar/" + idTaiKhoan;
    }

}
