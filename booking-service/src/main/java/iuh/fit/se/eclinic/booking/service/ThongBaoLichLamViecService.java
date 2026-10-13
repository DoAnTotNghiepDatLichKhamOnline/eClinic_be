package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;

/**
 * Ghi thông báo trong ứng dụng cho các sự kiện của ca làm việc và yêu cầu đổi ca / xin nghỉ. Như
 * {@link ThongBaoLichHenService}: mọi hàm phải được gọi trong transaction đang sửa dữ liệu.
 */
public interface ThongBaoLichLamViecService {

    /** Bác sĩ vừa gửi yêu cầu: báo mọi quản trị viên đang hoạt động. */
    void yeuCauMoi(YeuCauDoiLich yeuCau);

    /** Quản trị viên vừa duyệt / từ chối (ghi chú đã nằm trong {@code yeuCau.ghiChuXuLy}): báo bác sĩ đã gửi. */
    void ketQuaYeuCau(YeuCauDoiLich yeuCau);

    /** Ca của yêu cầu đang chờ duyệt vừa bị quản trị viên hủy nên yêu cầu bị đóng (từ chối): báo bác sĩ đã gửi. */
    void yeuCauDongDoHuyCa(YeuCauDoiLich yeuCau, LyDoHuyCa lyDo);

    // Quản trị viên vừa xếp / sửa / hủy ca của bác sĩ (không qua yêu cầu của bác sĩ): báo bác sĩ đó

    /** @param daTao các ca vừa xếp trong cùng 1 phòng, theo thứ tự ngày; ít nhất 1 ca */
    void caMoi(BacSi bacSi, List<LichLamViec> daTao, PhongKham phongKham);

    void caDaSua(LichLamViec ca);

    void caBiHuy(LichLamViec ca, LyDoHuyCa lyDo);

    /** Ca khám của lịch hẹn bị hủy, bệnh nhân phải đổi lịch: báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenCanDoi(LichHen lichHen, LyDoHuyCa lyDo);

    /** Ca khám đổi phòng (lịch hẹn đã mang phòng mới): báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenDoiPhong(LichHen lichHen);

}
