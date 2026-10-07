package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;

/**
 * Ghi thông báo trong ứng dụng cho các sự kiện của ca làm việc và yêu cầu đổi ca / xin nghỉ. Như
 * {@link ThongBaoLichHenService}: mọi hàm phải được gọi trong transaction đang sửa dữ liệu.
 */
public interface ThongBaoLichLamViecService {

    /** Bác sĩ vừa gửi yêu cầu: báo mọi quản trị viên đang hoạt động. */
    void yeuCauMoi(YeuCauDoiLich yeuCau);

    /** Quản trị viên vừa duyệt / từ chối: báo bác sĩ đã gửi. */
    void ketQuaYeuCau(YeuCauDoiLich yeuCau);

    /** Quản trị viên vừa xếp / sửa / hủy ca của bác sĩ (không qua yêu cầu của bác sĩ): báo bác sĩ đó. */
    void caThayDoi(BacSi bacSi, String noiDung);

    /** Ca khám của lịch hẹn bị hủy, bệnh nhân phải đổi lịch: báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenCanDoi(LichHen lichHen, String lyDo);

    /** Ca khám đổi phòng (lịch hẹn đã mang phòng mới): báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenDoiPhong(LichHen lichHen);

}
