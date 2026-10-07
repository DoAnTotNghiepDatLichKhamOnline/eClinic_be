package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/**
 * Việc dùng chung trên lượt khám (dòng khung_gio_kham) khi lịch hẹn hoặc ca làm việc thay đổi. Mọi hàm phải được gọi
 * trong transaction đang sửa lịch hẹn / ca.
 */
public interface LuotKhamService {

    /**
     * Mở lại lượt khám của 1 lịch hẹn vừa hết hiệu lực (bị từ chối, bị hủy, bị đổi): khoá lượt SAU khi đã khoá lịch hẹn
     * (đặt lịch chỉ khoá lượt khám, nên thứ tự "lịch hẹn trước, lượt sau" không deadlock), DA_DAT thì về CON_TRONG.
     * Lượt đã DA_HUY theo ca thì giữ nguyên.
     */
    void traLuot(LichHen lichHen);

    /**
     * Đánh lại số thứ tự khám của các lịch hẹn còn hiệu lực của 1 phòng khám trong 1 ngày. Số thứ tự là thứ hạng của
     * lượt khám trong các lượt của phòng trong ngày (quy tắc #4), nên phải tính lại khi phòng có thêm lượt đứng trước
     * (xếp ca mới, kéo dài ca) hoặc ca chuyển sang phòng khác.
     */
    void danhLaiSoThuTu(Long idPhongKham, LocalDate ngay);

}
