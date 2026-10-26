package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;

/** Ghi thông báo trong ứng dụng cho các sự kiện của ca làm việc và yêu cầu đổi ca / xin nghỉ. */
public interface ThongBaoLichLamViecService {

    /** Yêu cầu vừa tới bước quản trị viên duyệt: báo mọi quản trị viên đang hoạt động. */
    void yeuCauMoi(YeuCauDoiLich yeuCau);

    /** Bác sĩ vừa gửi 1 nhóm yêu cầu xin nghỉ nhiều ca: 1 thông báo cho mỗi quản trị viên đang hoạt động. */
    void yeuCauMoiNhieuCa(List<YeuCauDoiLich> cacYeuCau);

    /** Quản trị viên vừa duyệt hoặc từ chối: báo bác sĩ gửi, nhường ca thì báo cả bác sĩ nhận. */
    void ketQuaYeuCau(YeuCauDoiLich yeuCau);

    /** Quản trị viên vừa duyệt / từ chối cả nhóm xin nghỉ nhiều ca: 1 thông báo cho bác sĩ đã gửi. */
    void ketQuaNhom(List<YeuCauDoiLich> daXuLy);

    /** Ca của yêu cầu đang chờ vừa bị hủy nên yêu cầu bị đóng: báo bác sĩ gửi và bác sĩ nhận. */
    void yeuCauDongDoHuyCa(YeuCauDoiLich yeuCau, LyDoHuyCa lyDo);

    // Nhường ca: phía bác sĩ nhận và phản hồi của bác sĩ nhận

    /** Bác sĩ vừa gửi yêu cầu nhường ca: báo bác sĩ nhận để đồng ý / từ chối. */
    void nhuongCaMoi(YeuCauDoiLich yeuCau);

    /** Bác sĩ đã gửi vừa rút yêu cầu nhường ca: báo bác sĩ nhận. */
    void nhuongCaDaRut(YeuCauDoiLich yeuCau);

    /** Bác sĩ nhận vừa đồng ý / từ chối (ghi chú ở {@code yeuCau.ghiChuBacSiNhan}): báo bác sĩ đã gửi. */
    void dongNghiepPhanHoi(YeuCauDoiLich yeuCau);

    /** Ca của lịch hẹn vừa được nhường: báo phía bệnh nhân và email cho người đặt. */
    void lichHenDoiBacSi(LichHen lichHen, BacSi bacSiCu);

    // Quản trị viên vừa xếp / sửa / hủy ca của bác sĩ (không qua yêu cầu của bác sĩ): báo bác sĩ đó

    /** @param daTao các ca vừa xếp trong cùng 1 phòng, theo thứ tự ngày; ít nhất 1 ca */
    void caMoi(BacSi bacSi, List<LichLamViec> daTao, PhongKham phongKham);

    /** Các ca vừa tạo hàng loạt cho bác sĩ (sao chép ca, nhập từ Excel; có thể ở nhiều phòng): 1 thông báo. */
    void caMoiHangLoat(BacSi bacSi, List<LichLamViec> daTao);

    void caDaSua(LichLamViec ca);

    void caBiHuy(LichLamViec ca, LyDoHuyCa lyDo);

    /** Nhiều ca của bác sĩ bị hủy cùng lúc (ngày bệnh viện nghỉ): gộp thành 1 thông báo. */
    void caBiHuyHangLoat(BacSi bacSi, List<LichLamViec> cacCa, LyDoHuyCa lyDo);

    /** Gửi lịch làm việc: 1 email bảng các ca kèm link và 1 thông báo trong ứng dụng cho bác sĩ. */
    void guiLich(BacSi bacSi, LocalDate tuNgay, LocalDate denNgay, List<LichLamViec> cacCa);

    /** Số dòng ca tối đa trong 1 email lịch làm việc; các ca sau chỉ được đếm ("còn N ca khác"). */
    int SO_CA_TOI_DA_TRONG_EMAIL = 100;

    /** Ca của lịch hẹn bị hủy, bệnh nhân phải đổi lịch: báo phía bệnh nhân và email kèm link phiếu khám. */
    void lichHenCanDoi(LichHen lichHen, LyDoHuyCa lyDo);

    /** Ca khám đổi phòng (lịch hẹn đã mang phòng mới): báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenDoiPhong(LichHen lichHen);

}
