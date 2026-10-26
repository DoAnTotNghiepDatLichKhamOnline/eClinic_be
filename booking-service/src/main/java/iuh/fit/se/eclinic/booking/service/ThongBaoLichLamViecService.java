package iuh.fit.se.eclinic.booking.service;

import java.time.LocalDate;
import java.util.List;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

import iuh.fit.se.eclinic.common.entity.catalog.BacSi;
import iuh.fit.se.eclinic.common.entity.catalog.PhongKham;
import iuh.fit.se.eclinic.common.entity.scheduling.LichLamViec;
import iuh.fit.se.eclinic.common.entity.scheduling.YeuCauDoiLich;

/**
 * Ghi thông báo trong ứng dụng cho các sự kiện của ca làm việc và yêu cầu đổi ca / xin nghỉ. Như
 * {@link ThongBaoLichHenService}: mọi hàm phải được gọi trong transaction đang sửa dữ liệu.
 */
public interface ThongBaoLichLamViecService {

    /**
     * Yêu cầu vừa tới bước quản trị viên duyệt (bác sĩ gửi xin nghỉ / đổi ca, hoặc bác sĩ nhận đồng ý nhận ca được
     * nhường): báo mọi quản trị viên đang hoạt động.
     */
    void yeuCauMoi(YeuCauDoiLich yeuCau);

    /**
     * Bác sĩ vừa gửi 1 nhóm yêu cầu xin nghỉ nhiều ca: 1 thông báo cho mỗi quản trị viên đang hoạt động.
     *
     * @param cacYeuCau theo ngày của ca; ít nhất 1 yêu cầu
     */
    void yeuCauMoiNhieuCa(List<YeuCauDoiLich> cacYeuCau);

    /**
     * Quản trị viên vừa duyệt / từ chối (ghi chú đã nằm trong {@code yeuCau.ghiChuXuLy}): báo bác sĩ đã gửi; với nhường
     * ca thì báo cả bác sĩ nhận.
     */
    void ketQuaYeuCau(YeuCauDoiLich yeuCau);

    /**
     * Quản trị viên vừa duyệt / từ chối cả nhóm xin nghỉ nhiều ca: 1 thông báo cho bác sĩ đã gửi.
     *
     * @param daXuLy các yêu cầu vừa xử lý (cùng trạng thái, cùng ghi chú), theo ngày của ca; ít nhất 1 yêu cầu
     */
    void ketQuaNhom(List<YeuCauDoiLich> daXuLy);

    /**
     * Ca của yêu cầu đang chờ (đồng nghiệp hoặc quản trị viên) vừa bị quản trị viên hủy nên yêu cầu bị đóng (từ chối):
     * báo bác sĩ đã gửi; với nhường ca thì báo cả bác sĩ nhận.
     */
    void yeuCauDongDoHuyCa(YeuCauDoiLich yeuCau, LyDoHuyCa lyDo);

    // Nhường ca: phía bác sĩ nhận và phản hồi của bác sĩ nhận

    /** Bác sĩ vừa gửi yêu cầu nhường ca: báo bác sĩ nhận để đồng ý / từ chối. */
    void nhuongCaMoi(YeuCauDoiLich yeuCau);

    /** Bác sĩ đã gửi vừa rút yêu cầu nhường ca: báo bác sĩ nhận. */
    void nhuongCaDaRut(YeuCauDoiLich yeuCau);

    /** Bác sĩ nhận vừa đồng ý / từ chối (ghi chú ở {@code yeuCau.ghiChuBacSiNhan}): báo bác sĩ đã gửi. */
    void dongNghiepPhanHoi(YeuCauDoiLich yeuCau);

    /**
     * Ca khám của lịch hẹn vừa được nhường (lịch hẹn đã mang bác sĩ mới): báo tài khoản đã đặt, tài khoản của hồ sơ bệnh
     * nhân, và gửi email cho người đã đặt.
     */
    void lichHenDoiBacSi(LichHen lichHen, BacSi bacSiCu);

    // Quản trị viên vừa xếp / sửa / hủy ca của bác sĩ (không qua yêu cầu của bác sĩ): báo bác sĩ đó

    /** @param daTao các ca vừa xếp trong cùng 1 phòng, theo thứ tự ngày; ít nhất 1 ca */
    void caMoi(BacSi bacSi, List<LichLamViec> daTao, PhongKham phongKham);

    /**
     * Các ca vừa tạo hàng loạt cho bác sĩ (sao chép ca, nhập từ Excel; có thể ở nhiều phòng): 1 thông báo.
     *
     * @param daTao theo thứ tự ngày, giờ; ít nhất 1 ca
     */
    void caMoiHangLoat(BacSi bacSi, List<LichLamViec> daTao);

    void caDaSua(LichLamViec ca);

    void caBiHuy(LichLamViec ca, LyDoHuyCa lyDo);

    /**
     * Các ca của bác sĩ vừa bị hủy cùng lúc (khai báo ngày bệnh viện nghỉ): 1 thông báo; chỉ 1 ca thì như {@link #caBiHuy}.
     *
     * @param cacCa theo thứ tự ngày, giờ; ít nhất 1 ca
     */
    void caBiHuyHangLoat(BacSi bacSi, List<LichLamViec> cacCa, LyDoHuyCa lyDo);

    /**
     * Quản trị viên gửi lịch làm việc: 1 email cho bác sĩ (bảng các ca, tối đa {@code SO_CA_TOI_DA_TRONG_EMAIL} dòng, kèm
     * link trang Lịch làm việc) và 1 thông báo trong ứng dụng.
     *
     * @param cacCa các ca còn hoạt động của bác sĩ trong [tuNgay, denNgay], theo ngày, giờ; ít nhất 1 ca
     */
    void guiLich(BacSi bacSi, LocalDate tuNgay, LocalDate denNgay, List<LichLamViec> cacCa);

    /** Số dòng ca tối đa trong 1 email lịch làm việc; các ca sau chỉ được đếm ("còn N ca khác"). */
    int SO_CA_TOI_DA_TRONG_EMAIL = 100;

    /**
     * Ca khám của lịch hẹn bị hủy, bệnh nhân phải đổi lịch: báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân, và
     * gửi email kèm link phiếu khám cho người đã đặt (khách không có tài khoản cũng nhận được).
     */
    void lichHenCanDoi(LichHen lichHen, LyDoHuyCa lyDo);

    /** Ca khám đổi phòng (lịch hẹn đã mang phòng mới): báo tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân. */
    void lichHenDoiPhong(LichHen lichHen);

}
