package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.enums.SuKienLichHen;

/** Ghi nhật ký thao tác trên lịch hẹn (lich_su_thay_doi, loai = LICH_HEN). */
public interface LichSuLichHenService {

    void ghi(LichHen lichHen, SuKienLichHen suKien, NguoiThucHien nguoi, String lyDo, LichHen lienQuan);

    /** {@link #ghi} không có lý do, không có lịch liên quan. */
    default void ghi(LichHen lichHen, SuKienLichHen suKien, NguoiThucHien nguoi) {
        ghi(lichHen, suKien, nguoi, null, null);
    }

}
