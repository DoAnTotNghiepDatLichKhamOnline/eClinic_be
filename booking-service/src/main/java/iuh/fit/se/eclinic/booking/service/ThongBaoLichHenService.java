package iuh.fit.se.eclinic.booking.service;

import iuh.fit.se.eclinic.common.entity.booking.LichHen;
import iuh.fit.se.eclinic.common.entity.medical.HoSoBenhAn;

/**
 * Ghi thông báo trong ứng dụng và email cho các sự kiện của lịch hẹn. Mọi hàm phải được gọi TRONG transaction đang đổi trạng thái
 * lịch hẹn: thông báo được ghi vào bảng chờ gửi của booking-service cùng transaction đó, job gửi sang
 * notification-service sau (xem {@link GuiThongBaoService}).
 * <p>
 * Ai nhận: người làm thao tác không nhận thông báo về thao tác của chính mình. Bác sĩ nhận lịch mới, lịch bị bệnh nhân
 * hủy / đổi; phía bệnh nhân nhận tin bác sĩ hủy lịch, gửi cho tài khoản đã đặt và tài khoản của hồ sơ bệnh nhân (mỗi tài
 * khoản 1 thông báo). Khách đặt không đăng nhập và người giám hộ chỉ khớp theo CCCD không nhận (khi bác sĩ hủy,
 * người đặt vẫn nhận email theo địa chỉ liên hệ).
 * <p>
 * Email (các hàm {@code email...}) gửi tới 1 địa chỉ, không cần tài khoản: email liên hệ của lịch hẹn, để trống thì
 * email của tài khoản đã đặt; khách đặt mà không nhập email thì không ghi gì. Ngôn ngữ: của tài khoản đã đặt (đã lưu),
 * khách thì của request đã tạo lịch hẹn. Email chỉ chứa những gì phiếu khám đã hiện cho người có link: họ tên bệnh nhân,
 * giờ khám, bác sĩ, phòng, mã tra cứu, link phiếu khám; không có lý do khám, chuyên khoa, số điện thoại, CCCD.
 */
public interface ThongBaoLichHenService {

    /** Có lịch hẹn mới (đã xác nhận ngay khi đặt): báo bác sĩ. */
    void lichHenMoi(LichHen lichHen);

    /**
     * Bác sĩ đã hủy lịch hẹn (lý do đã nằm trong {@code lichHen.lyDoHuy}): báo trong ứng dụng cho phía bệnh nhân và
     * email cho người đặt (khách đặt không có tài khoản chỉ biết qua email). Email không chép lý do: lý do ở trên phiếu
     * khám.
     */
    void bacSiHuy(LichHen lichHen);

    /** Bệnh nhân đã hủy: báo bác sĩ. */
    void benhNhanDaHuy(LichHen lichHen);

    /**
     * Bệnh nhân đã đổi lịch. Cùng bác sĩ: 1 thông báo "đã đổi" gắn với lịch mới. Sang bác sĩ khác: bác sĩ cũ nhận "đã
     * hủy", bác sĩ mới nhận "lịch hẹn mới".
     */
    void benhNhanDaDoi(LichHen lichCu, LichHen lichMoi);

    /** Email "đặt lịch thành công" (lịch đã xác nhận) cho người đặt. */
    void emailDaNhanLich(LichHen lichHen);

    /** Email "đã đổi lịch" kèm mã tra cứu và link phiếu khám của lịch mới. */
    void emailDaDoiLich(LichHen lichMoi);

    /**
     * Nhắc lịch khám sắp tới: 1 thông báo cho mỗi tài khoản phía bệnh nhân và 1 email cho người đặt (cùng quy tắc người
     * nhận với email xác nhận). Câu chữ ghi giờ khám thật, không ghi "còn N ngày", nên nhắc trễ vẫn đúng.
     */
    void nhacLich(LichHen lichHen);

    /**
     * Bác sĩ đã hẹn ({@code doiNgay = false}) hoặc đổi ({@code true}) ngày tái khám của 1 lượt khám: 1 thông báo cho mỗi
     * tài khoản phía bệnh nhân và 1 email cho người đã đặt lịch hẹn của lượt khám đó (cùng quy tắc người nhận, ngôn ngữ
     * với email xác nhận). Nội dung chỉ có họ tên bệnh nhân, bác sĩ, ngày tái khám và link trang đặt lịch; không có chẩn
     * đoán, ghi chú, đơn thuốc. Chỉ là lời báo: không tạo lịch hẹn.
     */
    void taiKhamDaHen(HoSoBenhAn benhAn, boolean doiNgay);

    /** Nhắc trước ngày tái khám, cùng người nhận và nội dung với {@link #taiKhamDaHen}. */
    void nhacTaiKham(HoSoBenhAn benhAn);

}
