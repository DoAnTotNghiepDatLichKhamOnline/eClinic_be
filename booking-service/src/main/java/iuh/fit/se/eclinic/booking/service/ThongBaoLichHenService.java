package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;

/**
 * Ghi thông báo trong ứng dụng cho các sự kiện của lịch hẹn. Mọi hàm phải được gọi TRONG transaction đang đổi trạng thái
 * lịch hẹn: thông báo được ghi vào bảng chờ gửi của booking-service cùng transaction đó, job gửi sang
 * notification-service sau (xem {@link GuiThongBaoService}).
 * <p>
 * Ai nhận: người làm thao tác không nhận thông báo về thao tác của chính mình. Bác sĩ nhận lịch mới, lịch bị bệnh nhân
 * hủy / đổi; phía bệnh nhân nhận xác nhận / từ chối, gửi cho tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài
 * khoản 1 thông báo). Khách đặt không đăng nhập và người giám hộ chỉ khớp theo CCCD không nhận.
 */
public interface ThongBaoLichHenService {

    /** Có lịch hẹn mới chờ xác nhận: báo bác sĩ. */
    void lichHenMoi(LichHen lichHen);

    /** Bác sĩ đã xác nhận: báo phía bệnh nhân. */
    void daXacNhan(LichHen lichHen);

    /** Bác sĩ đã từ chối (lý do đã nằm trong {@code lichHen.lyDoHuy}): báo phía bệnh nhân. */
    void biTuChoi(LichHen lichHen);

    /** Bệnh nhân đã hủy: báo bác sĩ. */
    void benhNhanDaHuy(LichHen lichHen);

    /**
     * Bệnh nhân đã đổi lịch. Cùng bác sĩ: 1 thông báo "đã đổi" gắn với lịch mới. Sang bác sĩ khác: bác sĩ cũ nhận "đã
     * hủy", bác sĩ mới nhận "lịch hẹn mới".
     */
    void benhNhanDaDoi(LichHen lichCu, LichHen lichMoi);

}
