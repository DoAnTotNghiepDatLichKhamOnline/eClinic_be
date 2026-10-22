package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;

/**
 * Ghi nhật ký thao tác trên lịch hẹn (lich_su_lich_hen). Phải gọi TRONG transaction đang đổi lịch hẹn, như
 * {@link ThongBaoLichHenService}: thao tác bị hoàn tác (vd xem trước khai báo ngày nghỉ) thì dòng nhật ký cũng vậy.
 */
public interface LichSuLichHenService {

    /**
     * @param lyDo     câu người dùng gõ hoặc {@code {heThong.<khoa>}...} (VanBanHeThong); null = không có
     * @param lienQuan lịch hẹn mới của 1 lần đổi lịch; null ở các thao tác khác
     */
    void ghi(LichHen lichHen, SuKienLichHen suKien, NguoiThucHien nguoi, String lyDo, LichHen lienQuan);

    /** {@link #ghi} không có lý do, không có lịch liên quan. */
    default void ghi(LichHen lichHen, SuKienLichHen suKien, NguoiThucHien nguoi) {
        ghi(lichHen, suKien, nguoi, null, null);
    }

}
