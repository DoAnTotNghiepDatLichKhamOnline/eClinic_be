package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.NgonNgu;

/**
 * Tệp lịch .ics của 1 lịch hẹn (tải từ phiếu khám, đính kèm email lịch hẹn). Mọi lịch hẹn của 1 chuỗi đổi lịch là 1 sự
 * kiện: UID = mã tra cứu của lịch hẹn đầu chuỗi, SEQUENCE = 4 x số lần đã đổi + giai đoạn (0 bình thường, 1 đổi bác sĩ,
 * 2 chờ đổi lịch, 3 đã hủy / đã đổi đi), nên lịch hẹn mới luôn đè lên bản "đã hủy" của lịch cũ. Không có họ tên, SĐT,
 * CCCD, lý do khám. Không phụ thuộc ThongBaoLichHenService (service đó gọi service này).
 */
public interface LichIcsService {

    /** Nội dung tệp theo trạng thái hiện tại của lịch hẹn; gọi trong transaction (đọc lịch cũ, bác sĩ, phòng). */
    String tao(LichHen lichHen, NgonNgu ngonNgu);

}
