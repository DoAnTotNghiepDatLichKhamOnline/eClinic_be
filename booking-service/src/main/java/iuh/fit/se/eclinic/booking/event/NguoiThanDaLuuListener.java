package iuh.fit.se.eclinic.booking.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import iuh.fit.se.eclinic.booking.service.NguoiThanDaLuuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** Lưu người thân sau khi lịch hẹn đã commit. */
@Slf4j
@Component
@RequiredArgsConstructor
public class NguoiThanDaLuuListener {

    private final NguoiThanDaLuuService nguoiThanDaLuuService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sauKhiDatLich(DaDatLichChoNguoiThanEvent event) {
        try {
            nguoiThanDaLuuService.ghiNho(event);
        } catch (RuntimeException ex) {
            // Không ghi dữ liệu cá nhân ra log
            log.warn("Không lưu được người thân (hồ sơ id={}) cho tài khoản id={}: {}", event.idHoSoBenhNhan(),
                    event.idTaiKhoan(), ex.getClass().getSimpleName());
        }
    }

}
