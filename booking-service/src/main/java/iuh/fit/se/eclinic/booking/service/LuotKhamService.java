package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/** Việc dùng chung trên lượt khám (dòng khung_gio_kham) khi lịch hẹn hoặc ca làm việc thay đổi. */
public interface LuotKhamService {

    /** Mở lại lượt khám của lịch hẹn vừa hết hiệu lực; khoá lịch hẹn trước rồi lượt khám để tránh deadlock. */
    void traLuot(LichHen lichHen);

    /** Đánh lại số thứ tự khám của các lịch hẹn còn hiệu lực của 1 phòng khám trong 1 ngày. */
    void danhLaiSoThuTu(Long idPhongKham, LocalDate ngay);

}
