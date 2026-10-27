package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.NgonNgu;

/** Tệp lịch .ics của 1 lịch hẹn (tải từ phiếu khám, đính kèm email lịch hẹn). */
public interface LichIcsService {

    /** Nội dung tệp theo trạng thái hiện tại của lịch hẹn; gọi trong transaction (đọc lịch cũ, bác sĩ, phòng). */
    String tao(LichHen lichHen, NgonNgu ngonNgu);

}
