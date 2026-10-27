package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/** Ghi thông báo trong ứng dụng và email cho các sự kiện của lịch hẹn. */
public interface ThongBaoLichHenService {

    /** Có lịch hẹn mới (đã xác nhận ngay khi đặt): báo bác sĩ. */
    void lichHenMoi(LichHen lichHen);

    /** Bác sĩ đã hủy lịch hẹn: báo trong ứng dụng cho phía bệnh nhân và email cho người đặt. */
    void bacSiHuy(LichHen lichHen);

    /** Quản trị viên đã hủy lịch hẹn: báo phía bệnh nhân và bác sĩ, email cho người đặt. */
    void quanTriHuy(LichHen lichHen);

    /** Quản trị viên đã đổi lịch thay bệnh nhân: báo phía bệnh nhân và bác sĩ. */
    void quanTriDoi(LichHen lichCu, LichHen lichMoi);

    /** Bệnh nhân đã hủy: báo bác sĩ. */
    void benhNhanDaHuy(LichHen lichHen);

    /** Bệnh nhân đã đổi lịch. */
    void benhNhanDaDoi(LichHen lichCu, LichHen lichMoi);

    /** Email "đặt lịch thành công" (lịch đã xác nhận) cho người đặt. */
    void emailDaNhanLich(LichHen lichHen);

    /** Email "đã đổi lịch" kèm mã tra cứu và link phiếu khám của lịch mới. */
    void emailDaDoiLich(LichHen lichMoi);

    /** Email "lịch khám đổi bác sĩ" (ca khám được nhường; lịch hẹn đã mang bác sĩ mới) kèm link phiếu khám. */
    void emailDoiBacSi(LichHen lichHen);

    /** Email ca khám bị hủy, cần đổi lịch, kèm link phiếu khám. */
    void emailCanDoiLich(LichHen lichHen);

    /** Email cấp lại liên kết: link phiếu khám cũ, thêm link sổ khám bệnh khi đã khám. */
    String emailCapLaiLienKet(LichHen lichHen, String lienKetSoKham);

    /** Nhắc lịch khám sắp tới: thông báo cho phía bệnh nhân và email cho người đặt. */
    void nhacLich(LichHen lichHen);

    /** Bác sĩ hẹn hoặc đổi ngày tái khám: thông báo cho phía bệnh nhân và email cho người đặt. */
    void taiKhamDaHen(HoSoBenhAn benhAn, boolean doiNgay);

    /** Nhắc trước ngày tái khám, cùng người nhận và nội dung với {@link #taiKhamDaHen}. */
    void nhacTaiKham(HoSoBenhAn benhAn);

}
